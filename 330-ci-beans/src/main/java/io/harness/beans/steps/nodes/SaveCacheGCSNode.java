package io.harness.beans.steps.nodes;

import static io.harness.annotations.dev.HarnessTeam.CI;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIAbstractStepNode;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.SaveCacheGCSStepInfo;
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
@JsonTypeName("SaveCacheGCS")
@TypeAlias("SaveCacheGCSNode")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.steps.nodes.SaveCacheGCSNode")
public class SaveCacheGCSNode extends CIAbstractStepNode {
  @JsonProperty("type") @NotNull SaveCacheGCSNode.StepType type = SaveCacheGCSNode.StepType.SaveCacheGCS;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  SaveCacheGCSStepInfo saveCacheGCSStepInfo;
  @Override
  public String getType() {
    return CIStepInfoType.SAVE_CACHE_GCS.getDisplayName();
  }

  @Override
  public StepSpecType getStepSpecType() {
    return saveCacheGCSStepInfo;
  }

  enum StepType {
    SaveCacheGCS(CIStepInfoType.SAVE_CACHE_GCS.getDisplayName());
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
