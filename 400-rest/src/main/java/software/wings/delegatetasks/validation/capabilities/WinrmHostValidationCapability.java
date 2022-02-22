/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.validation.capabilities;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.winrm.AuthenticationScheme;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.WinRmConnectionAttributes;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class WinrmHostValidationCapability implements ExecutionCapability {
  @NotNull BasicValidationInfo validationInfo;
  @NotNull private WinRmConnectionAttributes winRmConnectionAttributes;
  @NotNull private List<EncryptedDataDetail> winrmConnectionEncryptedDataDetails;
  private Map<String, String> envVariables = new HashMap<>();

  @Builder.Default private final CapabilityType capabilityType = CapabilityType.WINRM_HOST_CONNECTION;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    if (validationInfo.isExecuteOnDelegate()) {
      return "localhost";
    }
    final StringBuilder basisBuilder = new StringBuilder().append(validationInfo.getPublicDns());
    if (AuthenticationScheme.KERBEROS == winRmConnectionAttributes.getAuthenticationScheme()) {
      return basisBuilder.append(":kerberos").toString();
    }
    return basisBuilder.toString();
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
