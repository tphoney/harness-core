/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.vaultconnector;

import static io.harness.SecretManagerDescriptionConstants.AWS_REGION;
import static io.harness.SecretManagerDescriptionConstants.SINK_PATH;
import static io.harness.SecretManagerDescriptionConstants.USE_AWS_IAM;
import static io.harness.SecretManagerDescriptionConstants.VAULT_AWS_IAM_HEADER;
import static io.harness.SecretManagerDescriptionConstants.VAULT_AWS_IAM_ROLE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.encryption.SecretRefHelper.getSecretConfigString;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.exception.WingsException.USER;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.SecretManagerDescriptionConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.AccessType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@OwnedBy(PL)
@Getter
@Setter
@Builder
@ToString(exclude = {"authToken", "secretId", "sinkPath", "xVaultAwsIamServerId"})
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Schema(name = "VaultConnector", description = "This contains the Vault Connector configuration.")
public class VaultConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @SecretReference
  @ApiModelProperty(dataType = "string")
  @Schema(description = SecretManagerDescriptionConstants.AUTH_TOKEN)
  private SecretRefData authToken;
  @Schema(description = SecretManagerDescriptionConstants.BASE_PATH) private String basePath;
  @Schema(description = SecretManagerDescriptionConstants.VAULT_URL) private String vaultUrl;
  @Schema(description = SecretManagerDescriptionConstants.READ_ONLY) private boolean isReadOnly;
  @Schema(description = SecretManagerDescriptionConstants.RENEWAL_INTERVAL_MINUTES) private long renewalIntervalMinutes;
  @Schema(description = SecretManagerDescriptionConstants.ENGINE_ENTERED_MANUALLY)
  private boolean secretEngineManuallyConfigured;
  @Schema(description = SecretManagerDescriptionConstants.SECRET_ENGINE_NAME) private String secretEngineName;
  @Schema(description = SecretManagerDescriptionConstants.APP_ROLE_ID) private String appRoleId;
  @Schema(description = SecretManagerDescriptionConstants.SECRET_ID)
  @SecretReference
  @ApiModelProperty(dataType = "string")
  private SecretRefData secretId;
  private boolean isDefault;
  @Schema(description = SecretManagerDescriptionConstants.SECRET_ENGINE_VERSION) private int secretEngineVersion;
  @Schema(description = SecretManagerDescriptionConstants.DELEGATE_SELECTORS) private Set<String> delegateSelectors;
  @Schema(description = SecretManagerDescriptionConstants.NAMESPACE) private String namespace;
  @Schema(description = SINK_PATH) private String sinkPath;
  @Schema(description = SecretManagerDescriptionConstants.USE_VAULT_AGENT) private boolean useVaultAgent;
  @Schema(description = USE_AWS_IAM) private boolean useAwsIam;
  @Schema(description = AWS_REGION) private String awsRegion;
  @Schema(description = VAULT_AWS_IAM_ROLE) private String vaultAwsIamRole;
  @SecretReference
  @ApiModelProperty(dataType = "string")
  @Schema(description = VAULT_AWS_IAM_HEADER)
  @JsonProperty(value = "xvaultAwsIamServerId")
  private SecretRefData headerAwsIam;

  public AccessType getAccessType() {
    if (useVaultAgent) {
      return AccessType.VAULT_AGENT;
    } else if (useAwsIam) {
      return AccessType.AWS_IAM;
    } else {
      return isNotEmpty(appRoleId) ? AccessType.APP_ROLE : AccessType.TOKEN;
    }
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(this);
  }

  @Override
  public void validate() {
    try {
      new URL(vaultUrl);
    } catch (MalformedURLException malformedURLException) {
      throw new InvalidRequestException("Please check the url and try again.", INVALID_REQUEST, USER);
    }
    if (secretEngineVersion <= 0) {
      throw new InvalidRequestException(
          String.format("Invalid value for secret engine version: %s", secretEngineVersion), INVALID_REQUEST, USER);
    }
    if (renewalIntervalMinutes <= 0) {
      throw new InvalidRequestException(
          String.format("Invalid value for renewal interval: %s", renewalIntervalMinutes), INVALID_REQUEST, USER);
    }
    if (isReadOnly && isDefault) {
      throw new InvalidRequestException("Read only secret manager cannot be set as default", INVALID_REQUEST, USER);
    }
    if (isUseVaultAgent() && isUseAwsIam()) {
      throw new InvalidRequestException(
          "You must use either Vault Agent or Aws Iam Auth method to authenticate. Both can not be used together",
          INVALID_REQUEST, USER);
    }
    if (isUseVaultAgent()) {
      if (isBlank(getSinkPath())) {
        throw new InvalidRequestException(
            "You must provide a sink path to read token if you are using VaultAgent", INVALID_REQUEST, USER);
      }
      if (isEmpty(getDelegateSelectors())) {
        throw new InvalidRequestException(
            "You must provide a delegate selector to read token if you are using VaultAgent", INVALID_REQUEST, USER);
      }
    }
    if (isUseAwsIam()) {
      if (isBlank(getVaultAwsIamRole())) {
        throw new InvalidRequestException(
            "You must provide a vault role if you are using Vault with Aws Iam Auth method", INVALID_REQUEST, USER);
      }
      if (isBlank(getAwsRegion())) {
        throw new InvalidRequestException(
            "You must provide a aws region if you are using Vault with Aws Iam Auth method", INVALID_REQUEST, USER);
      }
      if (isBlank(getSecretConfigString(getHeaderAwsIam()))) {
        throw new InvalidRequestException(
            "You must provide Iam Header Server ID if you are using Vault with Aws Iam Auth method", INVALID_REQUEST,
            USER);
      }
      if (isEmpty(getDelegateSelectors())) {
        throw new InvalidRequestException(
            "You must provide a delegate selector which can connect to vault using Aws IAM auth method",
            INVALID_REQUEST, USER);
      }
    }
  }
}
