/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure;

import com.google.inject.Inject;
import io.harness.azure.client.AzureKubernetesClient;
import io.harness.azure.model.AzureConfig;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.task.ConnectorValidationHandler;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureValidationParams;
import io.harness.errorhandling.NGErrorHelper;


import java.util.Collections;


public class AzureValidationHandler implements ConnectorValidationHandler {

    @Inject
    private NGErrorHelper ngErrorHelper;
    @Inject
    private AzureKubernetesClient azureKubernetesClient;
    @Inject
    private AzureConfigMapper azureConfigMapper;

    @Override
    public ConnectorValidationResult validate(ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
        final AzureValidationParams azureValidationParams = (AzureValidationParams) connectorValidationParams;
        final AzureConnectorDTO connectorDTO = azureValidationParams.getAzureConnectorDTO();
        final AzureManualDetailsDTO config = (AzureManualDetailsDTO) connectorDTO.getCredential().getConfig();


        AzureConfig azureConfig = azureConfigMapper.mapAzureConfigWithDecryption(connectorDTO.getCredential(), connectorDTO.getCredential().getAzureCredentialType(), azureValidationParams.getEncryptedDataDetails());


        return handleValidateTask(azureConfig, config.getSubscription());

    }

    private ConnectorValidationResult handleValidateTask(AzureConfig azureConfig, String subscription) {
        ConnectorValidationResult connectorValidationResult;
        try {
            azureKubernetesClient.listKubernetesClusters(azureConfig, subscription);
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
}
