/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.executable;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.security.PmsSecurityContextEventGuard;

import lombok.SneakyThrows;

@OwnedBy(PIPELINE)
public interface SyncExecutableWithRbac<T extends StepParameters> extends SyncExecutable<T> {
  void validateResources(Ambiance ambiance, T stepParameters);

  @Override
  @SneakyThrows
  default StepResponse executeSync(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData) {
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      validateResources(ambiance, stepParameters);
      return this.executeSyncAfterRbac(ambiance, stepParameters, inputPackage, passThroughData);
    }
  }

  StepResponse executeSyncAfterRbac(
      Ambiance ambiance, T stepParameters, StepInputPackage inputPackage, PassThroughData passThroughData);
}
