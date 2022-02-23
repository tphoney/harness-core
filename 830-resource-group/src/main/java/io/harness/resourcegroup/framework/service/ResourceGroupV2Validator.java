package io.harness.resourcegroup.framework.service;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2Request;

@OwnedBy(PL)
public interface ResourceGroupV2Validator {
  void validateResourceGroup(ResourceGroupV2Request resourceGroupRequest);
}
