/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.creation;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.SourceType;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.plan.creation.validator.PlanCreationValidator;
import io.harness.pms.sdk.PmsSdkHelper;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class PlanCreatorMergeServiceTest extends CategoryTest {
  @Mock PmsSdkHelper pmsSdkHelper;
  @Mock
  PlanCreationValidator planCreationValidator;
  @InjectMocks PlanCreatorMergeService planCreatorMergeService;

  String accountId = "acc";
  String orgId = "org";
  String projId = "proj";

  private String getYamlFieldFromGivenFileName(String file) throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream yamlFile = classLoader.getResourceAsStream(file);
    assertThat(yamlFile).isNotNull();

    String yaml = new Scanner(yamlFile, "UTF-8").useDelimiter("\\A").next();
    yaml = YamlUtils.injectUuid(yaml);
    return yaml;
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreateInitialPlanCreationContext() {
    ExecutionMetadata executionMetadata = ExecutionMetadata.newBuilder()
                                              .setExecutionUuid("execId")
                                              .setRunSequence(3)
                                              .setModuleType("cd")
                                              .setPipelineIdentifier("pipelineId")
                                              .build();
    PlanCreatorMergeService planCreatorMergeService =
        new PlanCreatorMergeService(null, null, null, null, Executors.newSingleThreadExecutor());
    Map<String, PlanCreationContextValue> initialPlanCreationContext =
        planCreatorMergeService.createInitialPlanCreationContext(accountId, orgId, projId, executionMetadata, null);
    assertThat(initialPlanCreationContext).hasSize(1);
    assertThat(initialPlanCreationContext.containsKey("metadata")).isTrue();
    PlanCreationContextValue planCreationContextValue = initialPlanCreationContext.get("metadata");
    assertThat(planCreationContextValue.getAccountIdentifier()).isEqualTo(accountId);
    assertThat(planCreationContextValue.getOrgIdentifier()).isEqualTo(orgId);
    assertThat(planCreationContextValue.getProjectIdentifier()).isEqualTo(projId);
    assertThat(planCreationContextValue.getMetadata()).isEqualTo(executionMetadata);
    assertThat(planCreationContextValue.getTriggerPayload()).isEqualTo(TriggerPayload.newBuilder().build());

    TriggerPayload triggerPayload = TriggerPayload.newBuilder()
                                        .setParsedPayload(ParsedPayload.newBuilder().build())
                                        .setSourceType(SourceType.GITHUB_REPO)
                                        .build();
    initialPlanCreationContext = planCreatorMergeService.createInitialPlanCreationContext(
        accountId, orgId, projId, executionMetadata, triggerPayload);
    assertThat(initialPlanCreationContext).hasSize(1);
    assertThat(initialPlanCreationContext.containsKey("metadata")).isTrue();
    planCreationContextValue = initialPlanCreationContext.get("metadata");
    assertThat(planCreationContextValue.getAccountIdentifier()).isEqualTo(accountId);
    assertThat(planCreationContextValue.getOrgIdentifier()).isEqualTo(orgId);
    assertThat(planCreationContextValue.getProjectIdentifier()).isEqualTo(projId);
    assertThat(planCreationContextValue.getMetadata()).isEqualTo(executionMetadata);
    assertThat(planCreationContextValue.getTriggerPayload()).isEqualTo(triggerPayload);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testCreatePlan() throws IOException {
    ExecutionMetadata executionMetadata = ExecutionMetadata.newBuilder()
            .setExecutionUuid("execId")
            .setRunSequence(3)
            .setModuleType("cd")
            .setPipelineIdentifier("pipelineId")
            .build();

    TriggerPayload triggerPayload = TriggerPayload.newBuilder()
            .setParsedPayload(ParsedPayload.newBuilder().build())
            .setSourceType(SourceType.GITHUB_REPO)
            .build();
    PlanExecutionMetadata planExecutionMetadata = PlanExecutionMetadata.builder().processedYaml(getYamlFieldFromGivenFileName("pipeline.yml")).triggerPayload(triggerPayload).build();
    planCreatorMergeService.createPlan(accountId,orgId,projId,executionMetadata,planExecutionMetadata);
    verify(planCreationValidator).validate(any(),any());
  }
}
