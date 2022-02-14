package io.harness.steps.policy.step;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.sdk.core.steps.io.StepResponse;

import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class PolicyStepHelper {
  public StepResponse buildResponseForUnresolvedExpressions(String message) {
    return buildUnknownFailureStepResponse(ErrorCode.UNRESOLVED_EXPRESSIONS_ERROR, message);
  }

  public StepResponse buildUnknownFailureStepResponse(ErrorCode errorCode, String message) {
    return buildFailureStepResponse(errorCode, message, FailureType.UNKNOWN_FAILURE, null);
  }

  public StepResponse buildFailureStepResponse(
      ErrorCode errorCode, String message, FailureType failureType, StepResponse.StepOutcome stepOutcome) {
    FailureData failureData = FailureData.newBuilder()
                                  .setCode(errorCode.name())
                                  .setLevel(Level.ERROR.name())
                                  .setMessage(message)
                                  .addFailureTypes(failureType)
                                  .build();
    FailureInfo failureInfo = FailureInfo.newBuilder().addFailureData(failureData).build();
    return StepResponse.builder().status(Status.FAILED).failureInfo(failureInfo).stepOutcome(stepOutcome).build();
  }
}
