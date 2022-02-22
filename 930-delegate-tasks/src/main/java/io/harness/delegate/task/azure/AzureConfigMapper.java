/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.azure.model.AzureConfig;

import io.harness.delegate.beans.connector.azureconnector.AzureConnectorCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.govern.Switch;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.List;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class AzureConfigMapper {

    @Inject
    private SecretDecryptionService secretDecryptionService;

    public AzureConfig mapAzureConfigWithDecryption(
            AzureConnectorCredentialDTO credential, AzureCredentialType azureCredentialType, List<EncryptedDataDetail> encryptionDetails) {
        AzureConfig azureConfig = null;
        switch (azureCredentialType) {
            case MANUAL_CREDENTIALS:
                final AzureManualDetailsDTO config = (AzureManualDetailsDTO) credential.getConfig();
                secretDecryptionService.decrypt(config, encryptionDetails);
                azureConfig = AzureConfig.builder()
                        .clientId(config.getClientId())
                        .tenantId(config.getTenantId())
                        .key(config.getSecretKey().getDecryptedValue())
                        .azureEnvironmentType(AzureEnvironmentType.AZURE)
                        .build();
                break;

            default:
                Switch.unhandled(azureCredentialType);
                break;
        }
        return azureConfig;
    }
}
