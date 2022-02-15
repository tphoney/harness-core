/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless.request;

import static io.harness.delegate.beans.connector.awsconnector.AwsCredentialType.MANUAL_CREDENTIALS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.awsconnector.AwsCapabilityHelper;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.serverless.ServerlessAwsInfraConfig;
import io.harness.delegate.task.serverless.ServerlessCliVersion;
import io.harness.delegate.task.serverless.ServerlessCommandType;
import io.harness.delegate.task.serverless.ServerlessInfraConfig;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDP)
public interface ServerlessCommandRequest extends TaskParameters, ExecutionCapabilityDemander {
  String getAccountId();
  String getAppId();
  String getActivityId();
  @NotEmpty ServerlessCommandType getServerlessCommandType();
  String getCommandName();
  CommandUnitsProgress getCommandUnitsProgress();
  ServerlessCliVersion getServerlessCliVersion();
  ServerlessInfraConfig getServerlessInfraConfig();
  // todo: add timeout

  @Override
  default List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    ServerlessInfraConfig serverlessInfraConfig = getServerlessInfraConfig();
    List<EncryptedDataDetail> cloudProviderEncryptionDetails = serverlessInfraConfig.getEncryptionDataDetails();

    List<ExecutionCapability> capabilities =
        new ArrayList<>(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
            cloudProviderEncryptionDetails, maskingEvaluator));
    if (serverlessInfraConfig instanceof ServerlessAwsInfraConfig) {
      AwsConnectorDTO awsConnectorDTO = ((ServerlessAwsInfraConfig) serverlessInfraConfig).getAwsConnectorDTO();
      if (awsConnectorDTO.getCredential().getAwsCredentialType() != MANUAL_CREDENTIALS) {
        throw new UnknownEnumTypeException(
            "AWS Credential Type", String.valueOf(awsConnectorDTO.getCredential().getAwsCredentialType()));
      }
      capabilities.addAll(AwsCapabilityHelper.fetchRequiredExecutionCapabilities(awsConnectorDTO, maskingEvaluator));
    }
    // todo:sls installation capability
    return capabilities;
  }
}
