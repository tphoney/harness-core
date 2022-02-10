/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.NodeDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.pms.resume.EngineResumeCallback;
import io.harness.engine.utils.PmsLevelUtils;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.logging.AutoLogContext;
import io.harness.plan.Node;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SpawnChildrenRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class SpawnChildrenRequestProcessor implements SdkResponseProcessor {
  @Inject private PlanService planService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) private String publisherName;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    SpawnChildrenRequest request = event.getSpawnChildrenRequest();
    Ambiance ambiance = event.getAmbiance();
    String nodeExecutionId = Objects.requireNonNull(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      List<String> callbackIds = new ArrayList<>();
      for (Child child : request.getChildren().getChildrenList()) {
        String uuid = generateUuid();
        callbackIds.add(uuid);
        Node node = planService.fetchNode(ambiance.getPlanId(), child.getChildNodeId());
        Ambiance clonedAmbiance = AmbianceUtils.cloneForChild(ambiance, PmsLevelUtils.buildLevelFromNode(uuid, node));
        executorService.submit(NodeDispatcher.builder().node(node).ambiance(clonedAmbiance).engine(engine).build());
      }

      // Attach a Callback to the parent for the child
      EngineResumeCallback callback = EngineResumeCallback.builder().ambiance(ambiance).build();
      waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));

      // Update the parent with executable response
      nodeExecutionService.updateV2(nodeExecutionId,
          ops
          -> ops.addToSet(NodeExecutionKeys.executableResponses,
              ExecutableResponse.newBuilder().setChildren(request.getChildren()).build()));
    }
  }
}
