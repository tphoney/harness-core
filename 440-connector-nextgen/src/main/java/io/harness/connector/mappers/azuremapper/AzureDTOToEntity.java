/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.azuremapper;

import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.azureconnector.AzureConfig;
import io.harness.connector.entities.embedded.azureconnector.AzureManualCredential;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;

import io.harness.delegate.beans.connector.azureconnector.*;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class AzureDTOToEntity implements ConnectorDTOToEntityMapper<AzureConnectorDTO, AzureConfig> {
  @Override
  public AzureConfig toConnectorEntity(AzureConnectorDTO connectorDTO) {
    final AzureConnectorCredentialDTO credential = connectorDTO.getCredential();
    final AzureCredentialType credentialType = credential.getAzureCredentialType();
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        return buildInheritFromDelegate(credential);
      case MANUAL_CREDENTIALS:
        return buildManualCredential(credential);
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
  }

  private AzureConfig buildInheritFromDelegate(AzureConnectorCredentialDTO connector) {
    return AzureConfig.builder().credentialType(AzureCredentialType.INHERIT_FROM_DELEGATE).credential(null).build();
  }

  private AzureConfig buildManualCredential(AzureConnectorCredentialDTO connector) {
    final AzureManualDetailsDTO config = (AzureManualDetailsDTO) connector.getConfig();
    final String secretKeyRef = SecretRefHelper.getSecretConfigString(config.getSecretKey());
    AzureManualCredential azureManualCredential = AzureManualCredential.builder()
            .subscription(config.getSubscription())
            .tenantId(config.getTenantId())
            .clientId(config.getClientId())
            .secretKeyRef(secretKeyRef)
            .build();
    return AzureConfig.builder().credentialType(AzureCredentialType.MANUAL_CREDENTIALS).credential(azureManualCredential).build();
  }
}
