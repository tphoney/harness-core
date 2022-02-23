package io.harness.resourcegroup.remote.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Schema(name = "ResourceGroupV2Filter", description = "Contains information of filters for Resource Group")
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceGroupV2FilterDTO {
  @Schema(description = "Filter by account identifier", required = true)
  @ApiModelProperty(required = true)
  String accountIdentifier;
  @Schema(description = "Filter by organization identifier") String orgIdentifier;
  @Schema(description = "Filter by project identifier") String projectIdentifier;
  @Schema(description = "Filter resource group matching by identifier/name") String searchTerm;
  @Schema(description = "Filter by resource group identifiers") Set<String> identifierFilter;
  @Schema(description = "Filter based on whether the resource group is Harness managed") ManagedFilter managedFilter;
}
