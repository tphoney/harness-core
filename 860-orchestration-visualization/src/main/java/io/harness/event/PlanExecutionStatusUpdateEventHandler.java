/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationGraph;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.service.GraphGenerationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanExecutionStatusUpdateEventHandler {
  @Inject private PlanExecutionService planExecutionService;
  @Inject private GraphGenerationService graphGenerationService;

  public OrchestrationGraph handleEvent(String planExecutionId, OrchestrationGraph orchestrationGraph) {
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    try {
      if (planExecution.getStatus() == Status.ERRORED) {
        // If plan Execution is ERRORED force generate the graph
        // TODO: Here we need to regenrate Plan Execution Summary too. Till we do not have that at least regenerate the
        // graph. So that pipeline is failed
        log.info("[PMS_GRAPH]  Got Errored execution regenerating the graph final time");
        return graphGenerationService.buildOrchestrationGraph(planExecutionId);
      }
      log.info("[PMS_GRAPH]  Updating Plan Execution with uuid [{}] with status [{}].", planExecution.getUuid(),
          planExecution.getStatus());
      if (planExecution.getEndTs() != null) {
        orchestrationGraph = orchestrationGraph.withEndTs(planExecution.getEndTs());
      }
    } catch (Exception e) {
      log.error("[PMS_GRAPH] Graph update for PLAN_EXECUTION_UPDATE event failed for plan [{}]", planExecutionId, e);
    }
    return orchestrationGraph.withStatus(planExecution.getStatus());
  }
}
