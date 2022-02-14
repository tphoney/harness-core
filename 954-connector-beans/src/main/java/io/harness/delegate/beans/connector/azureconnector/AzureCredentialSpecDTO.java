/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.azureconnector;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonSubTypes({ @JsonSubTypes.Type(value = GcpManualDetailsDTO.class, name = AzureConstants.MANUAL_CONFIG) })
@ApiModel("AzureCredentialSpec")
@Schema(name = "AzureCredentialSpec", description = "This contains Azure connector credentials spec")
public interface AzureCredentialSpecDTO extends DecryptableEntity {}
