/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.policy.step;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.policy.PolicyStepInfo;

@OwnedBy(PIPELINE)
public class PolicyStep implements SyncExecutable<PolicyStepInfo> {
  public static StepType STEP_TYPE =
      StepType.newBuilder().setType(StepSpecTypeConstants.POLICY_STEP).setStepCategory(StepCategory.STEP).build();

  @Override
  public StepResponse executeSync(Ambiance ambiance, PolicyStepInfo stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    return null;
  }

  @Override
  public Class<PolicyStepInfo> getStepParametersClass() {
    return PolicyStepInfo.class;
  }
}
