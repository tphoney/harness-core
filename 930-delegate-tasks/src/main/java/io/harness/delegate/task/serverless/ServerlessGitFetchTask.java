/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.serverless.request.ServerlessGitFetchRequest;
import io.harness.git.model.FetchFilesResult;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.secret.SecretSanitizerThreadLocal;
import io.harness.serverless.ServerlessCommandUnitConstants;

import com.google.inject.Inject;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ServerlessGitFetchTask extends AbstractDelegateRunnableTask {
  @Inject private GitDecryptionHelper gitDecryptionHelper;
  @Inject private ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;
  public ServerlessGitFetchTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
    SecretSanitizerThreadLocal.addAll(delegateTaskPackage.getSecrets());
  }
  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();
    try {
      ServerlessGitFetchRequest serverlessGitFetchRequest = (ServerlessGitFetchRequest) parameters;
      log.info("Running Serverless GitFetchFilesTask for activityId {}", serverlessGitFetchRequest.getActivityId());

      LogCallback executionLogCallback =
          new NGDelegateLogCallback(getLogStreamingTaskClient(), ServerlessCommandUnitConstants.fetchFiles.toString(),
              serverlessGitFetchRequest.isShouldOpenLogStream(), commandUnitsProgress);
      ServerlessGitFetchFileConfig serverlessGitFetchFileConfig =
          serverlessGitFetchRequest.getServerlessGitFetchFileConfig();
      executionLogCallback.saveExecutionLog(
          color(format("Fetching %s files with identifier: %s", serverlessGitFetchFileConfig.getManifestType(),
                    serverlessGitFetchFileConfig.getIdentifier()),
              White, Bold));
      Map<String, FetchFilesResult> filesFromMultipleRepo = new HashMap<>();
      FetchFilesResult filesResult = fetchManifestFile(
          serverlessGitFetchFileConfig, executionLogCallback, serverlessGitFetchRequest.getAccountId());
      filesFromMultipleRepo.put(serverlessGitFetchFileConfig.getIdentifier(), filesResult);
      if (serverlessGitFetchRequest.isCloseLogStream()) {
        executionLogCallback.saveExecutionLog("Done.", INFO, CommandExecutionStatus.SUCCESS);
      }
      return GitFetchResponse.builder()
          .taskStatus(TaskStatus.SUCCESS)
          .filesFromMultipleRepo(filesFromMultipleRepo)
          .unitProgressData(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress))
          .build();

    } catch (Exception e) {
      log.error("Exception in Git Fetch Files Task", e);
      throw new TaskNGDataException(UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress), e);
    }
  }

  private FetchFilesResult fetchManifestFile(ServerlessGitFetchFileConfig serverlessGitFetchFileConfig,
      LogCallback executionLogCallback, String accountId) throws NoSuchFileException {
    GitStoreDelegateConfig gitStoreDelegateConfig = serverlessGitFetchFileConfig.getGitStoreDelegateConfig();
    executionLogCallback.saveExecutionLog("Git connector Url: " + gitStoreDelegateConfig.getGitConfigDTO().getUrl());
    String fetchTypeInfo;
    GitConfigDTO gitConfigDTO = null;
    if (gitStoreDelegateConfig.getFetchType() == FetchType.BRANCH) {
      fetchTypeInfo = "Branch: " + gitStoreDelegateConfig.getBranch();
    } else {
      fetchTypeInfo = "Commit: " + gitStoreDelegateConfig.getCommitId();
    }
    executionLogCallback.saveExecutionLog(fetchTypeInfo);
    if (gitStoreDelegateConfig.isOptimizedFilesFetch()) {
      executionLogCallback.saveExecutionLog("Using optimized file fetch ");
      serverlessGitFetchTaskHelper.decryptGitStoreConfig(gitStoreDelegateConfig);
    } else {
      gitConfigDTO = ScmConnectorMapper.toGitConfigDTO(gitStoreDelegateConfig.getGitConfigDTO());
      gitDecryptionHelper.decryptGitConfig(gitConfigDTO, gitStoreDelegateConfig.getEncryptedDataDetails());
    }
    FetchFilesResult filesResult = null;
    try {
      if (EmptyPredicate.isNotEmpty(gitStoreDelegateConfig.getPaths())) {
        String folderPath = serverlessGitFetchFileConfig.getGitStoreDelegateConfig().getPaths().get(0);
        if (EmptyPredicate.isNotEmpty(serverlessGitFetchFileConfig.getConfigOverridePath())) {
          filesResult = fetchManifestFileFromRepo(gitStoreDelegateConfig, folderPath,
              serverlessGitFetchFileConfig.getConfigOverridePath(), accountId, gitConfigDTO, executionLogCallback);
        } else {
          filesResult = fetchManifestFileInPriorityOrder(
              gitStoreDelegateConfig, folderPath, accountId, gitConfigDTO, executionLogCallback);
        }
      }
    } catch (Exception e) {
      String msg = "Exception in processing GitFetchFilesTask. " + e.getMessage();
      if (e.getCause() instanceof NoSuchFileException) {
        log.error(msg, e);
        executionLogCallback.saveExecutionLog(
            color(format("No manifest file found with identifier: %s.", serverlessGitFetchFileConfig.getIdentifier()),
                White));
      }
      executionLogCallback.saveExecutionLog(msg, ERROR, CommandExecutionStatus.FAILURE);
      throw e;
    }
    return filesResult;
  }

  private FetchFilesResult fetchManifestFileInPriorityOrder(GitStoreDelegateConfig gitStoreDelegateConfig,
      String folderPath, String accountId, GitConfigDTO gitConfigDTO, LogCallback executionLogCallback)
      throws NoSuchFileException {
    // todo: // optimize in such a way fetching of files from git happens only once
    Optional<FetchFilesResult> serverlessManifestFileResult;
    serverlessManifestFileResult = fetchServerlessManifestFileFromRepo(
        gitStoreDelegateConfig, folderPath, "serverless.yaml", accountId, gitConfigDTO, executionLogCallback);
    if (serverlessManifestFileResult.isPresent()) {
      return serverlessManifestFileResult.get();
    }
    serverlessManifestFileResult = fetchServerlessManifestFileFromRepo(
        gitStoreDelegateConfig, folderPath, "serverless.yml", accountId, gitConfigDTO, executionLogCallback);
    if (serverlessManifestFileResult.isPresent()) {
      return serverlessManifestFileResult.get();
    }
    serverlessManifestFileResult = fetchServerlessManifestFileFromRepo(
        gitStoreDelegateConfig, folderPath, "serverless.json", accountId, gitConfigDTO, executionLogCallback);
    if (serverlessManifestFileResult.isPresent()) {
      return serverlessManifestFileResult.get();
    }
    throw new NoSuchFileException("No Serverless Manifest Found");
  }

  private Optional<FetchFilesResult> fetchServerlessManifestFileFromRepo(GitStoreDelegateConfig gitStoreDelegateConfig,
      String folderPath, String filePath, String accountId, GitConfigDTO gitConfigDTO,
      LogCallback executionLogCallback) {
    try {
      return Optional.of(fetchManifestFileFromRepo(
          gitStoreDelegateConfig, folderPath, filePath, accountId, gitConfigDTO, executionLogCallback));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private FetchFilesResult fetchManifestFileFromRepo(GitStoreDelegateConfig gitStoreDelegateConfig, String folderPath,
      String filePath, String accountId, GitConfigDTO gitConfigDTO, LogCallback executionLogCallback) {
    filePath = serverlessGitFetchTaskHelper.getCompleteFilePath(folderPath, filePath);
    List<String> filePaths = Collections.singletonList(filePath);
    FetchFilesResult fetchFilesResult =
        serverlessGitFetchTaskHelper.fetchFileFromRepo(gitStoreDelegateConfig, filePaths, accountId, gitConfigDTO);
    serverlessGitFetchTaskHelper.printFileNames(executionLogCallback, filePaths);
    return fetchFilesResult;
  }
}
