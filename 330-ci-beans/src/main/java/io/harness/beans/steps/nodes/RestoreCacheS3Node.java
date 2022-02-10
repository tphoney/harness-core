package io.harness.beans.steps.nodes;

import static io.harness.annotations.dev.HarnessTeam.CI;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIAbstractStepNode;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.RestoreCacheS3StepInfo;
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
@JsonTypeName("RestoreCacheS3")
@TypeAlias("RestoreCacheS3Node")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.steps.nodes.RestoreCacheS3Node")
public class RestoreCacheS3Node extends CIAbstractStepNode {
  @JsonProperty("type") @NotNull RestoreCacheS3Node.StepType type = RestoreCacheS3Node.StepType.RestoreCacheS3;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  RestoreCacheS3StepInfo restoreCacheS3StepInfo;
  @Override
  public String getType() {
    return CIStepInfoType.RESTORE_CACHE_S3.getDisplayName();
  }

  @Override
  public StepSpecType getStepSpecType() {
    return restoreCacheS3StepInfo;
  }

  enum StepType {
    RestoreCacheS3(CIStepInfoType.RESTORE_CACHE_S3.getDisplayName());
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
