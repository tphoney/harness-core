package io.harness.beans.steps.nodes;

import static io.harness.annotations.dev.HarnessTeam.CI;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIAbstractStepNode;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
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
@JsonTypeName("Plugin")
@TypeAlias("PluginStepNode")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.steps.nodes.PluginStepNode")
public class PluginStepNode extends CIAbstractStepNode {
  @JsonProperty("type") @NotNull PluginStepNode.StepType type = PluginStepNode.StepType.Plugin;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  PluginStepInfo pluginStepInfo;
  @Override
  public String getType() {
    return CIStepInfoType.PLUGIN.getDisplayName();
  }

  @Override
  public StepSpecType getStepSpecType() {
    return pluginStepInfo;
  }

  enum StepType {
    Plugin(CIStepInfoType.PLUGIN.getDisplayName());
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
