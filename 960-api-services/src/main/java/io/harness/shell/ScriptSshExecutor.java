/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.INVALID_EXECUTION_ID;
import static io.harness.eraro.ErrorCode.UNKNOWN_ERROR;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.shell.SshHelperUtils.normalizeError;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.SshRetryableException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.Misc;
import io.harness.shell.ExecuteCommandResponse.ExecuteCommandResponseBuilder;
import io.harness.shell.ShellExecutionData.ShellExecutionDataBuilder;
import io.harness.stream.BoundedInputStream;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Created by anubhaw on 2/10/16.
 */
@ValidateOnExecution
@Slf4j
public class ScriptSshExecutor extends AbstractScriptExecutor {
  public static final int CHUNK_SIZE = 10 * 1024; // 10KB
  /**
   * The constant DEFAULT_SUDO_PROMPT_PATTERN.
   */
  public static final String DEFAULT_SUDO_PROMPT_PATTERN = "^\\[sudo\\] password for .+: .*";
  /**
   * The constant LINE_BREAK_PATTERN.
   */
  public static final String LINE_BREAK_PATTERN = "\\R+";
  /**
   * The constant log.
   */
  private static final int MAX_BYTES_READ_PER_CHANNEL =
      1024 * 1024 * 1024; // TODO: Read from config. 1 GB per channel for now.

  public static final String CHANNEL_IS_NOT_OPENED = "channel is not opened.";

  private Pattern sudoPasswordPromptPattern = Pattern.compile(DEFAULT_SUDO_PROMPT_PATTERN);
  private Pattern lineBreakPattern = Pattern.compile(LINE_BREAK_PATTERN);

  protected SshSessionConfig config;

  /**
   * Instantiates a new abstract ssh executor.
   * @param logCallback          the log service
   */
  @Inject
  public ScriptSshExecutor(LogCallback logCallback, boolean shouldSaveExecutionLogs, ScriptExecutionContext config) {
    super(logCallback, shouldSaveExecutionLogs);
    if (isEmpty(((SshSessionConfig) config).getExecutionId())) {
      throw new WingsException(INVALID_EXECUTION_ID);
    }
    this.config = (SshSessionConfig) config;
  }

  @Override
  public CommandExecutionStatus executeCommandString(String command, StringBuffer output, boolean displayCommand) {
    try {
      return executeCommandString(command, output, displayCommand, false);
    } catch (SshRetryableException ex) {
      log.info("As MaxSessions limit reached, fetching new session for executionId: {}, hostName: {}",
          config.getExecutionId(), config.getHost());
      saveExecutionLog(format("Retry connecting to %s ....", config.getHost()));
      return executeCommandString(command, output, displayCommand, true);
    }
  }

  @NotNull
  private CommandExecutionStatus executeCommandString(
      String command, StringBuffer output, boolean displayCommand, boolean isRetry) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    Channel channel = null;
    long start = System.currentTimeMillis();
    try {
      saveExecutionLog(format("Initializing SSH connection to %s ....", config.getHost()));

      Session session;
      if (isRetry) {
        session = SshSessionManager.getSimplexSession(config, logCallback);
      } else {
        session = SshSessionManager.getCachedSession(config, logCallback);
      }

      channel = session.openChannel("exec");
      log.info("Session fetched in " + (System.currentTimeMillis() - start) + " ms");

      ((ChannelExec) channel).setPty(true);
      try (OutputStream outputStream = channel.getOutputStream(); InputStream inputStream = channel.getInputStream()) {
        ((ChannelExec) channel).setCommand(command);
        saveExecutionLog(format("Connecting to %s ....", config.getHost()));
        channel.connect(config.getSocketConnectTimeout());
        saveExecutionLog(format("Connection to %s established", config.getHost()));
        if (displayCommand) {
          saveExecutionLog(format("Executing command %s ...", command));
        } else {
          saveExecutionLog("Executing command ...");
        }

        int totalBytesRead = 0;
        byte[] byteBuffer = new byte[1024];
        String text = "";

        while (true) {
          while (inputStream.available() > 0) {
            int numOfBytesRead = inputStream.read(byteBuffer, 0, 1024);
            if (numOfBytesRead < 0) {
              break;
            }
            totalBytesRead += numOfBytesRead;
            if (totalBytesRead >= MAX_BYTES_READ_PER_CHANNEL) {
              // TODO: better error reporting
              throw new WingsException(UNKNOWN_ERROR);
            }
            String dataReadFromTheStream = new String(byteBuffer, 0, numOfBytesRead, UTF_8);
            if (output != null) {
              output.append(dataReadFromTheStream);
            }

            text += dataReadFromTheStream;
            text = processStreamData(text, false, outputStream);
          }

          if (text.length() > 0) {
            text = processStreamData(text, true, outputStream); // finished reading. update logs
          }

          if (channel.isClosed()) {
            commandExecutionStatus = channel.getExitStatus() == 0 ? SUCCESS : FAILURE;
            saveExecutionLog("Command finished with status " + commandExecutionStatus, commandExecutionStatus);
            return commandExecutionStatus;
          }
          sleep(Duration.ofSeconds(1));
        }
      }
    } catch (JSchException jsch) {
      if (!isRetry && CHANNEL_IS_NOT_OPENED.equals(jsch.getMessage())) {
        throw new SshRetryableException(jsch);
      } else {
        handleException(jsch);
        log.error("ex-Session fetched in " + (System.currentTimeMillis() - start) / 1000);
        log.error("Command execution failed with error", jsch);
        return commandExecutionStatus;
      }
    } catch (RuntimeException | IOException ex) {
      handleException(ex);
      log.error("ex-Session fetched in " + (System.currentTimeMillis() - start) / 1000);
      log.error("Command execution failed with error", ex);
      return commandExecutionStatus;
    } finally {
      if (channel != null && !channel.isClosed()) {
        log.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
  }

  @Override
  public ExecuteCommandResponse executeCommandString(String command, List<String> envVariablesToCollect) {
    return executeCommandString(command, envVariablesToCollect, Collections.emptyList(), null);
  }

  @Override
  public ExecuteCommandResponse executeCommandString(String command, List<String> envVariablesToCollect,
      List<String> secretEnvVariablesToCollect, Long timeoutInMillis) {
    try {
      return getExecuteCommandResponse(command, envVariablesToCollect, secretEnvVariablesToCollect, false);
    } catch (SshRetryableException ex) {
      log.info("As MaxSessions limit reached, fetching new session for executionId: {}, hostName: {}",
          config.getExecutionId(), config.getHost());
      saveExecutionLog(format("Retry connecting to %s ....", config.getHost()));
      return getExecuteCommandResponse(command, envVariablesToCollect, secretEnvVariablesToCollect, true);
    }
  }

  private ExecuteCommandResponse getExecuteCommandResponse(
      String command, List<String> envVariablesToCollect, List<String> secretEnvVariablesToCollect, boolean isRetry) {
    ShellExecutionDataBuilder executionDataBuilder = ShellExecutionData.builder();
    ExecuteCommandResponseBuilder executeCommandResponseBuilder = ExecuteCommandResponse.builder();
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    Channel channel = null;
    long start = System.currentTimeMillis();
    Map<String, String> envVariablesMap = new HashMap<>();
    try {
      saveExecutionLog(format("Initializing SSH connection to %s ....", config.getHost()));
      Session session;
      if (isRetry) {
        session = SshSessionManager.getSimplexSession(this.config, this.logCallback);
      } else {
        session = SshSessionManager.getCachedSession(this.config, this.logCallback);
      }

      channel = session.openChannel("exec");
      log.info("Session fetched in " + (System.currentTimeMillis() - start) + " ms");

      ((ChannelExec) channel).setPty(true);

      String directoryPath = resolveEnvVarsInPath(this.config.getWorkingDirectory() + "/");
      String envVariablesFilename = null;
      command = "cd \"" + directoryPath + "\"\n" + command;

      // combine both variable types
      List<String> allVariablesToCollect =
          Stream.concat(envVariablesToCollect.stream(), secretEnvVariablesToCollect.stream())
              .collect(Collectors.toList());

      if (!allVariablesToCollect.isEmpty()) {
        envVariablesFilename = "harness-" + this.config.getExecutionId() + ".out";
        command = addEnvVariablesCollector(
            command, allVariablesToCollect, "\"" + directoryPath + envVariablesFilename + "\"", ScriptType.BASH);
      }

      try (OutputStream outputStream = channel.getOutputStream(); InputStream inputStream = channel.getInputStream()) {
        ((ChannelExec) channel).setCommand(command);
        saveExecutionLog(format("Connecting to %s ....", config.getHost()));
        channel.connect(config.getSocketConnectTimeout());
        saveExecutionLog(format("Connection to %s established", config.getHost()));
        saveExecutionLog("Executing command...");

        int totalBytesRead = 0;
        byte[] byteBuffer = new byte[1024];
        String text = "";

        while (true) {
          while (inputStream.available() > 0) {
            int numOfBytesRead = inputStream.read(byteBuffer, 0, 1024);
            if (numOfBytesRead < 0) {
              break;
            }
            totalBytesRead += numOfBytesRead;
            if (totalBytesRead >= MAX_BYTES_READ_PER_CHANNEL) {
              // TODO: better error reporting
              throw new WingsException(UNKNOWN_ERROR);
            }
            String dataReadFromTheStream = new String(byteBuffer, 0, numOfBytesRead, UTF_8);
            text += dataReadFromTheStream;
            text = processStreamData(text, false, outputStream);
          }

          if (text.length() > 0) {
            text = processStreamData(text, true, outputStream); // finished reading. update logs
          }

          if (channel.isClosed()) {
            commandExecutionStatus = channel.getExitStatus() == 0 ? SUCCESS : FAILURE;
            saveExecutionLog("Command finished with status " + commandExecutionStatus, commandExecutionStatus);

            if (commandExecutionStatus == SUCCESS && envVariablesFilename != null) {
              BufferedReader br = null;
              try {
                channel = getSftpConnectedChannel();
                ((ChannelSftp) channel).cd(directoryPath);
                BoundedInputStream stream =
                    new BoundedInputStream(((ChannelSftp) channel).get(envVariablesFilename), CHUNK_SIZE);
                br = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8));
                processScriptOutputFile(envVariablesMap, br, secretEnvVariablesToCollect);
              } catch (JSchException | SftpException | IOException e) {
                log.error("Exception occurred during reading file from SFTP server due to " + e.getMessage(), e);
              } finally {
                if (br != null) {
                  br.close();
                }
                try {
                  ((ChannelSftp) channel).rm(directoryPath + envVariablesFilename);
                } catch (SftpException e) {
                  log.error("Failed to delete file " + envVariablesFilename, e);
                }
              }
            }
            executionDataBuilder.sweepingOutputEnvVariables(envVariablesMap);
            executeCommandResponseBuilder.status(commandExecutionStatus);
            executeCommandResponseBuilder.commandExecutionData(executionDataBuilder.build());
            return executeCommandResponseBuilder.build();
          }
          sleep(Duration.ofSeconds(1));
        }
      }
    } catch (RuntimeException | JSchException | IOException ex) {
      if (ex instanceof JSchException && !isRetry && CHANNEL_IS_NOT_OPENED.equals(ex.getMessage())) {
        log.error("Command execution failed with error", ex);
        throw new SshRetryableException(ex);
      }

      handleException(ex);
      log.error("ex-Session fetched in " + (System.currentTimeMillis() - start) / 1000);
      log.error("Command execution failed with error", ex);
      executionDataBuilder.sweepingOutputEnvVariables(envVariablesMap);
      executeCommandResponseBuilder.status(commandExecutionStatus);
      executeCommandResponseBuilder.commandExecutionData(executionDataBuilder.build());
      return executeCommandResponseBuilder.build();
    } finally {
      if (channel != null && !channel.isClosed()) {
        log.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
  }

  private Channel getSftpConnectedChannel() throws JSchException {
    Channel channel = SshSessionManager.getCachedSession(this.config, this.logCallback).openChannel("sftp");
    try {
      channel.connect(config.getSocketConnectTimeout());
    } catch (JSchException jsch) {
      if (CHANNEL_IS_NOT_OPENED.equals(jsch.getMessage())) {
        log.info("As MaxSessions limit reached, fetching new session for executionId: {}, hostName: {}",
            config.getExecutionId(), config.getHost());
        saveExecutionLog(format("Retry connecting to %s ....", config.getHost()));
        channel = SshSessionManager.getSimplexSession(this.config, this.logCallback).openChannel("sftp");
        channel.connect(config.getSocketConnectTimeout());
      }
    }
    return channel;
  }

  @Override
  public String getAccountId() {
    return config.getAccountId();
  }

  @Override
  public String getCommandUnitName() {
    return config.getCommandUnitName();
  }

  @Override
  public String getExecutionId() {
    return config.getExecutionId();
  }

  @Override
  public String getHost() {
    return config.getHost();
  }

  private void handleException(Exception ex) {
    RuntimeException rethrow = null;
    if (ex instanceof JSchException) {
      saveExecutionLogError("Command execution failed with error " + normalizeError((JSchException) ex));
    } else if (ex instanceof IOException) {
      log.error("Exception in reading InputStream", ex);
    } else if (ex instanceof RuntimeException) {
      rethrow = (RuntimeException) ex;
    }
    int i = 0;
    Throwable t = ex;
    while (t != null && i++ < Misc.MAX_CAUSES) {
      String msg = ExceptionUtils.getMessage(t);
      if (isNotBlank(msg)) {
        saveExecutionLogError(msg);
      }
      t = t instanceof JSchException ? null : t.getCause();
    }
    if (rethrow != null) {
      throw rethrow;
    }
  }

  private void passwordPromptResponder(String line, OutputStream outputStream) throws IOException {
    if (matchesPasswordPromptPattern(line)) {
      if (config.getSudoAppPassword() != null) {
        outputStream.write((new String(config.getSudoAppPassword()) + "\n").getBytes(UTF_8));
        outputStream.flush();
      }
    }
  }

  private boolean matchesPasswordPromptPattern(String line) {
    return sudoPasswordPromptPattern.matcher(line).find();
  }

  private String processStreamData(String text, boolean finishedReading, OutputStream outputStream) throws IOException {
    if (text == null || text.length() == 0) {
      return text;
    }

    String[] lines = lineBreakPattern.split(text);
    if (lines.length == 0) {
      return "";
    }

    for (int i = 0; i < lines.length - 1; i++) { // Ignore last line.
      saveExecutionLog(lines[i]);
    }

    String lastLine = lines[lines.length - 1];
    // last line is processed only if it ends with new line char or stream closed
    if (textEndsAtNewLineChar(text, lastLine) || finishedReading) {
      passwordPromptResponder(lastLine, outputStream);
      saveExecutionLog(lastLine);
      return ""; // nothing left to carry over
    }
    return lastLine;
  }

  private boolean textEndsAtNewLineChar(String text, String lastLine) {
    return lastLine.charAt(lastLine.length() - 1) != text.charAt(text.length() - 1);
  }

  @VisibleForTesting
  public String resolveEnvVarsInPath(String directoryPath) {
    String regex = "(\\$[A-Za-z_-])\\w+";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(directoryPath);
    List<String> envVars = new ArrayList<>();
    while (matcher.find()) {
      envVars.add(matcher.group());
    }
    for (String envVar : envVars) {
      int index = directoryPath.indexOf(envVar);
      if (index > 0 && directoryPath.charAt(index - 1) == '/') {
        directoryPath = directoryPath.replace("/" + envVar, getEnvVarValue(envVar.substring(1)));
      } else {
        directoryPath = directoryPath.replace(envVar, getEnvVarValue(envVar.substring(1)));
      }
    }
    return directoryPath;
  }

  private String getEnvVarValue(String envVar) {
    return System.getenv(envVar);
  }
}
