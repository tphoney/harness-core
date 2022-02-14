/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.execution.strategy;

import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.event.handlers.SdkResponseProcessor;
import io.harness.execution.NodeExecution;
import io.harness.execution.PmsNodeExecutionMetadata;
import io.harness.logging.AutoLogContext;
import io.harness.plan.Node;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.registries.SdkResponseProcessorFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractNodeExecutionStrategy<P extends Node, M extends PmsNodeExecutionMetadata>
    implements NodeExecutionStrategy<P, NodeExecution, M> {
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private SdkResponseProcessorFactory sdkResponseProcessorFactory;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public NodeExecution triggerNode(@NonNull Ambiance ambiance, @NonNull P node, M metadata) {
    String parentId = AmbianceUtils.obtainParentRuntimeId(ambiance);
    String notifyId = parentId == null ? null : AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    NodeExecution save = createNodeExecution(ambiance, node, notifyId, parentId, null);
    // TODO: Should add to an execution queue rather than submitting straight to thread pool
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(ambiance).orchestrationEngine(orchestrationEngine).build());
    return save;
  }

  @Override
  public NodeExecution triggerNextNode(
      @NonNull Ambiance ambiance, @NonNull P node, NodeExecution prevExecution, PmsNodeExecutionMetadata metadata) {
    NodeExecution saved = createNodeExecution(
        ambiance, node, prevExecution.getNotifyId(), prevExecution.getParentId(), prevExecution.getUuid());
    // TODO: Should add to an execution queue rather than submitting straight to thread pool
    executorService.submit(ExecutionEngineDispatcher.builder()
                               .ambiance(saved.getAmbiance())
                               .orchestrationEngine(orchestrationEngine)
                               .build());
    return saved;
  }

  @Override
  public void handleSdkResponseEvent(SdkResponseEventProto event) {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(event.getAmbiance())) {
      log.info("Event for SdkResponseEvent received for eventType {}", event.getSdkResponseEventType());
      SdkResponseProcessor handler = sdkResponseProcessorFactory.getHandler(event.getSdkResponseEventType());
      handler.handleEvent(event);
      log.info("Event for SdkResponseEvent for event type {} completed successfully", event.getSdkResponseEventType());
    }
  }

  public abstract NodeExecution createNodeExecution(
      Ambiance ambiance, P node, String notifyId, String parentId, String previousId);
}
