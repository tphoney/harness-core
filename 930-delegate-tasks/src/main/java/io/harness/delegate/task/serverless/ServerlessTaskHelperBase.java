/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.filesystem.FileIo.*;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.ScmFetchFilesHelperNG;
import io.harness.filesystem.FileIo;
import io.harness.logging.LogCallback;
import io.harness.serverless.model.ServerlessDelegateTaskParams;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class ServerlessTaskHelperBase {
  @Inject private ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;
  @Inject private ScmFetchFilesHelperNG scmFetchFilesHelper;
  @Inject private GitDecryptionHelper gitDecryptionHelper;
  @Inject private NGGitService ngGitService;

  public LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }
  public void createHomeDirectory(String directoryPath) throws IOException {
    createDirectoryIfDoesNotExist(directoryPath);
    waitForDirectoryToBeAccessibleOutOfProcess(directoryPath, 10);
  }

  public void fetchManifestFilesAndWriteToDirectory(ServerlessManifestConfig serverlessManifestConfig, String accountId,
      LogCallback executionLogCallback, ServerlessDelegateTaskParams serverlessDelegateTaskParams) {
    GitStoreDelegateConfig gitStoreDelegateConfig =
        serverlessManifestConfig.getServerlessGitFetchFileConfig().getGitStoreDelegateConfig();
    downloadFilesFromGit(
        gitStoreDelegateConfig, executionLogCallback, accountId, serverlessDelegateTaskParams.getWorkingDirectory());
    // todo: print file download statements
  }

  private void downloadFilesFromGit(GitStoreDelegateConfig gitStoreDelegateConfig, LogCallback executionLogCallback,
      String accountId, String workingDirectory) {
    try {
      // todo: print git config files
      if (gitStoreDelegateConfig.isOptimizedFilesFetch()) {
        executionLogCallback.saveExecutionLog("Using optimized file fetch");
        serverlessGitFetchTaskHelper.decryptGitStoreConfig(gitStoreDelegateConfig);
        scmFetchFilesHelper.downloadFilesUsingScm(workingDirectory, gitStoreDelegateConfig, executionLogCallback);
      } else {
        GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO(gitStoreDelegateConfig.getGitConfigDTO());
        gitDecryptionHelper.decryptGitConfig(gitConfigDTO, gitStoreDelegateConfig.getEncryptedDataDetails());
        SshSessionConfig sshSessionConfig = gitDecryptionHelper.getSSHSessionConfig(
            gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());
        ngGitService.downloadFiles(gitStoreDelegateConfig, workingDirectory, accountId, sshSessionConfig, gitConfigDTO);
      }
      // todo: add print statements for fetched directory
    } catch (Exception e) {
    }
  }
  public boolean replaceManifestWithRenderedContent(
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, ServerlessManifestConfig serverlessManifestConfig) {
    String updatedManifestContent = serverlessManifestConfig.getManifestContent();
    String manifestFilePath =
        Paths.get(serverlessDelegateTaskParams.getWorkingDirectory(), serverlessManifestConfig.getManifestPath())
            .toString();
    // todo: add suitable print statements
    // todo: validate content and file
    try {
      updateManifestFileContent(manifestFilePath, updatedManifestContent);
      return true;
    } catch (IOException e) {
    }
    return false;
  }

  private void updateManifestFileContent(String manifestFilePath, String manifestContent) throws IOException {
    FileIo.deleteFileIfExists(manifestFilePath);
    FileIo.writeUtf8StringToFile(manifestFilePath, manifestContent);
  }
}
