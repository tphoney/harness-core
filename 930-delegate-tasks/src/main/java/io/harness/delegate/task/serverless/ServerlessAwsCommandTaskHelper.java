/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.serverless.ServerlessAwsDeployResult;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.serverless.AbstractExecutable;
import io.harness.serverless.ConfigCredentialCommand;
import io.harness.serverless.DeployCommand;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.model.ServerlessAwsConfig;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class ServerlessAwsCommandTaskHelper {
  public static LogOutputStream getExecutionLogOutputStream(LogCallback executionLogCallback, LogLevel logLevel) {
    return new LogOutputStream() {
      @Override
      protected void processLine(String line) {
        executionLogCallback.saveExecutionLog(line, logLevel);
      }
    };
  }

  public static ProcessResult executeCommand(AbstractExecutable command, String workingDirectory,
      LogCallback executionLogCallback, boolean printCommand) throws Exception {
    try (LogOutputStream logOutputStream = getExecutionLogOutputStream(executionLogCallback, INFO);
         LogOutputStream logErrorStream = getExecutionLogOutputStream(executionLogCallback, ERROR)) {
      return command.execute(workingDirectory, logOutputStream, logErrorStream, printCommand);
    }
  }

  public boolean setServerlessAwsConfigCredentials(ServerlessClient serverlessClient,
      ServerlessAwsConfig serverlessAwsConfig, ServerlessDelegateTaskParams serverlessDelegateTaskParams,
      LogCallback executionLogCallback, boolean overwrite) throws Exception {
    ConfigCredentialCommand command = serverlessClient.configCredential()
                                          .provider(serverlessAwsConfig.getProvider())
                                          .key(serverlessAwsConfig.getAccessKey())
                                          .secret(serverlessAwsConfig.getSecretKey())
                                          .overwrite(overwrite);
    ProcessResult result =
        executeCommand(command, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true);
    if (result.getExitValue() == 0) {
      return true;
    }
    return false;
  }

  public ServerlessAwsDeployResult deploy(ServerlessClient serverlessClient,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, LogCallback executionLogCallback,
      ServerlessAwsDeployConfig serverlessAwsDeployConfig) throws Exception {
    DeployCommand command = serverlessClient.deploy()
                                .region(serverlessAwsDeployConfig.getRegion())
                                .stage(serverlessAwsDeployConfig.getStage())
                                .forceDeployment(serverlessAwsDeployConfig.isForceDeploymentFlag())
                                .awsS3Accelerate(serverlessAwsDeployConfig.isAwsS3AccelerateFlag())
                                .noAwsS3Accelerate(serverlessAwsDeployConfig.isNoAwsS3AccelerateFlag());
    // todo: add other options for deploy command
    ProcessResult result =
        executeCommand(command, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true);
    if (result.getExitValue() == 0) {
      // todo: parse result into java object
    }
    // todo: add error handling
    return null;
  }
}
