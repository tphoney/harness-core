/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.delegatetasks.ExceptionMessageSanitizer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(HarnessTeam.CDP)
public class ServerlessInfraConfigHelper {
  @Inject private SecretDecryptionService secretDecryptionService;

  public void decryptServerlessInfraConfig(ServerlessInfraConfig serverlessInfraConfig) {
    if (serverlessInfraConfig instanceof AwsServerlessInfraConfig) {
      AwsServerlessInfraConfig awsServerlessInfraConfig = (AwsServerlessInfraConfig) serverlessInfraConfig;
      decryptAwsInfraConfig(
          awsServerlessInfraConfig.getAwsConnectorDTO(), awsServerlessInfraConfig.getEncryptionDataDetails());
    }
  }

  private void decryptAwsInfraConfig(AwsConnectorDTO awsConnectorDTO, List<EncryptedDataDetail> encryptedDataDetails) {
    if (awsConnectorDTO.getCredential().getAwsCredentialType() == AwsCredentialType.MANUAL_CREDENTIALS) {
      AwsManualConfigSpecDTO awsCredentialSpecDTO =
          (AwsManualConfigSpecDTO) awsConnectorDTO.getCredential().getConfig();
      secretDecryptionService.decrypt(awsCredentialSpecDTO, encryptedDataDetails);
      ExceptionMessageSanitizer.storeAllSecretsForSanitizing(awsCredentialSpecDTO, encryptedDataDetails);
    }
  }
}
