package io.harness.steps.policy.step;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.internal.PMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.policy.PolicyStepNode;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(PIPELINE)
public class PolicyStepPlanCreator extends PMSStepPlanCreatorV2<PolicyStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.POLICY_STEP);
  }

  @Override
  public Class<PolicyStepNode> getFieldClass() {
    return PolicyStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, PolicyStepNode field) {
    return super.createPlanForField(ctx, field);
  }
}
