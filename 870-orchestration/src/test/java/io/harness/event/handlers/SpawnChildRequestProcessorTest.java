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
import io.harness.engine.NodeDispatcher;
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
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class SpawnChildRequestProcessorTest extends OrchestrationTestBase {
  @Mock NodeExecutionService nodeExecutionService;
  @Mock PlanService planService;
  @Mock WaitNotifyEngine waitNotifyEngine;
  @Mock @Named("EngineExecutorService") ExecutorService executorService;

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

    PlanNode node1 = PlanNode.builder()
                         .uuid(child1Id)
                         .name("child1")
                         .identifier(generateUuid())
                         .stepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                         .serviceName("CD")
                         .build();

    when(planService.fetchNode(eq(planId), eq(child1Id))).thenReturn(node1);

    processor.handleEvent(event);

    ArgumentCaptor<io.harness.engine.NodeDispatcher> dispatcher = ArgumentCaptor.forClass(NodeDispatcher.class);
    verify(executorService).submit(dispatcher.capture());

    Ambiance childAmbiance = dispatcher.getValue().getAmbiance();

    assertThat(childAmbiance.getLevelsCount()).isEqualTo(2);
    assertThat(childAmbiance.getLevels(1).getSetupId()).isEqualTo(child1Id);

    Node planNode = dispatcher.getValue().getNode();
    assertThat(planNode).isEqualTo(node1);

    ArgumentCaptor<EngineResumeCallback> callbackCaptor = ArgumentCaptor.forClass(EngineResumeCallback.class);
    ArgumentCaptor<String> exIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(waitNotifyEngine).waitForAllOn(any(), callbackCaptor.capture(), exIdCaptor.capture());

    assertThat(callbackCaptor.getValue().getAmbiance()).isEqualTo(ambiance);
    assertThat(childAmbiance.getLevels(1).getRuntimeId()).isEqualTo(exIdCaptor.getValue());

    verify(nodeExecutionService).updateV2(eq(nodeExecutionId), any());
  }
}