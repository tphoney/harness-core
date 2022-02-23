/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.azureconnector;

import static io.harness.ConnectorConstants.INHERIT_FROM_DELEGATE_TYPE_ERROR_MSG;
import static io.harness.azure.AzureEnvironmentType.AZURE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.azure.AzureEnvironmentType;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("AzureConnector")
@Schema(name = "AzureConnector", description = "This contains details of the Azure connector")
public class AzureConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @Valid AzureConnectorCredentialDTO credential;
  Set<String> delegateSelectors;
  @Builder.Default
  @Schema(description = "This specifies the Azure Environment type, which is AZURE by default.")
  private AzureEnvironmentType azureEnvironmentType = AZURE;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    if (credential.getAzureCredentialType() == AzureCredentialType.MANUAL_CREDENTIALS) {
      return Collections.singletonList(credential.getConfig());
    }
    return null;
  }

  @Override
  public void validate() {
    if (AzureCredentialType.INHERIT_FROM_DELEGATE.equals(credential.getAzureCredentialType())
        && isEmpty(delegateSelectors)) {
      throw new InvalidRequestException(INHERIT_FROM_DELEGATE_TYPE_ERROR_MSG);
    }
  }
}
