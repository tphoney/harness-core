package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Boolean.TRUE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.beans.SortOrder;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.outbox.api.OutboxService;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.resourcegroup.framework.events.ResourceGroupV2CreateEvent;
import io.harness.resourcegroup.framework.events.ResourceGroupV2DeleteEvent;
import io.harness.resourcegroup.framework.events.ResourceGroupV2UpdateEvent;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupV2Mapper;
import io.harness.resourcegroup.framework.repositories.spring.ResourceGroupV2Repository;
import io.harness.resourcegroup.framework.service.ResourceGroupV2Service;
import io.harness.resourcegroup.model.ResourceFilter;
import io.harness.resourcegroup.model.ResourceGroupV2;
import io.harness.resourcegroup.remote.dto.ManagedFilter;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2DTO;
import io.harness.resourcegroupclient.ResourceGroupV2Response;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@OwnedBy(PL)
@ValidateOnExecution
public class ResourceGroupV2ServiceImpl implements ResourceGroupV2Service {
  ResourceGroupV2Repository resourceGroupV2Repository;
  OutboxService outboxService;
  TransactionTemplate transactionTemplate;

  @Inject
  public ResourceGroupV2ServiceImpl(ResourceGroupV2Repository resourceGroupV2Repository, OutboxService outboxService,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate) {
    this.resourceGroupV2Repository = resourceGroupV2Repository;
    this.outboxService = outboxService;
    this.transactionTemplate = transactionTemplate;
  }

  private ResourceGroupV2 createInternal(ResourceGroupV2 resourceGroup) {
    return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      ResourceGroupV2 savedResourceGroup = resourceGroupV2Repository.save(resourceGroup);
      outboxService.save(new ResourceGroupV2CreateEvent(
          savedResourceGroup.getAccountIdentifier(), ResourceGroupV2Mapper.toDTO(savedResourceGroup)));
      return savedResourceGroup;
    }));
  }

  private ResourceGroupV2 create(ResourceGroupV2 resourceGroup) {
    try {
      return createInternal(resourceGroup);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("A resource group with identifier %s already exists at the specified scope",
              resourceGroup.getIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public ResourceGroupV2Response create(ResourceGroupV2DTO resourceGroupDTO, boolean harnessManaged) {
    ResourceGroupV2 resourceGroup = ResourceGroupV2Mapper.fromDTO(resourceGroupDTO);
    resourceGroup.setHarnessManaged(harnessManaged);

    return ResourceGroupV2Mapper.toResponseWrapper(create(resourceGroup));
  }

  private Criteria getResourceGroupFilterCriteria(ResourceGroupFilterDTO resourceGroupFilterDTO) {
    Criteria criteria = new Criteria();
    if (isNotEmpty(resourceGroupFilterDTO.getIdentifierFilter())) {
      criteria.and(ResourceGroupV2.ResourceGroupV2Keys.identifier).in(resourceGroupFilterDTO.getIdentifierFilter());
    }
    Criteria scopeCriteria = getBaseScopeCriteria(resourceGroupFilterDTO.getAccountIdentifier(),
        resourceGroupFilterDTO.getOrgIdentifier(), resourceGroupFilterDTO.getProjectIdentifier())
                                 .and(ResourceGroupV2.ResourceGroupV2Keys.harnessManaged)
                                 .ne(true);
    Criteria managedCriteria =
        getBaseScopeCriteria(null, null, null).and(ResourceGroupV2.ResourceGroupV2Keys.harnessManaged).is(true);

    if (isNotEmpty(resourceGroupFilterDTO.getAccountIdentifier())) {
      managedCriteria.and(ResourceGroupV2.ResourceGroupV2Keys.allowedScopeLevels)
          .is(ScopeLevel
                  .of(resourceGroupFilterDTO.getAccountIdentifier(), resourceGroupFilterDTO.getOrgIdentifier(),
                      resourceGroupFilterDTO.getProjectIdentifier())
                  .toString()
                  .toLowerCase());
    } else if (isNotEmpty(resourceGroupFilterDTO.getScopeLevelFilter())) {
      criteria.and(ResourceGroupV2.ResourceGroupV2Keys.allowedScopeLevels)
          .in(resourceGroupFilterDTO.getScopeLevelFilter());
    }

    List<Criteria> andOperatorCriteriaList = new ArrayList<>();

    if (ManagedFilter.ONLY_MANAGED.equals(resourceGroupFilterDTO.getManagedFilter())) {
      andOperatorCriteriaList.add(managedCriteria);
    } else if (ManagedFilter.ONLY_CUSTOM.equals(resourceGroupFilterDTO.getManagedFilter())) {
      andOperatorCriteriaList.add(scopeCriteria);
    } else {
      andOperatorCriteriaList.add(new Criteria().orOperator(scopeCriteria, managedCriteria));
    }

    if (isNotEmpty(resourceGroupFilterDTO.getSearchTerm())) {
      andOperatorCriteriaList.add(new Criteria().orOperator(
          Criteria.where(ResourceGroupV2.ResourceGroupV2Keys.name).regex(resourceGroupFilterDTO.getSearchTerm(), "i"),
          Criteria.where(ResourceGroupV2.ResourceGroupV2Keys.identifier)
              .regex(resourceGroupFilterDTO.getSearchTerm(), "i"),
          Criteria.where(ResourceGroupV2.ResourceGroupV2Keys.tags + "." + NGTag.NGTagKeys.key)
              .regex(resourceGroupFilterDTO.getSearchTerm(), "i"),
          Criteria.where(ResourceGroupV2.ResourceGroupV2Keys.tags + "." + NGTag.NGTagKeys.value)
              .regex(resourceGroupFilterDTO.getSearchTerm(), "i")));
    }

    if (isNotEmpty(resourceGroupFilterDTO.getResourceSelectorFilterList())) {
      List<Criteria> resourceSelectorCriteria = new ArrayList<>();
      resourceGroupFilterDTO.getResourceSelectorFilterList().forEach(resourceSelectorFilter
          -> resourceSelectorCriteria.add(Criteria.where(ResourceGroupV2.ResourceGroupV2Keys.resourceFilter)
                                              .elemMatch(Criteria.where(ResourceFilter.ResourceFilterKeys.resourceType)
                                                             .is(resourceSelectorFilter.getResourceType())
                                                             .and(ResourceFilter.ResourceFilterKeys.identifiers)
                                                             .is(resourceSelectorFilter.getResourceIdentifier()))));
      andOperatorCriteriaList.add(new Criteria().orOperator(resourceSelectorCriteria.toArray(new Criteria[0])));
    }

    criteria.andOperator(andOperatorCriteriaList.toArray(new Criteria[0]));

    return criteria;
  }

  @Override
  public Page<ResourceGroupV2Response> list(ResourceGroupFilterDTO resourceGroupFilterDTO, PageRequest pageRequest) {
    Criteria criteria = getResourceGroupFilterCriteria(resourceGroupFilterDTO);
    return resourceGroupV2Repository.findAll(criteria, getPageRequest(pageRequest))
        .map(ResourceGroupV2Mapper::toResponseWrapper);
  }

  @Override
  public Page<ResourceGroupV2Response> list(Scope scope, PageRequest pageRequest, String searchTerm) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder harnessManagedOrder =
          SortOrder.Builder.aSortOrder()
              .withField(ResourceGroupV2.ResourceGroupV2Keys.harnessManaged, SortOrder.OrderType.DESC)
              .build();
      SortOrder lastModifiedOrder =
          SortOrder.Builder.aSortOrder()
              .withField(ResourceGroupV2.ResourceGroupV2Keys.lastModifiedAt, SortOrder.OrderType.DESC)
              .build();
      pageRequest.setSortOrders(ImmutableList.of(harnessManagedOrder, lastModifiedOrder));
    }
    Pageable page = getPageRequest(pageRequest);
    ResourceGroupFilterDTO resourceGroupFilterDTO = ResourceGroupFilterDTO.builder()
                                                        .accountIdentifier(scope.getAccountIdentifier())
                                                        .orgIdentifier(scope.getOrgIdentifier())
                                                        .projectIdentifier(scope.getProjectIdentifier())
                                                        .searchTerm(searchTerm)
                                                        .build();
    Criteria criteria = getResourceGroupFilterCriteria(resourceGroupFilterDTO);
    return resourceGroupV2Repository.findAll(criteria, page).map(ResourceGroupV2Mapper::toResponseWrapper);
  }

  public Optional<ResourceGroupV2Response> get(Scope scope, String identifier, ManagedFilter managedFilter) {
    Optional<ResourceGroupV2> resourceGroupOpt = getResourceGroup(scope, identifier, managedFilter);
    return Optional.ofNullable(ResourceGroupV2Mapper.toResponseWrapper(resourceGroupOpt.orElse(null)));
  }

  private Optional<ResourceGroupV2> getResourceGroup(Scope scope, String identifier, ManagedFilter managedFilter) {
    Criteria criteria = new Criteria();
    criteria.and(ResourceGroupV2.ResourceGroupV2Keys.identifier).is(identifier);
    if ((scope == null || isEmpty(scope.getAccountIdentifier())) && !ManagedFilter.ONLY_MANAGED.equals(managedFilter)) {
      throw new InvalidRequestException(
          "Either managed filter should be set to only managed, or scope filter should be non-empty");
    }

    Criteria managedCriteria =
        getBaseScopeCriteria(null, null, null).and(ResourceGroupV2.ResourceGroupV2Keys.harnessManaged).is(true);

    if (ManagedFilter.ONLY_MANAGED.equals(managedFilter)) {
      if (scope != null && isNotEmpty(scope.getAccountIdentifier())) {
        managedCriteria.and(ResourceGroupV2.ResourceGroupV2Keys.allowedScopeLevels)
            .is(ScopeLevel.of(scope).toString().toLowerCase());
      }
      criteria.andOperator(managedCriteria);
    } else if (ManagedFilter.ONLY_CUSTOM.equals(managedFilter)) {
      criteria.andOperator(
          getBaseScopeCriteria(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier())
              .and(ResourceGroupV2.ResourceGroupV2Keys.harnessManaged)
              .ne(true));
    } else {
      managedCriteria.and(ResourceGroupV2.ResourceGroupV2Keys.allowedScopeLevels)
          .is(ScopeLevel.of(scope).toString().toLowerCase());
      criteria.orOperator(
          getBaseScopeCriteria(scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier())
              .and(ResourceGroupV2.ResourceGroupV2Keys.harnessManaged)
              .ne(true),
          managedCriteria);
    }

    return resourceGroupV2Repository.find(criteria);
  }

  private Criteria getBaseScopeCriteria(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return Criteria.where(ResourceGroupV2.ResourceGroupV2Keys.accountIdentifier)
        .is(accountIdentifier)
        .and(ResourceGroupV2.ResourceGroupV2Keys.orgIdentifier)
        .is(orgIdentifier)
        .and(ResourceGroupV2.ResourceGroupV2Keys.projectIdentifier)
        .is(projectIdentifier);
  }

  @Override
  public Optional<ResourceGroupV2Response> update(ResourceGroupV2DTO resourceGroupDTO, boolean harnessManaged) {
    ManagedFilter managedFilter = harnessManaged ? ManagedFilter.ONLY_MANAGED : ManagedFilter.ONLY_CUSTOM;
    Optional<ResourceGroupV2> resourceGroupOpt =
        getResourceGroup(Scope.of(resourceGroupDTO.getAccountIdentifier(), resourceGroupDTO.getOrgIdentifier(),
                             resourceGroupDTO.getProjectIdentifier()),
            resourceGroupDTO.getIdentifier(), managedFilter);
    if (!resourceGroupOpt.isPresent()) {
      throw new InvalidRequestException(
          String.format("Resource group with Identifier [{%s}] does not exist", resourceGroupDTO.getIdentifier()));
    }
    ResourceGroupV2 updatedResourceGroup = ResourceGroupV2Mapper.fromDTO(resourceGroupDTO);

    ResourceGroupV2 savedResourceGroup = resourceGroupOpt.get();
    if (savedResourceGroup.getHarnessManaged().equals(TRUE) && !harnessManaged) {
      throw new InvalidRequestException("Can't update managed resource group");
    }

    ResourceGroupV2DTO oldResourceGroup =
        (ResourceGroupV2DTO) NGObjectMapperHelper.clone(ResourceGroupV2Mapper.toDTO(savedResourceGroup));
    savedResourceGroup.setName(updatedResourceGroup.getName());
    savedResourceGroup.setColor(updatedResourceGroup.getColor());
    savedResourceGroup.setTags(updatedResourceGroup.getTags());
    savedResourceGroup.setDescription(updatedResourceGroup.getDescription());
    savedResourceGroup.setResourceFilter(updatedResourceGroup.getResourceFilter());
    savedResourceGroup.setIncludedScopes(updatedResourceGroup.getIncludedScopes());
    if (areScopeLevelsUpdated(savedResourceGroup, updatedResourceGroup) && !harnessManaged) {
      throw new InvalidRequestException("Cannot change the scopes at which this resource group can be used.");
    }
    savedResourceGroup.setAllowedScopeLevels(updatedResourceGroup.getAllowedScopeLevels());

    updatedResourceGroup =
        Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
          ResourceGroupV2 resourceGroup = resourceGroupV2Repository.save(
              ResourceGroupV2Mapper.fromDTO(resourceGroupDTO, resourceGroupOpt.get().getHarnessManaged()));
          outboxService.save(new ResourceGroupV2UpdateEvent(
              savedResourceGroup.getAccountIdentifier(), ResourceGroupV2Mapper.toDTO(resourceGroup), oldResourceGroup));
          return resourceGroup;
        }));
    return Optional.ofNullable(ResourceGroupV2Mapper.toResponseWrapper(updatedResourceGroup));
  }

  @Override
  public void deleteManaged(String identifier) {
    Optional<ResourceGroupV2> resourceGroupOpt = getResourceGroup(null, identifier, ManagedFilter.ONLY_MANAGED);
    if (!resourceGroupOpt.isPresent()) {
      return;
    }
    ResourceGroupV2 resourceGroup = resourceGroupOpt.get();

    Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      resourceGroupV2Repository.delete(resourceGroup);
      outboxService.save(new ResourceGroupV2DeleteEvent(null, ResourceGroupV2Mapper.toDTO(resourceGroup)));
      return true;
    }));
  }

  @Override
  public void deleteByScope(Scope scope) {
    if (scope == null || isEmpty(scope.getAccountIdentifier())) {
      throw new InvalidRequestException("Invalid scope. Cannot proceed with deletion.");
    }
    Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      List<ResourceGroupV2> deletedResourceGroups =
          resourceGroupV2Repository.deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
              scope.getAccountIdentifier(), scope.getOrgIdentifier(), scope.getProjectIdentifier());
      if (isNotEmpty(deletedResourceGroups)) {
        deletedResourceGroups.forEach(rg
            -> outboxService.save(
                new ResourceGroupV2DeleteEvent(rg.getAccountIdentifier(), ResourceGroupV2Mapper.toDTO(rg))));
      }
      return true;
    }));
  }

  @Override
  public boolean delete(Scope scope, String identifier) {
    Optional<ResourceGroupV2> resourceGroupOpt = getResourceGroup(scope, identifier, ManagedFilter.ONLY_CUSTOM);
    if (!resourceGroupOpt.isPresent()) {
      return false;
    }

    ResourceGroupV2 resourceGroup = resourceGroupOpt.get();
    if (Boolean.TRUE.equals(resourceGroup.getHarnessManaged())) {
      throw new InvalidRequestException("Managed resource group cannot be deleted");
    }

    return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
      resourceGroupV2Repository.delete(resourceGroup);
      outboxService.save(
          new ResourceGroupV2DeleteEvent(scope.getAccountIdentifier(), ResourceGroupV2Mapper.toDTO(resourceGroup)));
      return true;
    }));
  }

  private boolean areScopeLevelsUpdated(ResourceGroupV2 currentResourceGroup, ResourceGroupV2 resourceGroupUpdate) {
    if (isEmpty(currentResourceGroup.getAllowedScopeLevels())) {
      return false;
    }
    return !currentResourceGroup.getAllowedScopeLevels().equals(resourceGroupUpdate.getAllowedScopeLevels());
  }
}
