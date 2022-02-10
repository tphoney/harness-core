package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.RestoreCacheS3Node;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class RestoreCacheS3StepPlanCreator extends CIPMSStepPlanCreatorV2<RestoreCacheS3Node> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.RESTORE_CACHE_S3.getDisplayName());
  }

  @Override
  public Class<RestoreCacheS3Node> getFieldClass() {
    return RestoreCacheS3Node.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, RestoreCacheS3Node stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
