/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.execution.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class AmbianceUtilsTest extends CategoryTest {
  private static final String ACCOUNT_ID = generateUuid();
  private static final String ORG_ID = generateUuid();
  private static final String PROJECT_ID = generateUuid();
  private static final String APP_ID = generateUuid();
  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String PLAN_ID = generateUuid();
  private static final String PHASE_RUNTIME_ID = generateUuid();
  private static final String PHASE_SETUP_ID = generateUuid();
  private static final String SECTION_RUNTIME_ID = generateUuid();
  private static final String SECTION_SETUP_ID = generateUuid();
  private static final String STEP_RUNTIME_ID = generateUuid();
  private static final String STEP_SETUP_ID = generateUuid();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void cloneForFinish() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelsList()).hasSize(4);

    Ambiance clonedAmbiance = AmbianceUtils.cloneForFinish(ambiance);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(3);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testCloneForChildAndLevel() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelsList()).hasSize(4);

    Level stepLevel =
        Level.newBuilder()
            .setRuntimeId(STEP_RUNTIME_ID)
            .setSetupId(STEP_SETUP_ID)
            .setStepType(StepType.newBuilder().setType("HTTP_STEP").setStepCategory(StepCategory.STEP).build())
            .build();

    Ambiance clonedAmbiance = AmbianceUtils.cloneForChild(ambiance, stepLevel);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(5);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestClone() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelsList()).hasSize(4);

    Ambiance clonedAmbiance = AmbianceUtils.clone(ambiance, 0);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(0);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);

    clonedAmbiance = AmbianceUtils.clone(ambiance, 1);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(1);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);

    clonedAmbiance = AmbianceUtils.clone(ambiance, 2);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(2);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);

    clonedAmbiance = AmbianceUtils.clone(ambiance, 5);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(4);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestDeepCopy() throws InvalidProtocolBufferException {
    Ambiance ambiance = buildAmbiance();
    Ambiance copy = AmbianceUtils.deepCopy(ambiance);

    assertThat(copy).isNotNull();
    assertThat(System.identityHashCode(copy.getLevelsList()))
        .isNotEqualTo(System.identityHashCode(ambiance.getLevelsList()));
    assertThat(copy.getSetupAbstractionsMap()).isEqualTo(ambiance.getSetupAbstractionsMap());
    assertThat(copy.getLevelsList()).isEqualTo(ambiance.getLevelsList());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAmbiancePropertyGetters() {
    Ambiance ambiance = buildAmbiance();
    assertThat(AmbianceUtils.getAccountId(ambiance)).isEqualTo(ACCOUNT_ID);
    assertThat(AmbianceUtils.getOrgIdentifier(ambiance)).isEqualTo(ORG_ID);
    assertThat(AmbianceUtils.getProjectIdentifier(ambiance)).isEqualTo(PROJECT_ID);

    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    assertThat(ngAccess.getAccountIdentifier()).isEqualTo(ACCOUNT_ID);
    assertThat(ngAccess.getOrgIdentifier()).isEqualTo(ORG_ID);
    assertThat(ngAccess.getProjectIdentifier()).isEqualTo(PROJECT_ID);

    assertThat(AmbianceUtils.obtainCurrentRuntimeId(ambiance)).isEqualTo(SECTION_RUNTIME_ID);
    assertThat(AmbianceUtils.obtainCurrentSetupId(ambiance)).isEqualTo(SECTION_SETUP_ID);
    assertThat(AmbianceUtils.obtainStepIdentifier(ambiance)).isEqualTo("i4");
    assertThat(AmbianceUtils.getCurrentStepType(ambiance))
        .isEqualTo(StepType.newBuilder().setType("SECTION").setStepCategory(StepCategory.STEP).build());
    assertThat(AmbianceUtils.getCurrentGroup(ambiance)).isEqualTo("SECTION");
    assertThat(AmbianceUtils.getCurrentLevelStartTs(ambiance)).isEqualTo(4);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testEmptyAmbiancePropertyGetters() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    assertThat(AmbianceUtils.obtainCurrentRuntimeId(ambiance)).isNull();
    assertThat(AmbianceUtils.obtainCurrentSetupId(ambiance)).isNull();
    assertThat(AmbianceUtils.obtainStepIdentifier(ambiance)).isNull();
    assertThat(AmbianceUtils.getCurrentStepType(ambiance)).isNull();
    assertThat(AmbianceUtils.getCurrentGroup(ambiance)).isNull();
    assertThatThrownBy(() -> AmbianceUtils.getCurrentLevelStartTs(ambiance))
        .isInstanceOf(InvalidRequestException.class);
  }

  private Ambiance buildAmbiance() {
    Level phaseLevel =
        Level.newBuilder()
            .setRuntimeId(PHASE_RUNTIME_ID)
            .setSetupId(PHASE_SETUP_ID)
            .setStartTs(1)
            .setIdentifier("i1")
            .setStepType(StepType.newBuilder().setType("PHASE").setStepCategory(StepCategory.STEP).build())
            .build();
    Level sectionLevel =
        Level.newBuilder()
            .setRuntimeId(SECTION_RUNTIME_ID)
            .setSetupId(SECTION_SETUP_ID)
            .setGroup("SECTION")
            .setStartTs(2)
            .setIdentifier("i2")
            .setStepType(StepType.newBuilder().setType("SECTION").setStepCategory(StepCategory.STAGE).build())
            .build();
    Level stageLevel =
        Level.newBuilder()
            .setRuntimeId(SECTION_RUNTIME_ID)
            .setSetupId(SECTION_SETUP_ID)
            .setGroup("STAGE")
            .setStartTs(3)
            .setIdentifier("i3")
            .setStepType(StepType.newBuilder().setType("SECTION").setStepCategory(StepCategory.STAGE).build())
            .build();
    Level stepLevel =
        Level.newBuilder()
            .setRuntimeId(SECTION_RUNTIME_ID)
            .setSetupId(SECTION_SETUP_ID)
            .setGroup("SECTION")
            .setStartTs(4)
            .setIdentifier("i4")
            .setStepType(StepType.newBuilder().setType("SECTION").setStepCategory(StepCategory.STEP).build())
            .build();
    List<Level> levels = new ArrayList<>();
    levels.add(phaseLevel);
    levels.add(sectionLevel);
    levels.add(stageLevel);
    levels.add(stepLevel);
    return Ambiance.newBuilder()
        .setPlanExecutionId(PLAN_EXECUTION_ID)
        .setPlanId(PLAN_ID)
        .putAllSetupAbstractions(ImmutableMap.of(
            "accountId", ACCOUNT_ID, "orgIdentifier", ORG_ID, "projectIdentifier", PROJECT_ID, "appId", APP_ID))
        .addAllLevels(levels)
        .build();
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetStageLevelFromAmbiance() {
    Ambiance ambiance = buildAmbiance();
    Optional<Level> stage = AmbianceUtils.getStageLevelFromAmbiance(ambiance);
    assertThat(stage.isPresent()).isTrue();
    assertThat(stage.get())
        .isEqualTo(
            Level.newBuilder()
                .setRuntimeId(SECTION_RUNTIME_ID)
                .setSetupId(SECTION_SETUP_ID)
                .setGroup("STAGE")
                .setStartTs(3)
                .setIdentifier("i3")
                .setStepType(StepType.newBuilder().setType("SECTION").setStepCategory(StepCategory.STAGE).build())
                .build());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testObtainParentRuntimeId() {
    Ambiance ambiance = buildAmbiance();
    String parentRuntimeId = AmbianceUtils.obtainParentRuntimeId(ambiance);
    assertThat(parentRuntimeId).isNotNull();
    assertThat(parentRuntimeId).isEqualTo(SECTION_RUNTIME_ID);
  }
}
