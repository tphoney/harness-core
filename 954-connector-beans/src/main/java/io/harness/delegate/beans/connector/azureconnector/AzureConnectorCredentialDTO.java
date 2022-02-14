/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.azureconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialDTODeserializer;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AzureConnectorCredential")
@JsonDeserialize(using = AzureCredentialDTODeserializer.class)
@Schema(name = "AzureConnectorCredential", description = "This contains Azure connector credentials")
public class AzureConnectorCredentialDTO {
    @NotNull
    @JsonProperty("type")
    AzureCredentialType azureCredentialType;
    @JsonProperty("spec")
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
    @Valid
    AzureCredentialSpecDTO config;

    @Builder
    public AzureConnectorCredentialDTO(AzureCredentialType azureCredentialType, AzureCredentialSpecDTO config) {
        this.azureCredentialType = azureCredentialType;
        this.config = config;
    }
}
