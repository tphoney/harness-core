/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.executable;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rollback.RollbackUtility;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class ChildrenExecutableWithRollbackAndRbac<T extends StepParameters>
    implements ChildrenExecutableWithRbac<T> {
  @Inject ExecutionSweepingOutputService executionSweepingOutputService;

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, T stepParameters, Map<String, ResponseData> responseDataMap) {
    RollbackUtility.publishRollbackInformation(ambiance, responseDataMap, executionSweepingOutputService);
    return handleChildrenResponseInternal(ambiance, stepParameters, responseDataMap);
  }

  public abstract StepResponse handleChildrenResponseInternal(
      Ambiance ambiance, T stepParameters, Map<String, ResponseData> responseDataMap);
}
