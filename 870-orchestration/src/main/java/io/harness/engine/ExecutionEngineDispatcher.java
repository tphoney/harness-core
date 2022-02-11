/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Value
@Builder
@Slf4j
public class ExecutionEngineDispatcher implements Runnable {
  Ambiance ambiance;
  OrchestrationEngine orchestrationEngine;

  @Override
  public void run() {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      orchestrationEngine.startNodeExecution(ambiance);
    } catch (Exception exception) {
      log.error("Exception Occurred in ExecutionEngineDispatcher", exception);
      orchestrationEngine.handleError(ambiance, exception);
    }
  }
}
