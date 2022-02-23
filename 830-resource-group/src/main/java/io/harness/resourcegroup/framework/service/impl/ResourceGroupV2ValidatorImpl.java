package io.harness.resourcegroup.framework.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.resourcegroup.beans.ScopeFilterType;
import io.harness.resourcegroup.framework.service.ResourceGroupV2Validator;
import io.harness.resourcegroup.model.ScopeSelector;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2DTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2Request;
import io.harness.utils.ScopeUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class ResourceGroupV2ValidatorImpl implements ResourceGroupV2Validator {
  private boolean isCurrentOnlyScopeSelector(Scope scope, ScopeSelector scopeSelector) {
    Scope scopeOfSelector = Scope.of(
        scopeSelector.getAccountIdentifier(), scopeSelector.getOrgIdentifier(), scopeSelector.getProjectIdentifier());
    if (scope.equals(scopeOfSelector) && ScopeFilterType.EXCLUDING_CHILD_SCOPES.equals(scopeSelector.getFilter())) {
      return true;
    }
    return false;
  }

  public void validateResourceGroup(ResourceGroupV2Request resourceGroupRequest) {
    if (resourceGroupRequest == null || resourceGroupRequest.getResourceGroup() == null) {
      return;
    }
    ResourceGroupV2DTO resourceGroupDTO = resourceGroupRequest.getResourceGroup();
    Scope scopeOfResourceGroup = Scope.of(resourceGroupDTO.getAccountIdentifier(), resourceGroupDTO.getOrgIdentifier(),
        resourceGroupDTO.getProjectIdentifier());
    AtomicBoolean includeStaticResources = new AtomicBoolean(false);

    if (ScopeUtils.isAccountScope(scopeOfResourceGroup)) {
      resourceGroupDTO.getIncludedScopes().forEach(scope -> {
        includeStaticResources.set(isCurrentOnlyScopeSelector(scopeOfResourceGroup, scope));
        if (!scope.getAccountIdentifier().equals(scopeOfResourceGroup.getAccountIdentifier())) {
          throw new InvalidRequestException("Scope of included scopes does not match with the scope of resource group");
        }
      });
    } else if (ScopeUtils.isOrganizationScope(scopeOfResourceGroup)) {
      resourceGroupDTO.getIncludedScopes().forEach(scope -> {
        includeStaticResources.set(isCurrentOnlyScopeSelector(scopeOfResourceGroup, scope));
        if (!scopeOfResourceGroup.getAccountIdentifier().equals(scope.getAccountIdentifier())
            || scopeOfResourceGroup.getOrgIdentifier().equals(scope.getOrgIdentifier())) {
          throw new InvalidRequestException("Scope of included scopes does not match with the scope of resource group");
        }
      });
    } else if (ScopeUtils.isProjectScope(scopeOfResourceGroup)) {
      resourceGroupDTO.getIncludedScopes().forEach(scope -> {
        includeStaticResources.set(false);
        if (!scopeOfResourceGroup.equals(
                Scope.of(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier()))) {
          throw new InvalidRequestException("Scope of included scopes does not match with the scope of resource group");
        }
      });
    } else {
      throw new InvalidRequestException("Invalid scope of resource group");
    }

    if (!isEmpty(resourceGroupDTO.getResourceFilter())) {
      resourceGroupDTO.getResourceFilter().forEach(filter -> {
        if (!includeStaticResources.get() && !isEmpty(filter.getIdentifiers())) {
          throw new InvalidRequestException(
              "Cannot provide specific identifiers in resource filter for a dynamic scope");
        }
      });
    }
  }
}
