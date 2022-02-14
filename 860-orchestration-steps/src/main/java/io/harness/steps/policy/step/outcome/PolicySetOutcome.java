package io.harness.steps.policy.step.outcome;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class PolicySetOutcome {
  String status;
  String identifier;
  String name;
  Map<String, PolicyOutcome> policyDetails;
}
