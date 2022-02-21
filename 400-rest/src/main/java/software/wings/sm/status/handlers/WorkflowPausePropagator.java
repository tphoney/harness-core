/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.status.handlers;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RUNNING;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EventType;
import io.harness.beans.WorkflowType;

import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.sm.status.StateStatusUpdateInfo;
import software.wings.sm.status.WorkflowStatusPropagator;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class WorkflowPausePropagator implements WorkflowStatusPropagator {
  @Inject private WorkflowStatusPropagatorHelper propagatorHelper;
  @Inject private WorkflowExecutionUpdate workflowExecutionUpdate;

  @Override
  public void handleStatusUpdate(StateStatusUpdateInfo updateInfo) {
    String appId = updateInfo.getAppId();
    WorkflowExecution updatedExecution =
        propagatorHelper.updateStatus(appId, updateInfo.getWorkflowExecutionId(), asList(QUEUED, RUNNING), PAUSED);
    if (updatedExecution == null) {
      log.info("Updating status to paused failed for execution id: {}", updateInfo.getWorkflowExecutionId());
    } else {
      workflowExecutionUpdate.publish(updatedExecution);
      if (WorkflowType.PIPELINE.equals(updatedExecution.getWorkflowType())) {
        propagatorHelper.refreshPipelineExecution(updatedExecution);
        workflowExecutionUpdate.publish(updatedExecution, updateInfo, EventType.PIPELINE_PAUSE);
      } else {
        workflowExecutionUpdate.publish(updatedExecution, updateInfo, EventType.WORKFLOW_PAUSE);
      }
    }

    WorkflowExecution execution = propagatorHelper.obtainExecution(appId, updateInfo.getWorkflowExecutionId());
    // We need to refresh the pipeline execution because the pipelineExecution field only gets set when a call from UI
    // is made.
    propagatorHelper.refreshPipelineExecution(execution.getAccountId(), appId, execution.getPipelineExecutionId());
    if (propagatorHelper.shouldPausePipeline(appId, execution.getPipelineExecutionId())) {
      WorkflowExecution pipelineExecution =
          propagatorHelper.updateStatus(appId, execution.getPipelineExecutionId(), singletonList(RUNNING), PAUSED);
      if (pipelineExecution == null) {
        log.info("Updating status to paused failed for Pipeline with id: {}", execution.getPipelineExecution());
      } else {
        workflowExecutionUpdate.publish(pipelineExecution, updateInfo, EventType.PIPELINE_PAUSE);
      }
    }
  }
}
