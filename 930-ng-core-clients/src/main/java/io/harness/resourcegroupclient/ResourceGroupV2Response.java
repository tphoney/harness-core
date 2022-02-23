package io.harness.resourcegroupclient;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
@Schema(
    name = "ResourceGroupV2Response", description = "This has details of the Resource Group along with its metadata.")
public class ResourceGroupV2Response {
  @NotNull private ResourceGroupV2DTO resourceGroup;
  private Long createdAt;
  private Long lastModifiedAt;
  private boolean harnessManaged;
}
