/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.policy.step;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.network.SafeHttpCall;
import io.harness.opaclient.OpaServiceClient;
import io.harness.opaclient.model.OpaConstants;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.policy.PolicyStepConstants;
import io.harness.steps.policy.PolicyStepSpecParameters;
import io.harness.steps.policy.custom.CustomPolicyStepSpec;
import io.harness.steps.policy.step.outcome.PolicyStepOutcome;
import io.harness.steps.policy.step.outcome.PolicyStepOutcomeMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class PolicyStep implements SyncExecutable<StepElementParameters> {
  public static StepType STEP_TYPE =
      StepType.newBuilder().setType(StepSpecTypeConstants.POLICY_STEP).setStepCategory(StepCategory.STEP).build();
  @Inject OpaServiceClient opaServiceClient;

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    PolicyStepSpecParameters policyStepSpecParameters = (PolicyStepSpecParameters) stepParameters.getSpec();
    ParameterField<List<String>> policySetsPF = policyStepSpecParameters.getPolicySets();
    if (policySetsPF.isExpression()) {
      return PolicyStepHelper.buildResponseForUnresolvedExpressions(
          "List of policy sets is an unresolved expression: " + policySetsPF.getExpressionValue());
    }
    List<String> policySets = policySetsPF.getValue();

    String payload;
    String policyStepType = policyStepSpecParameters.getType();
    switch (policyStepType) {
      case PolicyStepConstants.CUSTOM_POLICY_STEP_TYPE:
        CustomPolicyStepSpec customPolicySpec = (CustomPolicyStepSpec) policyStepSpecParameters.getPolicySpec();
        ParameterField<String> payloadPF = customPolicySpec.getPayload();
        if (payloadPF.isExpression()) {
          return PolicyStepHelper.buildResponseForUnresolvedExpressions(
              "Custom payload has unresolved expressions in it.");
        }
        payload = payloadPF.getValue();
        try {
          YamlUtils.readTree(payload);
        } catch (IOException e) {
          log.error("Custom payload after expression resolution is not a valid JSON:\n" + payload);
          FailureData failureData = FailureData.newBuilder()
                                        .setCode(ErrorCode.INVALID_JSON_PAYLOAD.name())
                                        .setLevel(Level.ERROR.name())
                                        .setMessage("Custom payload after expression resolution is not a valid JSON.")
                                        .addFailureTypes(FailureType.UNKNOWN_FAILURE)
                                        .build();
          FailureInfo failureInfo = FailureInfo.newBuilder().addFailureData(failureData).build();
          return StepResponse.builder().status(Status.FAILED).failureInfo(failureInfo).build();
        }
        break;
      default:
        return PolicyStepHelper.buildUnknownFailureStepResponse(
            ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION, "Policy Step type " + policyStepType + " is not supported.");
    }

    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    OpaEvaluationResponseHolder opaEvaluationResponseHolder;
    try {
      opaEvaluationResponseHolder = SafeHttpCall.executeWithExceptions(opaServiceClient.evaluateWithCredentialsByID(
          accountId, orgIdentifier, projectIdentifier, policySets, payload));
    } catch (Exception ex) {
      log.error("Exception while evaluating OPA rules", ex);
      return PolicyStepHelper.buildFailureStepResponse(ErrorCode.HTTP_RESPONSE_EXCEPTION,
          "Error occurred while evaluating Policies", FailureType.APPLICATION_FAILURE, null);
    }
    PolicyStepOutcome policyStepOutcome = PolicyStepOutcomeMapper.toOutcome(opaEvaluationResponseHolder);
    StepOutcome stepOutcome = StepOutcome.builder()
                                  .group(StepOutcomeGroup.STEP.name())
                                  .name(YAMLFieldNameConstants.OUTPUT)
                                  .outcome(policyStepOutcome)
                                  .build();
    if (opaEvaluationResponseHolder.getStatus().equals(OpaConstants.OPA_STATUS_ERROR)) {
      return PolicyStepHelper.buildFailureStepResponse(ErrorCode.POLICY_EVALUATION_FAILURE,
          "Some Policies were not adhered to", FailureType.POLICY_EVALUATION_FAILURE, stepOutcome);
    }
    return StepResponse.builder().status(Status.SUCCEEDED).stepOutcome(stepOutcome).build();
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
}
