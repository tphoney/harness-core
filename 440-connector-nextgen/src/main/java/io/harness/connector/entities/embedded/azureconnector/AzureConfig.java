/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.entities.embedded.azureconnector;

import io.harness.azure.AzureEnvironmentType;
import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

import static io.harness.azure.AzureEnvironmentType.AZURE;

@Data
@Builder
@FieldNameConstants(innerTypeName = "AzureConfigKeys")
@EqualsAndHashCode(callSuper = false)
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.azureconnector.AzureConfig")
public class AzureConfig extends Connector {
  AzureCredentialType credentialType;
  AzureCredential credential;

  @Builder.Default
  AzureEnvironmentType azureEnvironmentType = AZURE;
}
