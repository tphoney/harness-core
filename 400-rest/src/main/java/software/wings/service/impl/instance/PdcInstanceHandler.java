/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.FeatureName.SSH_PERPETUAL_TASK;
import static io.harness.beans.FeatureName.STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_SSH_DEPLOYMENTS;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.FeatureName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.Base;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostReachabilityResponse;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.InstanceSyncPerpetualTaskCreator;
import software.wings.service.PdcInstanceSyncPerpetualTaskCreator;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.aws.manager.AwsLambdaHelperServiceManager;
import software.wings.service.intfc.instance.ServerlessInstanceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CDP)
public class PdcInstanceHandler extends InstanceHandler implements InstanceSyncByPerpetualTaskHandler {
  @Inject private AwsLambdaHelperServiceManager awsLambdaHelperServiceManager;
  @Inject ArtifactService artifactService;
  @Inject ServerlessInstanceService serverlessInstanceService;
  @Inject DelegateProxyFactory delegateProxyFactory;
  @Inject DelegateService delegateService;
  @Inject private PdcInstanceSyncPerpetualTaskCreator perpetualTaskCreator;

  @Override
  public void syncInstances(String appId, String infraMappingId, InstanceSyncFlow instanceSyncFlow) {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    if (!(infrastructureMapping instanceof PhysicalInfrastructureMapping)) {
      String msg = "Incompatible infra mapping type. Expecting PhysicalInfrastructureMapping, found:"
          + infrastructureMapping.getInfraMappingType();
      log.error(msg);
      throw WingsException.builder().message(msg).build();
    }
    PhysicalInfrastructureMapping physicalInfrastructureMapping = (PhysicalInfrastructureMapping) infrastructureMapping;
    List<String> hosts = physicalInfrastructureMapping.getHostNames();
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getHostConnectionAttrs());
    HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails(hostConnectionAttributes, null, null);

    boolean canUpdateDb = canUpdateInstancesInDb(instanceSyncFlow, infrastructureMapping.getAccountId());
    List<Instance> instances = getInstances(appId, infraMappingId);

    if (instanceSyncFlow != InstanceSyncFlow.PERPETUAL_TASK) {
      updateInstances(hosts, settingAttribute, encryptedDataDetails, instances, canUpdateDb);
    } else {
    }
  }

  private void updateInstances(List<String> hostNames, SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, List<Instance> instances, boolean canUpdateDb) {
    Map<String, Boolean> reachableMap = checkReachability(hostNames, settingAttribute, encryptedDataDetails);

    if (canUpdateDb) {
      Set<String> instancesToRemove = instances.stream()
                                          .filter(i -> {
                                            String hostName = i.getHostInstanceKey().getHostName();
                                            return !reachableMap.containsKey(hostName) || !reachableMap.get(hostName);
                                          })
                                          .map(Base::getUuid)
                                          .collect(Collectors.toSet());
      instanceService.delete(instancesToRemove);
    }
  }

  private Map<String, Boolean> checkReachability(
      List<String> hostNames, SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    Map<String, Boolean> result = new HashMap<>();
    boolean activeInstanceFound = true;
    List<String> filteredHostNames = new ArrayList<>(hostNames);

    while (activeInstanceFound) {
      List<HostReachabilityResponse> hostReachabilityResponses =
          executeTask(settingAttribute.getAccountId(), filteredHostNames, settingAttribute, encryptedDataDetails);
      activeInstanceFound = !EmptyPredicate.isEmpty(hostReachabilityResponses)
          && hostReachabilityResponses.stream().anyMatch(r -> Boolean.TRUE.equals(r.getReachable()));

      if (activeInstanceFound) {
        hostReachabilityResponses.stream()
            .filter(r -> Boolean.TRUE.equals(r.getReachable()))
            .forEach(r -> result.put(r.getHostName(), true));
        filteredHostNames.removeAll(result.keySet());
      }
    }

    filteredHostNames.forEach(h -> result.put(h, false));

    return result;
  }

  private List<HostReachabilityResponse> executeTask(String accountId, List<String> hostNames,
      SettingAttribute settingAttribute, List<EncryptedDataDetail> encryptedDataDetails) {
    HostValidationTaskParameters parameters = HostValidationTaskParameters.builder()
                                                  .hostNames(hostNames)
                                                  .connectionSetting(settingAttribute)
                                                  .encryptionDetails(encryptedDataDetails)
                                                  .checkOnlyReachability(true)
                                                  .checkOr(true)
                                                  .build();

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(accountId)
                                    .data(TaskData.builder()
                                              .async(false)
                                              .taskType(TaskType.HOST_VALIDATION.name())
                                              .parameters(new Object[] {parameters})
                                              .timeout(TimeUnit.MINUTES.toMillis(10))
                                              .build())
                                    .build();

    try {
      DelegateResponseData notifyResponseData = delegateService.executeTask(delegateTask);
      RemoteMethodReturnValueData value = (RemoteMethodReturnValueData) notifyResponseData;
      return (List<HostReachabilityResponse>) value.getReturnValue();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(ex.getMessage(), USER);
    }
  }

  // done
  @Override
  public void handleNewDeployment(
      List<DeploymentSummary> deploymentSummaries, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    throw WingsException.builder().message("Deployments should be handled at InstanceHelper for PDC ssh type.").build();
  }

  // done
  @Override
  public FeatureName getFeatureFlagToStopIteratorBasedInstanceSync() {
    return STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_SSH_DEPLOYMENTS;
  }

  // done
  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact) {
    throw WingsException.builder()
        .message("Deployments should be handled at InstanceHelper for aws ssh type except for with ASG.")
        .build();
  }

  // done
  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    return null;
  }

  // done
  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    // do nothing
  }

  // done
  @Override
  public FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync() {
    return SSH_PERPETUAL_TASK;
  }

  // done
  @Override
  public InstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator() {
    return perpetualTaskCreator;
  }

  @Override
  public void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    System.out.println("here");
  }

  @Override
  public Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    return null;
  }
}
