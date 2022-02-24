/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure;

import io.harness.azure.AzureEnvironmentType;
import io.harness.azure.client.AzureManagementClient;
import io.harness.azure.model.AzureConfig;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.task.ConnectorValidationHandler;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.azureconnector.*;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;

public class AzureValidationHandler implements ConnectorValidationHandler {
  @Inject private NGErrorHelper ngErrorHelper;
  @Inject private AzureManagementClient azureManagementClient;
  @Inject private SecretDecryptionService secretDecryptionService;

  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final AzureValidationParams azureValidationParams = (AzureValidationParams) connectorValidationParams;
    final AzureConnectorDTO connectorDTO = azureValidationParams.getAzureConnectorDTO();
    final AzureManualDetailsDTO config = (AzureManualDetailsDTO) connectorDTO.getCredential().getConfig();

    AzureConfig azureConfig =
        mapAzureConfigWithDecryption(connectorDTO.getCredential(), azureValidationParams.getEncryptedDataDetails());

    return handleValidateTask(azureConfig, config.getSubscription());
  }

  private ConnectorValidationResult handleValidateTask(AzureConfig azureConfig, String subscription) {
    ConnectorValidationResult connectorValidationResult;
    try {
      azureManagementClient.validateConnection(azureConfig, subscription);
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.SUCCESS)
                                      .testedAt(System.currentTimeMillis())
                                      .build();

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.FAILURE)
                                      .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)))
                                      .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                                      .testedAt(System.currentTimeMillis())
                                      .build();
    }
    return connectorValidationResult;
  }

  private AzureConfig mapAzureConfigWithDecryption(
      AzureConnectorCredentialDTO credential, List<EncryptedDataDetail> encryptedDataDetails) {
    AzureManualDetailsDTO config = (AzureManualDetailsDTO) credential.getConfig();

    secretDecryptionService.decrypt(config, encryptedDataDetails);
    return AzureConfig.builder()
        .clientId(config.getClientId())
        .tenantId(config.getTenantId())
        .key(config.getSecretKeyRef().getDecryptedValue())
        .azureEnvironmentType(AzureEnvironmentType.AZURE)
        .build();
  }
}
