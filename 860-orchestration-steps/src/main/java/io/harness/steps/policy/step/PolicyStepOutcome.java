package io.harness.steps.policy.step;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;
import io.harness.pms.sdk.core.data.Outcome;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class PolicyStepOutcome implements Outcome {
  OpaEvaluationResponseHolder policyEvaluationResponse;
}
