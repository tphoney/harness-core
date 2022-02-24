/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.cdng.provision.terraform.steps.rolllback.TerraformRollbackStepInfo;
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
@JsonTypeName(StepSpecTypeConstants.TERRAFORM_ROLLBACK)
@TypeAlias("TerraformRollbackStepNode")
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.provision.terraform.TerraformRollbackStepNode")
public class TerraformRollbackStepNode extends CdAbstractStepNode {
  @JsonProperty("type") @NotNull StepType type = StepType.TerraformRollback;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  TerraformRollbackStepInfo terraformRollbackStepInfo;
  @Override
  public String getType() {
    return StepSpecTypeConstants.TERRAFORM_ROLLBACK;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return terraformRollbackStepInfo;
  }

  enum StepType {
    TerraformRollback(StepSpecTypeConstants.TERRAFORM_ROLLBACK);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
