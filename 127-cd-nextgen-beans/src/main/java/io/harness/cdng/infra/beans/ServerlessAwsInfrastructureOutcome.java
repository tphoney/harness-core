/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.infra.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.steps.environment.EnvironmentOutcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Value
@Builder
@JsonTypeName(InfrastructureKind.SERVERLESS_AWS)
@TypeAlias("cdng.infra.beans.ServerlessAwsInfrastructureOutcome")
@RecasterAlias("io.harness.cdng.infra.beans.ServerlessAwsInfrastructureOutcome")
public class ServerlessAwsInfrastructureOutcome implements InfrastructureOutcome {
  EnvironmentOutcome environment;
  String infrastructureKey; // need to check with infrastructureKey param
  String connectorRef;
  String region;
  String stage; // need to check this once

  @Override
  public String getKind() {
    return InfrastructureKind.SERVERLESS_AWS;
  }
}
