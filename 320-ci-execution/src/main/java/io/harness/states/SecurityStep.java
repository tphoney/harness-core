/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.states;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.SecurityStepInfo;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(HarnessTeam.STO)
public class SecurityStep extends AbstractStepExecutable {
  public static final StepType STEP_TYPE = SecurityStepInfo.STEP_TYPE;
}
