package io.harness.steps.policy.step.outcome;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class PolicyOutcome {
  String identifier;
  String name;
  String status;
  List<String> denyMessages;
  String error;
}