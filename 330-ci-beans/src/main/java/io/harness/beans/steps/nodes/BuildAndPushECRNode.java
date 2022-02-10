package io.harness.beans.steps.nodes;

import static io.harness.annotations.dev.HarnessTeam.CI;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIAbstractStepNode;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.ECRStepInfo;
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
@JsonTypeName("BuildAndPushECR")
@TypeAlias("BuildAndPushECRNode")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.steps.nodes.BuildAndPushECRNode")

public class BuildAndPushECRNode extends CIAbstractStepNode {
  @JsonProperty("type") @NotNull BuildAndPushECRNode.StepType type = BuildAndPushECRNode.StepType.BuildAndPushECR;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ECRStepInfo ecrStepInfo;
  @Override
  public String getType() {
    return CIStepInfoType.ECR.getDisplayName();
  }

  @Override
  public StepSpecType getStepSpecType() {
    return ecrStepInfo;
  }

  enum StepType {
    BuildAndPushECR(CIStepInfoType.ECR.getDisplayName());
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
