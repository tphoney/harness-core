package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.RestoreCacheGCSNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class RestoreCacheGCSStepPlanCreator extends CIPMSStepPlanCreatorV2<RestoreCacheGCSNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.RESTORE_CACHE_GCS.getDisplayName());
  }

  @Override
  public Class<RestoreCacheGCSNode> getFieldClass() {
    return RestoreCacheGCSNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, RestoreCacheGCSNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
