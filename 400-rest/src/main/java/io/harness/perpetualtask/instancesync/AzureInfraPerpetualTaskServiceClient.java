/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.perpetualtask.instancesync;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.exception.InvalidRequestException;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.AwsUtils;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public class AzureInfraPerpetualTaskServiceClient implements PerpetualTaskServiceClient {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private SecretManager secretManager;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private AwsUtils awsUtils;
  @Inject private SettingsService settingsService;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    final PerpetualTaskData taskData = getPerpetualTaskData(clientContext);

    ByteString configBytes = ByteString.copyFrom(kryoSerializer.asBytes(taskData.getAzureConfig()));
    ByteString infraMappingBytes =
        ByteString.copyFrom(kryoSerializer.asBytes(taskData.getAzureInfrastructureMapping()));
    ByteString encryptionDetailsBytes = ByteString.copyFrom(kryoSerializer.asBytes(taskData.getEncryptionDetails()));

    return AzureInfraInstanceSyncPerpetualTaskParams.newBuilder()
        .setAzureConfig(configBytes)
        .setAzureInfrastructureMapping(infraMappingBytes)
        .setEncryptedData(encryptionDetailsBytes)
        .build();
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    final PerpetualTaskData taskData = getPerpetualTaskData(clientContext);

    // TODO - create validation task here.
    return DelegateTask.builder().build();
  }

  private PerpetualTaskData getPerpetualTaskData(PerpetualTaskClientContext clientContext) {
    String infraMappingId = getInfraMappingId(clientContext);
    String appId = getAppId(clientContext);

    InfrastructureMapping infraMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (!(infraMapping instanceof AzureInfrastructureMapping)) {
      String msg = "Incompatible infra mapping type. Expecting AzureInfrastructureMapping type. Found:"
          + infraMapping.getInfraMappingType();
      log.error(msg);
      throw new InvalidRequestException(msg);
    }

    AzureInfrastructureMapping azureInfrastructureMapping = (AzureInfrastructureMapping) infraMapping;
    SettingAttribute cloudProviderSetting =
        settingsService.get(azureInfrastructureMapping.getComputeProviderSettingId());
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) cloudProviderSetting.getValue(), null, null);

    return PerpetualTaskData.builder()
        .azureInfrastructureMapping(azureInfrastructureMapping)
        .azureConfig((AzureConfig) cloudProviderSetting.getValue())
        .encryptionDetails(encryptionDetails)
        .build();
  }

  private String getAppId(PerpetualTaskClientContext clientContext) {
    return clientContext.getClientParams().get(InstanceSyncConstants.HARNESS_APPLICATION_ID);
  }

  private String getInfraMappingId(PerpetualTaskClientContext clientContext) {
    return clientContext.getClientParams().get(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID);
  }

  @Data
  @Builder
  private static class PerpetualTaskData {
    private AzureInfrastructureMapping azureInfrastructureMapping;
    private AzureConfig azureConfig;
    private List<EncryptedDataDetail> encryptionDetails;
  }
}
