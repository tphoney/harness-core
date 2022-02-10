/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.ChildrenExecutableResponse.Child;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.NodeDispatcher;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.engine.pms.resume.EngineResumeCallback;
import io.harness.plan.Node;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.contracts.execution.events.SpawnChildrenRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SpawnChildrenRequestProcessorTest extends OrchestrationTestBase {
  @Mock NodeExecutionService nodeExecutionService;
  @Mock PlanService planService;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @Mock @Named("EngineExecutorService") ExecutorService executorService;

  @Inject @InjectMocks SpawnChildrenRequestProcessor processor;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testHandleSpawnChildrenEvent() throws Exception {
    String planId = generateUuid();
    String planExecutionId = generateUuid();
    String planNodeId = generateUuid();
    String nodeExecutionId = generateUuid();
    String child1Id = generateUuid();
    String child2Id = generateUuid();

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
            .setSdkResponseEventType(SdkResponseEventType.SPAWN_CHILDREN)
            .setSpawnChildrenRequest(
                SpawnChildrenRequest.newBuilder()
                    .setChildren(ChildrenExecutableResponse.newBuilder()
                                     .addChildren(Child.newBuilder().setChildNodeId(child1Id).build())
                                     .addChildren(Child.newBuilder().setChildNodeId(child2Id).build())
                                     .build())
                    .build())
            .setAmbiance(ambiance)
            .build();

    PlanNode node1 = PlanNode.builder()
                         .uuid(child1Id)
                         .name("child1")
                         .identifier(generateUuid())
                         .stepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                         .serviceName("CD")
                         .build();

    PlanNode node2 = PlanNode.builder()
                         .uuid(child2Id)
                         .name("child1")
                         .identifier(generateUuid())
                         .stepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                         .serviceName("CD")
                         .build();

    when(planService.fetchNode(eq(planId), eq(child1Id))).thenReturn(node1);
    when(planService.fetchNode(eq(planId), eq(child2Id))).thenReturn(node2);

    processor.handleEvent(event);

    ArgumentCaptor<io.harness.engine.NodeDispatcher> dispatcher =
        ArgumentCaptor.forClass(io.harness.engine.NodeDispatcher.class);
    verify(executorService, times(2)).submit(dispatcher.capture());

    List<Ambiance> childAmbianceList = dispatcher.getAllValues()
                                           .stream()
                                           .map(io.harness.engine.NodeDispatcher::getAmbiance)
                                           .collect(Collectors.toList());
    assertThat(childAmbianceList).hasSize(2);

    assertThat(childAmbianceList.get(0).getLevelsCount()).isEqualTo(2);
    assertThat(childAmbianceList.get(0).getLevels(1).getSetupId()).isEqualTo(child1Id);

    assertThat(childAmbianceList.get(1).getLevelsCount()).isEqualTo(2);
    assertThat(childAmbianceList.get(1).getLevels(1).getSetupId()).isEqualTo(child2Id);

    List<Node> planNodes = dispatcher.getAllValues().stream().map(NodeDispatcher::getNode).collect(Collectors.toList());
    assertThat(planNodes).hasSize(2);

    assertThat(planNodes.get(0)).isEqualTo(node1);
    assertThat(planNodes.get(1)).isEqualTo(node2);

    ArgumentCaptor<EngineResumeCallback> callbackCaptor = ArgumentCaptor.forClass(EngineResumeCallback.class);
    ArgumentCaptor<String> exIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(waitNotifyEngine).waitForAllOn(any(), callbackCaptor.capture(), exIdCaptor.capture());

    assertThat(callbackCaptor.getValue().getAmbiance()).isEqualTo(ambiance);
    assertThat(childAmbianceList.get(0).getLevels(1).getRuntimeId()).isIn(exIdCaptor.getAllValues());
    assertThat(childAmbianceList.get(1).getLevels(1).getRuntimeId()).isIn(exIdCaptor.getAllValues());

    verify(nodeExecutionService).updateV2(eq(nodeExecutionId), any());
  }
}