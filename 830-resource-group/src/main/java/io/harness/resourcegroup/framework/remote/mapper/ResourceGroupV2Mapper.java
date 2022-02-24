package io.harness.resourcegroup.framework.remote.mapper;

import static io.harness.NGConstants.HARNESS_BLUE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.model.ResourceGroupV2;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2DTO;
import io.harness.resourcegroupclient.ResourceGroupV2Response;

import java.util.ArrayList;
import java.util.HashSet;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class ResourceGroupV2Mapper {
  public static ResourceGroupV2 fromDTO(ResourceGroupV2DTO resourceGroupDTO) {
    if (resourceGroupDTO == null) {
      return null;
    }
    ResourceGroupV2 resourceGroupV2 =
        ResourceGroupV2.builder()
            .accountIdentifier(resourceGroupDTO.getAccountIdentifier())
            .orgIdentifier(resourceGroupDTO.getOrgIdentifier())
            .projectIdentifier(resourceGroupDTO.getProjectIdentifier())
            .identifier(resourceGroupDTO.getIdentifier())
            .name(resourceGroupDTO.getName())
            .color(isBlank(resourceGroupDTO.getColor()) ? HARNESS_BLUE : resourceGroupDTO.getColor())
            .tags(convertToList(resourceGroupDTO.getTags()))
            .description(resourceGroupDTO.getDescription())
            .allowedScopeLevels(resourceGroupDTO.getAllowedScopeLevels() == null
                    ? new HashSet<>()
                    : resourceGroupDTO.getAllowedScopeLevels())
            .includedScopes(
                resourceGroupDTO.getIncludedScopes() == null ? new ArrayList<>() : resourceGroupDTO.getIncludedScopes())
            .build();

    if (resourceGroupDTO.getResourceFilter() != null) {
      resourceGroupV2.setResourceFilter(resourceGroupDTO.getResourceFilter());
    }

    return resourceGroupV2;
  }

  public static ResourceGroupV2DTO toDTO(ResourceGroupV2 resourceGroup) {
    if (resourceGroup == null) {
      return null;
    }
    ResourceGroupV2DTO dto = ResourceGroupV2DTO.builder()
                                 .accountIdentifier(resourceGroup.getAccountIdentifier())
                                 .orgIdentifier(resourceGroup.getOrgIdentifier())
                                 .projectIdentifier(resourceGroup.getProjectIdentifier())
                                 .identifier(resourceGroup.getIdentifier())
                                 .name(resourceGroup.getName())
                                 .color(resourceGroup.getColor())
                                 .tags(convertToMap(resourceGroup.getTags()))
                                 .description(resourceGroup.getDescription())
                                 .includedScopes(resourceGroup.getIncludedScopes())
                                 .resourceFilter(resourceGroup.getResourceFilter())
                                 .build();

    return dto;
  }

  public static ResourceGroupV2Response toResponseWrapper(ResourceGroupV2 resourceGroup) {
    if (resourceGroup == null) {
      return null;
    }
    return ResourceGroupV2Response.builder()
        .createdAt(resourceGroup.getCreatedAt())
        .lastModifiedAt(resourceGroup.getLastModifiedAt())
        .resourceGroup(toDTO(resourceGroup))
        .harnessManaged(Boolean.TRUE.equals(resourceGroup.getHarnessManaged()))
        .build();
  }
}
