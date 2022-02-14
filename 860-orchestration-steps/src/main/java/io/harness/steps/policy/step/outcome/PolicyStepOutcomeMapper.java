package io.harness.steps.policy.step.outcome;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;
import io.harness.opaclient.model.OpaPolicyEvaluationResponse;
import io.harness.opaclient.model.OpaPolicySetEvaluationResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class PolicyStepOutcomeMapper {
  public PolicyStepOutcome toOutcome(OpaEvaluationResponseHolder evaluationResponse) {
    List<OpaPolicySetEvaluationResponse> policySetsResponse = evaluationResponse.getDetails();
    List<PolicySetOutcome> policySetOutcomes =
        policySetsResponse.stream().map(PolicyStepOutcomeMapper::toPolicySetOutcome).collect(Collectors.toList());
    Map<String, PolicySetOutcome> policySetDetails =
        policySetOutcomes.stream().collect(Collectors.toMap(PolicySetOutcome::getIdentifier, Function.identity()));
    return PolicyStepOutcome.builder()
        .status(evaluationResponse.getStatus())
        .policySetDetails(policySetDetails)
        .build();
  }

  private PolicySetOutcome toPolicySetOutcome(OpaPolicySetEvaluationResponse policySetDetails) {
    List<OpaPolicyEvaluationResponse> policyDetails = policySetDetails.getDetails();
    List<PolicyOutcome> policyOutcomes =
        policyDetails.stream().map(PolicyStepOutcomeMapper::toPolicyOutcome).collect(Collectors.toList());
    return PolicySetOutcome.builder()
        .status(policySetDetails.getStatus())
        .identifier(policySetDetails.getIdentifier())
        .name(policySetDetails.getName())
        .policyDetails(policyOutcomes)
        .build();
  }

  private PolicyOutcome toPolicyOutcome(OpaPolicyEvaluationResponse opaPolicyEvaluationResponse) {
    return PolicyOutcome.builder()
        .identifier(opaPolicyEvaluationResponse.getPolicy().getIdentifier())
        .name(opaPolicyEvaluationResponse.getPolicy().getName())
        .status(opaPolicyEvaluationResponse.getStatus())
        .denyMessages(opaPolicyEvaluationResponse.getDeny_messages())
        .error(opaPolicyEvaluationResponse.getError())
        .build();
  }
}
