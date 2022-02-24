package io.harness.steps.policy.step.outcome;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
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
    // todo(@NamanVerma): Change field names in OpaEvaluationResponseHolder to follow Java naming conventions
    List<OpaPolicySetEvaluationResponse> policySetsResponse = evaluationResponse.getDetails();
    Map<String, PolicySetOutcome> accountPolicySetsOutcome = getAccountPolicySetsOutcome(policySetsResponse);
    Map<String, PolicySetOutcome> orgPolicySetsOutcome = getOrgPolicySetsOutcome(policySetsResponse);
    Map<String, PolicySetOutcome> projectPolicySetsOutcome = getProjectPolicySetsOutcome(policySetsResponse);
    return PolicyStepOutcome.builder()
        .status(evaluationResponse.getStatus())
        .accountPolicySetDetails(accountPolicySetsOutcome)
        .orgPolicySetDetails(orgPolicySetsOutcome)
        .projectPolicySetDetails(projectPolicySetsOutcome)
        .build();
  }

  Map<String, PolicySetOutcome> getAccountPolicySetsOutcome(List<OpaPolicySetEvaluationResponse> fullResponse) {
    List<OpaPolicySetEvaluationResponse> accountResponse =
        fullResponse.stream()
            .filter(response -> EmptyPredicate.isEmpty(response.getOrg_id()))
            .collect(Collectors.toList());
    return accountResponse.stream()
        .map(PolicyStepOutcomeMapper::toPolicySetOutcome)
        .collect(Collectors.toMap(PolicySetOutcome::getIdentifier, Function.identity()));
  }

  Map<String, PolicySetOutcome> getOrgPolicySetsOutcome(List<OpaPolicySetEvaluationResponse> fullResponse) {
    List<OpaPolicySetEvaluationResponse> orgResponse =
        fullResponse.stream()
            .filter(response
                -> EmptyPredicate.isNotEmpty(response.getOrg_id()) && EmptyPredicate.isEmpty(response.getProject_id()))
            .collect(Collectors.toList());
    return orgResponse.stream()
        .map(PolicyStepOutcomeMapper::toPolicySetOutcome)
        .collect(Collectors.toMap(PolicySetOutcome::getIdentifier, Function.identity()));
  }

  Map<String, PolicySetOutcome> getProjectPolicySetsOutcome(List<OpaPolicySetEvaluationResponse> fullResponse) {
    List<OpaPolicySetEvaluationResponse> projectResponse =
        fullResponse.stream()
            .filter(response -> EmptyPredicate.isNotEmpty(response.getProject_id()))
            .collect(Collectors.toList());
    return projectResponse.stream()
        .map(PolicyStepOutcomeMapper::toPolicySetOutcome)
        .collect(Collectors.toMap(PolicySetOutcome::getIdentifier, Function.identity()));
  }

  PolicySetOutcome toPolicySetOutcome(OpaPolicySetEvaluationResponse policySetResponse) {
    List<OpaPolicyEvaluationResponse> policyDetails = policySetResponse.getDetails();
    Map<String, PolicyOutcome> accountPoliciesOutcome = getAccountPoliciesOutcome(policyDetails);
    Map<String, PolicyOutcome> orgPoliciesOutcome = getOrgPoliciesOutcome(policyDetails);
    Map<String, PolicyOutcome> projectPoliciesOutcome = getProjectPoliciesOutcome(policyDetails);
    return PolicySetOutcome.builder()
        .status(policySetResponse.getStatus())
        .identifier(policySetResponse.getIdentifier())
        .name(policySetResponse.getName())
        .accountPolicyDetails(accountPoliciesOutcome)
        .orgPolicyDetails(orgPoliciesOutcome)
        .projectPolicyDetails(projectPoliciesOutcome)
        .build();
  }

  Map<String, PolicyOutcome> getAccountPoliciesOutcome(List<OpaPolicyEvaluationResponse> policiesResponse) {
    List<OpaPolicyEvaluationResponse> accountPolicies =
        policiesResponse.stream()
            .filter(response -> EmptyPredicate.isEmpty(response.getPolicy().getOrg_id()))
            .collect(Collectors.toList());
    return accountPolicies.stream()
        .map(PolicyStepOutcomeMapper::toPolicyOutcome)
        .collect(Collectors.toMap(PolicyOutcome::getIdentifier, Function.identity()));
  }

  Map<String, PolicyOutcome> getOrgPoliciesOutcome(List<OpaPolicyEvaluationResponse> policiesResponse) {
    List<OpaPolicyEvaluationResponse> orgPolicies =
        policiesResponse.stream()
            .filter(response
                -> EmptyPredicate.isNotEmpty(response.getPolicy().getOrg_id())
                    && EmptyPredicate.isEmpty(response.getPolicy().getProject_id()))
            .collect(Collectors.toList());
    return orgPolicies.stream()
        .map(PolicyStepOutcomeMapper::toPolicyOutcome)
        .collect(Collectors.toMap(PolicyOutcome::getIdentifier, Function.identity()));
  }

  Map<String, PolicyOutcome> getProjectPoliciesOutcome(List<OpaPolicyEvaluationResponse> policiesResponse) {
    List<OpaPolicyEvaluationResponse> projectPolicies =
        policiesResponse.stream()
            .filter(response -> EmptyPredicate.isNotEmpty(response.getPolicy().getProject_id()))
            .collect(Collectors.toList());
    return projectPolicies.stream()
        .map(PolicyStepOutcomeMapper::toPolicyOutcome)
        .collect(Collectors.toMap(PolicyOutcome::getIdentifier, Function.identity()));
  }

  PolicyOutcome toPolicyOutcome(OpaPolicyEvaluationResponse policyResponse) {
    return PolicyOutcome.builder()
        .status(policyResponse.getStatus())
        .identifier(policyResponse.getPolicy().getIdentifier())
        .name(policyResponse.getPolicy().getName())
        .denyMessages(policyResponse.getDeny_messages())
        .error(policyResponse.getError())
        .build();
  }
}
