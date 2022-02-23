package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.beans.PageRequest;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2DTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2FilterDTO;
import io.harness.resourcegroupclient.ResourceGroupV2Response;

import java.util.Optional;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
public interface ResourceGroupV2Service {
  ResourceGroupV2Response create(ResourceGroupV2DTO resourceGroupDTO, boolean harnessManaged);

  Page<ResourceGroupV2Response> list(ResourceGroupV2FilterDTO resourceGroupFilterDTO, PageRequest pageRequest);

  Page<ResourceGroupV2Response> list(Scope scope, PageRequest pageRequest, String searchTerm);

  Optional<ResourceGroupV2Response> update(ResourceGroupV2DTO resourceGroupDTO, boolean harnessManaged);

  boolean delete(Scope scope, String identifier);

  void deleteManaged(@NotEmpty String identifier);

  void deleteByScope(Scope scope);
}
