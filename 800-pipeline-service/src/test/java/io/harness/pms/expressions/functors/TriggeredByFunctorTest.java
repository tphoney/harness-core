/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.expressions.functors;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class TriggeredByFunctorTest extends CategoryTest {
  @Mock private PMSExecutionService pmsExecutionService;
  @InjectMocks private TriggeredByFunctor triggeredByFunctor;
  Ambiance ambiance = Ambiance.newBuilder()
                          .putSetupAbstractions("accountId", "accountId")
                          .putSetupAbstractions("projectIdentifier", "projectId")
                          .putSetupAbstractions("orgIdentifier", "orgIdentifier")
                          .build();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testBind() {
    on(triggeredByFunctor).set("ambiance", ambiance);
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity =
        PipelineExecutionSummaryEntity.builder()
            .executionTriggerInfo(ExecutionTriggerInfo.newBuilder()
                                      .setTriggerType(TriggerType.WEBHOOK)
                                      .setTriggeredBy(TriggeredBy.newBuilder().setIdentifier("system").build())
                                      .build())
            .build();
    doReturn(pipelineExecutionSummaryEntity)
        .when(pmsExecutionService)
        .getPipelineExecutionSummaryEntity(any(), any(), any(), any());

    Map<String, Object> response = (Map<String, Object>) triggeredByFunctor.bind();
    assertEquals(response.get("triggerType"), TriggerType.WEBHOOK.toString());
    Map<String, String> triggeredByMap = (Map<String, String>) response.get("triggeredBy");
    assertNull(triggeredByMap.get("email"));
    assertEquals(triggeredByMap.get("name"), "system");

    pipelineExecutionSummaryEntity =
        PipelineExecutionSummaryEntity.builder()
            .executionTriggerInfo(ExecutionTriggerInfo.newBuilder()
                                      .setTriggerType(TriggerType.MANUAL)
                                      .setTriggeredBy(TriggeredBy.newBuilder()
                                                          .setIdentifier("Admin")
                                                          .putExtraInfo("email", "admin@harness.io")
                                                          .build())
                                      .build())
            .build();

    doReturn(pipelineExecutionSummaryEntity)
        .when(pmsExecutionService)
        .getPipelineExecutionSummaryEntity(any(), any(), any(), any());

    response = (Map<String, Object>) triggeredByFunctor.bind();
    assertEquals(response.get("triggerType"), TriggerType.MANUAL.toString());
    triggeredByMap = (Map<String, String>) response.get("triggeredBy");
    assertEquals(triggeredByMap.get("email"), "admin@harness.io");
    assertEquals(triggeredByMap.get("name"), "Admin");
  }
}
