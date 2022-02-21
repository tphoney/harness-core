/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.k8s.manifest.ManifestHelper.normalizeFolderPath;
import static io.harness.steps.StepUtils.prepareCDTaskRequest;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.steps.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.*;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.serverless.ServerlessGitFetchFileConfig;
import io.harness.delegate.task.serverless.request.ServerlessGitFetchRequest;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.ng.core.NGAccess;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;

import software.wings.beans.TaskType;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class ServerlessStepHelper extends CDStepHelper {
  @Inject private OutcomeService outcomeService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private ServerlessEntityHelper serverlessEntityHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StepHelper stepHelper;

  public TaskChainResponse startChainLink(
      ServerlessStepExecutor serverlessStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters) {
    ManifestsOutcome manifestsOutcome = resolveServerlessManifestsOutcome(ambiance);
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    ExpressionEvaluatorUtils.updateExpressions(
        manifestsOutcome, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    validateManifestsOutcome(ambiance, manifestsOutcome);
    ManifestOutcome serverlessManifestOutcome = getServerlessSupportedManifestOutcome(manifestsOutcome.values());
    return prepareServerlessManifestFetchTask(
        serverlessStepExecutor, serverlessManifestOutcome, ambiance, stepElementParameters, infrastructureOutcome);
  }

  public ManifestsOutcome resolveServerlessManifestsOutcome(Ambiance ambiance) {
    OptionalOutcome manifestsOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));

    if (!manifestsOutcome.isFound()) {
      String stageName = AmbianceUtils.getStageLevelFromAmbiance(ambiance)
                             .map(level -> level.getIdentifier())
                             .orElse("Deployment stage");
      String stepType =
          Optional.ofNullable(AmbianceUtils.getCurrentStepType(ambiance)).map(StepType::getType).orElse("Serverless");
      throw new GeneralException(format(
          "No manifests found in stage %s. %s step requires at least one manifest defined in stage service definition",
          stageName, stepType));
    }
    return (ManifestsOutcome) manifestsOutcome.getOutcome();
  }

  private TaskChainResponse prepareServerlessManifestFetchTask(ServerlessStepExecutor serverlessStepExecutor,
      ManifestOutcome manifestOutcome, Ambiance ambiance, StepElementParameters stepElementParameters,
      InfrastructureOutcome infrastructureOutcome) {
    ServerlessManifestOutcome serverlessManifestOutcome = (ServerlessManifestOutcome) manifestOutcome;
    StoreConfig storeConfig = serverlessManifestOutcome.getStore();
    if (ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      return prepareGitFetchManifestTaskChainResponse(storeConfig, ambiance, stepElementParameters,
          infrastructureOutcome, serverlessManifestOutcome, manifestOutcome);
    } else {
      throw new InvalidRequestException("Invalid kind of storeConfig for Serverless step", USER);
    }
  }

  private TaskChainResponse prepareGitFetchManifestTaskChainResponse(StoreConfig storeConfig, Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructureOutcome,
      ServerlessManifestOutcome serverlessManifestOutcome, ManifestOutcome manifestOutcome) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig;
    ServerlessGitFetchFileConfig serverlessGitFetchFileConfig =
        mapServerlessManifestToGitFetchFileConfig(ambiance, manifestOutcome, serverlessManifestOutcome, gitStoreConfig);
    ServerlessStepPassThroughData serverlessStepPassThroughData =
        ServerlessStepPassThroughData.builder()
            .serverlessManifestOutcome(serverlessManifestOutcome)
            .infrastructureOutcome(infrastructureOutcome)
            .build();
    return getGitFetchFileTaskResponse(
        ambiance, true, stepElementParameters, serverlessStepPassThroughData, serverlessGitFetchFileConfig);
  }

  private TaskChainResponse getGitFetchFileTaskResponse(Ambiance ambiance, boolean shouldOpenLogStream,
      StepElementParameters stepElementParameters, ServerlessStepPassThroughData serverlessStepPassThroughData,
      ServerlessGitFetchFileConfig serverlessGitFetchFilesConfig) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    ServerlessGitFetchRequest serverlessGitFetchRequest =
        ServerlessGitFetchRequest.builder()
            .accountId(accountId)
            .serverlessGitFetchFileConfig(serverlessGitFetchFilesConfig)
            .shouldOpenLogStream(shouldOpenLogStream)
            .build();
    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.SERVERLESS_GIT_FETCH_TASK_NG.name())
                                  .parameters(new Object[] {serverlessGitFetchRequest})
                                  .build();
    String taskName = TaskType.SERVERLESS_GIT_FETCH_TASK_NG.getDisplayName();
    ServerlessSpecParameters serverlessSpecParameters = (ServerlessSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest =
        prepareCDTaskRequest(ambiance, taskData, kryoSerializer, serverlessSpecParameters.getCommandUnits(), taskName,
            TaskSelectorYaml.toTaskSelector(
                emptyIfNull(getParameterFieldValue(serverlessSpecParameters.getDelegateSelectors()))),
            stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(serverlessStepPassThroughData)
        .build();
  }

  private ServerlessGitFetchFileConfig mapServerlessManifestToGitFetchFileConfig(Ambiance ambiance,
      ManifestOutcome manifestOutcome, ServerlessManifestOutcome serverlessManifestOutcome,
      GitStoreConfig gitStoreConfig) {
    String validationMessage = format("Serverless manifest with Id [%s]", serverlessManifestOutcome.getIdentifier());
    return getManifestGitFetchFilesConfig(
        ambiance, validationMessage, serverlessManifestOutcome, gitStoreConfig, manifestOutcome);
  }

  private ServerlessGitFetchFileConfig getManifestGitFetchFilesConfig(Ambiance ambiance, String validationMessage,
      ServerlessManifestOutcome serverlessManifestOutcome, GitStoreConfig gitStoreConfig,
      ManifestOutcome manifestOutcome) {
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = getConnectorDTO(connectorId, ambiance);
    validateManifest(gitStoreConfig.getKind(), connectorDTO, validationMessage);
    List<String> gitPaths = getFolderPathsForManifest(gitStoreConfig);
    GitStoreDelegateConfig gitStoreDelegateConfig =
        getGitStoreDelegateConfig(gitStoreConfig, connectorDTO, manifestOutcome, gitPaths, ambiance);
    return ServerlessGitFetchFileConfig.builder()
        .gitStoreDelegateConfig(gitStoreDelegateConfig)
        .identifier(serverlessManifestOutcome.getIdentifier())
        .manifestType(ManifestType.Serverless)
        .configOverridePath(getParameterFieldValue(serverlessManifestOutcome.getConfigOverridePath()))
        .succeedIfFileNotFound(false)
        .build();
  }

  @VisibleForTesting
  public ManifestOutcome getServerlessSupportedManifestOutcome(@NotEmpty Collection<ManifestOutcome> manifestOutcomes) {
    List<ManifestOutcome> serverlessManifests =
        manifestOutcomes.stream()
            .filter(manifestOutcome -> ManifestType.Serverless.equals(manifestOutcome.getType()))
            .collect(Collectors.toList());
    if (isEmpty(serverlessManifests)) {
      throw new InvalidRequestException("Manifests are mandatory for Serverless step", USER);
    }
    if (serverlessManifests.size() > 1) {
      throw new InvalidRequestException("There can be only a single manifest for Serverless step", USER);
    }
    return serverlessManifests.get(0);
  }

  private ConnectorInfoDTO getConnectorDTO(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    return serverlessEntityHelper.getConnectorInfoDTO(connectorId, ngAccess);
  }

  private List<String> getFolderPathsForManifest(GitStoreConfig gitStoreConfig) {
    List<String> folderPaths = new ArrayList<>();
    String folderPath = getParameterFieldValue(gitStoreConfig.getFolderPath());
    folderPaths.add(normalizeFolderPath(folderPath));
    return folderPaths;
  }
}
