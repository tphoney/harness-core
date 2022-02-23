/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.K8S_CANARY_DELETE)
@TypeAlias("K8sCanaryDeleteStepNode")
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.k8s.K8sCanaryDeleteStepNode")
public class K8sCanaryDeleteStepNode extends CdAbstractStepNode {
  @JsonProperty("type") @NotNull StepType type = StepType.K8sCanaryDelete;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  K8sCanaryDeleteStepInfo k8sCanaryDeleteStepInfo;
  @Override
  public String getType() {
    return StepSpecTypeConstants.K8S_CANARY_DELETE;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return k8sCanaryDeleteStepInfo;
  }

  enum StepType {
    K8sCanaryDelete(StepSpecTypeConstants.K8S_CANARY_DELETE);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
