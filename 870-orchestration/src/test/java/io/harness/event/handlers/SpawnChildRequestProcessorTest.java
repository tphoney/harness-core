/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.pms.resume.EngineResumeCallback;
import io.harness.plan.Node;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SpawnChildRequestProcessorTest extends OrchestrationTestBase {
  @Mock NodeExecutionService nodeExecutionService;
  @Mock PlanService planService;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @Mock OrchestrationEngine orchestrationEngine;

  @Inject @InjectMocks SpawnChildRequestProcessor processor;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testHandleSpawnChildEvent() {
    String planId = generateUuid();
    String planExecutionId = generateUuid();
    String planNodeId = generateUuid();
    String nodeExecutionId = generateUuid();
    String child1Id = generateUuid();

    Ambiance ambiance =
        Ambiance.newBuilder()
            .setPlanId(planId)
            .setPlanExecutionId(planExecutionId)
            .addLevels(
                Level.newBuilder()
                    .setIdentifier("IDENTIFIER")
                    .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.FORK).build())
                    .setRuntimeId(nodeExecutionId)
                    .setSetupId(planNodeId)
                    .build())
            .build();

    SdkResponseEventProto event =
        SdkResponseEventProto.newBuilder()
            .setSdkResponseEventType(SdkResponseEventType.SPAWN_CHILD)
            .setSpawnChildRequest(SpawnChildRequest.newBuilder()
                                      .setChild(ChildExecutableResponse.newBuilder().setChildNodeId(child1Id).build())
                                      .build())
            .setAmbiance(ambiance)
            .build();

    PlanNode node = PlanNode.builder()
                        .uuid(child1Id)
                        .name("child1")
                        .identifier(generateUuid())
                        .stepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                        .serviceName("CD")
                        .build();
    when(planService.fetchNode(eq(planId), eq(child1Id))).thenReturn(node);

    processor.handleEvent(event);

    ArgumentCaptor<Ambiance> ambianceCaptor = ArgumentCaptor.forClass(Ambiance.class);
    ArgumentCaptor<PlanNode> nodeCaptor = ArgumentCaptor.forClass(PlanNode.class);
    verify(orchestrationEngine).triggerNode(ambianceCaptor.capture(), nodeCaptor.capture(), eq(null));

    Ambiance childAmbiance = ambianceCaptor.getValue();

    assertThat(childAmbiance.getLevelsCount()).isEqualTo(2);
    assertThat(childAmbiance.getLevels(1).getSetupId()).isEqualTo(child1Id);

    Node planNode = nodeCaptor.getValue();
    assertThat(planNode).isEqualTo(node);

    ArgumentCaptor<EngineResumeCallback> callbackCaptor = ArgumentCaptor.forClass(EngineResumeCallback.class);
    ArgumentCaptor<String> exIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(waitNotifyEngine).waitForAllOn(any(), callbackCaptor.capture(), exIdCaptor.capture());

    assertThat(callbackCaptor.getValue().getAmbiance()).isEqualTo(ambiance);
    assertThat(childAmbiance.getLevels(1).getRuntimeId()).isEqualTo(exIdCaptor.getValue());

    verify(nodeExecutionService).updateV2(eq(nodeExecutionId), any());
  }
}
