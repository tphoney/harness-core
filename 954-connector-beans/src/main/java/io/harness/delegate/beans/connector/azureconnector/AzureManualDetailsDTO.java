/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.azureconnector;

import com.fasterxml.jackson.annotation.JsonTypeName;

import com.google.common.base.Preconditions;
import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonTypeName(AzureConstants.MANUAL_CONFIG)
@ApiModel("AzureManualDetails")
@Schema(name = "AzureManualDetails", description = "This contains Azure manual credentials connector details")
public class AzureManualDetailsDTO implements AzureCredentialSpecDTO {
    @Schema(description = "Application ID of the Azure App.")
    @NotNull
    private String clientId;
    @SecretReference
    @ApiModelProperty(dataType = "string")
    @Schema(description = "This is the Harness text secret with the Azure authentication key as its value.")
    @NotNull
    private SecretRefData secretKey;
    @NotNull
    @Schema(description = "The Azure Active Directory (AAD) directory ID where you created your application.")
    private String tenantId;
    @Schema(description = "Azure Subscription ID.")
    @NotNull
    private String subscription;

}
