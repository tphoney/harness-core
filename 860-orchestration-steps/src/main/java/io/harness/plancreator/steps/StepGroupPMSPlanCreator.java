/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.PARALLEL;
import static io.harness.pms.yaml.YAMLFieldNameConstants.ROLLBACK_STEPS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEPS;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP_GROUP;

import io.harness.advisers.nextstep.NextStepAdviserParameters;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.execution.utils.SkipInfoUtils;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.adviser.success.OnSuccessAdviserParameters;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.common.steps.stepgroup.StepGroupStep;
import io.harness.steps.common.steps.stepgroup.StepGroupStepParameters;
import io.harness.when.utils.RunInfoUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(PIPELINE)
public class StepGroupPMSPlanCreator extends ChildrenPlanCreator<StepGroupElementConfig> {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, StepGroupElementConfig config) {
    List<YamlField> dependencyNodeIdsList = ctx.getStepYamlFields();

    LinkedHashMap<String, PlanCreationResponse> responseMap = new LinkedHashMap<>();
    // Add Steps Node
    if (EmptyPredicate.isNotEmpty(dependencyNodeIdsList)) {
      YamlField stepsField =
          Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STEPS));
      String stepsNodeId = stepsField.getNode().getUuid();
      Map<String, YamlField> stepsYamlFieldMap = new HashMap<>();
      stepsYamlFieldMap.put(stepsNodeId, stepsField);
      responseMap.put(stepsNodeId,
          PlanCreationResponse.builder()
              .dependencies(DependenciesUtils.toDependenciesProto(stepsYamlFieldMap))
              .build());
    }

    return responseMap;
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, StepGroupElementConfig config, List<String> childrenNodeIds) {
    YamlField stepsField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STEPS));
    StepParameters stepParameters = StepGroupStepParameters.getStepParameters(config, stepsField.getNode().getUuid());

    boolean isStepGroupInsideRollback = false;
    if (YamlUtils.findParentNode(ctx.getCurrentField().getNode(), ROLLBACK_STEPS) != null) {
      isStepGroupInsideRollback = true;
    }

    return PlanNode.builder()
        .name(config.getName())
        .uuid(config.getUuid())
        .identifier(config.getIdentifier())
        .stepType(StepGroupStep.STEP_TYPE)
        .group(StepOutcomeGroup.STEP_GROUP.name())
        .skipCondition(SkipInfoUtils.getSkipCondition(config.getSkipCondition()))
        // We Should add default when condition as StageFailure if stepGroup is inside rollback
        .whenCondition(isStepGroupInsideRollback ? RunInfoUtils.getRunConditionForRollback(config.getWhen())
                                                 : RunInfoUtils.getRunCondition(config.getWhen()))
        .stepParameters(stepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
        .skipExpressionChain(false)
        .build();
  }

  @Override
  public Class<StepGroupElementConfig> getFieldClass() {
    return StepGroupElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(STEP_GROUP, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField currentField) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();

    /*
     * Adding OnSuccess adviser if stepGroup is inside rollback section else adding NextStep adviser for when condition
     * to work.
     */
    if (currentField != null && currentField.getNode() != null) {
      // Check if step is inside RollbackStep
      if (YamlUtils.findParentNode(currentField.getNode(), ROLLBACK_STEPS) != null) {
        addOnSuccessAdviser(currentField, adviserObtainments);
      } else {
        // Adding NextStep Adviser at last due to giving priority to Failure strategy more. DO NOT CHANGE.
        addNextStepAdviser(currentField, adviserObtainments);
      }
    }
    return adviserObtainments;
  }

  private void addNextStepAdviser(YamlField currentField, List<AdviserObtainment> adviserObtainments) {
    if (currentField.checkIfParentIsParallel(STEPS)) {
      return;
    }
    YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
        currentField.getName(), Arrays.asList(YAMLFieldNameConstants.STEP, PARALLEL, STEP_GROUP));
    if (siblingField != null && siblingField.getNode().getUuid() != null) {
      adviserObtainments.add(
          AdviserObtainment.newBuilder()
              .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.NEXT_STEP.name()).build())
              .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                  NextStepAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
              .build());
    }
  }

  private void addOnSuccessAdviser(YamlField currentField, List<AdviserObtainment> adviserObtainments) {
    if (currentField.checkIfParentIsParallel(ROLLBACK_STEPS)) {
      return;
    }
    YamlField siblingField = currentField.getNode().nextSiblingFromParentArray(
        currentField.getName(), Arrays.asList(YAMLFieldNameConstants.STEP, PARALLEL, STEP_GROUP));
    if (siblingField != null && siblingField.getNode().getUuid() != null) {
      adviserObtainments.add(
          AdviserObtainment.newBuilder()
              .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
              .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                  OnSuccessAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
              .build());
    }
  }
}
