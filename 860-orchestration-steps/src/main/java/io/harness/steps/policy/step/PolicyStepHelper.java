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
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import java.io.IOException;
import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class PolicyStepHelper {
  public String getPolicySetsStringForQueryParam(List<String> policySets) {
    return policySets.toString().replace("[", "").replace("]", "").replace(" ", "");
  }

  public boolean isInvalidPayload(String payload) {
    try {
      YamlField yamlField = YamlUtils.readTree(payload);
      // Policy manager does not support primitive values like strings or numbers. Arrays are also not supported
      return !yamlField.getNode().isObject();
    } catch (IOException e) {
      return true;
    }
  }

  public StepResponse buildFailureStepResponse(ErrorCode errorCode, String message, FailureType failureType) {
    FailureData failureData = FailureData.newBuilder()
                                  .setCode(errorCode.name())
                                  .setLevel(Level.ERROR.name())
                                  .setMessage(message)
                                  .addFailureTypes(failureType)
                                  .build();
    FailureInfo failureInfo = FailureInfo.newBuilder().addFailureData(failureData).build();
    return StepResponse.builder().status(Status.FAILED).failureInfo(failureInfo).build();
  }
}
