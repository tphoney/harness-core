package io.harness.resourcegroup.framework.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.REETIKA;

import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.outbox.api.OutboxService;
import io.harness.resourcegroup.ResourceGroupTestBase;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupV2Mapper;
import io.harness.resourcegroup.framework.repositories.spring.ResourceGroupV2Repository;
import io.harness.resourcegroup.model.ResourceGroupV2;
import io.harness.resourcegroup.model.ResourceGroupV2.ResourceGroupV2Keys;
import io.harness.resourcegroup.remote.dto.ManagedFilter;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2DTO;
import io.harness.rule.Owner;
import io.harness.utils.PageTestUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
public class ResourceGroupV2ServiceImplTest extends ResourceGroupTestBase {
  @Inject private ResourceGroupV2Repository resourceGroupV2Repository;
  private ResourceGroupV2Repository resourceGroupV2RepositoryMock;
  private OutboxService outboxService;
  private TransactionTemplate transactionTemplate;
  private ResourceGroupV2ServiceImpl resourceGroupV2Service;
  private ResourceGroupV2ServiceImpl resourceGroupV2ServiceMockRepo;
  private PageRequest pageRequest;

  @Before
  public void setup() {
    resourceGroupV2RepositoryMock = mock(ResourceGroupV2Repository.class);
    outboxService = mock(OutboxService.class);
    transactionTemplate = mock(TransactionTemplate.class);
    resourceGroupV2Service =
        spy(new ResourceGroupV2ServiceImpl(resourceGroupV2Repository, outboxService, transactionTemplate));
    resourceGroupV2ServiceMockRepo =
        spy(new ResourceGroupV2ServiceImpl(resourceGroupV2RepositoryMock, outboxService, transactionTemplate));

    pageRequest = PageRequest.builder().pageIndex(0).pageSize(50).build();
  }

  private Set<String> getRandomStrings(int count) {
    Set<String> strings = new HashSet<>();
    for (int i = 0; i < count; i++) {
      strings.add(randomAlphabetic(10));
    }
    return strings;
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testGet() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    resourceGroupV2Service.get(Scope.of(accountIdentifier, orgIdentifier, null), identifier, ManagedFilter.NO_FILTER);

    Criteria criteria =
        getActualGetCriteria(Scope.of(accountIdentifier, orgIdentifier, null), identifier, ManagedFilter.NO_FILTER);
    assertGetNoFilterCriteria(criteria, accountIdentifier, orgIdentifier, null, identifier);
  }

  private void assertGetNoFilterCriteria(
      Criteria criteria, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Criteria expectedCriteria = Criteria.where(ResourceGroupV2Keys.identifier).is(identifier);
    Criteria scopeExpectedCriteria = getExpectedScopeCriteria(accountIdentifier, orgIdentifier, null);
    Criteria expectedManagedCriteria = getExpectedManagedCriteria();
    expectedManagedCriteria.and(ResourceGroupV2Keys.allowedScopeLevels)
        .is(ScopeLevel.of(accountIdentifier, orgIdentifier, null).toString().toLowerCase());
    expectedCriteria.orOperator(scopeExpectedCriteria, expectedManagedCriteria);
    assertEquals(expectedCriteria, criteria);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testGetOnlyManagedFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    resourceGroupV2Service.get(
        Scope.of(accountIdentifier, orgIdentifier, null), identifier, ManagedFilter.ONLY_MANAGED);

    Criteria criteria =
        getActualGetCriteria(Scope.of(accountIdentifier, orgIdentifier, null), identifier, ManagedFilter.ONLY_MANAGED);
    assertGetOnlyManagedFilterCriteria(criteria, accountIdentifier, orgIdentifier, null, identifier);
  }

  private void assertGetOnlyManagedFilterCriteria(
      Criteria criteria, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Criteria expectedCriteria = Criteria.where(ResourceGroupV2Keys.identifier).is(identifier);
    Criteria expectedManagedCriteria = getExpectedManagedCriteria();
    if (isNotEmpty(accountIdentifier)) {
      expectedManagedCriteria.and(ResourceGroupV2Keys.allowedScopeLevels)
          .is(ScopeLevel.of(accountIdentifier, orgIdentifier, null).toString().toLowerCase());
    }
    expectedCriteria.andOperator(expectedManagedCriteria);
    assertEquals(expectedCriteria, criteria);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testGetOnlyCustomFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    resourceGroupV2Service.get(Scope.of(accountIdentifier, orgIdentifier, null), identifier, ManagedFilter.ONLY_CUSTOM);

    Criteria criteria =
        getActualGetCriteria(Scope.of(accountIdentifier, orgIdentifier, null), identifier, ManagedFilter.ONLY_CUSTOM);
    assertGetOnlyCustomFilterCriteria(criteria, accountIdentifier, orgIdentifier, null, identifier);
  }

  private void assertGetOnlyCustomFilterCriteria(
      Criteria criteria, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Criteria expectedCriteria = Criteria.where(ResourceGroupV2Keys.identifier).is(identifier);
    Criteria scopeExpectedCriteria = getExpectedScopeCriteria(accountIdentifier, orgIdentifier, null);
    expectedCriteria.andOperator(scopeExpectedCriteria);
    assertEquals(expectedCriteria, criteria);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testGetNullScope() {
    String identifier = randomAlphabetic(10);
    resourceGroupV2Service.get(null, identifier, ManagedFilter.NO_FILTER);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testGetNullScopeOnlyManagedFilter() {
    String identifier = randomAlphabetic(10);
    resourceGroupV2Service.get(Scope.of(null, null, null), identifier, ManagedFilter.ONLY_MANAGED);

    Criteria criteria = getActualGetCriteria(null, identifier, ManagedFilter.ONLY_MANAGED);
    assertGetOnlyManagedFilterCriteria(criteria, null, null, null, identifier);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testGetNullScopeOnlyCustomFilter() {
    String identifier = randomAlphabetic(10);
    resourceGroupV2Service.get(null, identifier, ManagedFilter.ONLY_CUSTOM);
  }

  private Criteria getActualGetCriteria(Scope scope, String identifier, ManagedFilter managedFilter) {
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(resourceGroupV2RepositoryMock.find(any())).thenReturn(Optional.empty());
    resourceGroupV2ServiceMockRepo.get(scope, identifier, managedFilter);
    verify(resourceGroupV2RepositoryMock).find(criteriaArgumentCaptor.capture());
    return criteriaArgumentCaptor.getValue();
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testListScopeFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    ResourceGroupFilterDTO resourceGroupFilterDTO =
        ResourceGroupFilterDTO.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    resourceGroupV2Service.list(resourceGroupFilterDTO, pageRequest);

    Criteria criteria = getActualListCriteria(resourceGroupFilterDTO);
    Criteria expectedCriteria = new Criteria();
    Criteria scopeExpectedCriteria = getExpectedScopeCriteria(accountIdentifier, orgIdentifier, null);
    Criteria expectedManagedCriteria = getExpectedManagedCriteria();
    expectedManagedCriteria.and(ResourceGroupV2Keys.allowedScopeLevels)
        .is(ScopeLevel.of(accountIdentifier, orgIdentifier, null).toString().toLowerCase());
    expectedCriteria.andOperator(new Criteria().orOperator(scopeExpectedCriteria, expectedManagedCriteria));
    assertEquals(expectedCriteria, criteria);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testListScopeOnlyManagedFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    ResourceGroupFilterDTO resourceGroupFilterDTO = ResourceGroupFilterDTO.builder()
                                                        .accountIdentifier(accountIdentifier)
                                                        .orgIdentifier(orgIdentifier)
                                                        .managedFilter(ManagedFilter.ONLY_MANAGED)
                                                        .build();
    resourceGroupV2Service.list(resourceGroupFilterDTO, pageRequest);

    Criteria criteria = getActualListCriteria(resourceGroupFilterDTO);
    Criteria expectedCriteria = new Criteria();
    Criteria expectedManagedCriteria = getExpectedManagedCriteria();
    expectedManagedCriteria.and(ResourceGroupV2Keys.allowedScopeLevels)
        .is(ScopeLevel.of(accountIdentifier, orgIdentifier, null).toString().toLowerCase());
    expectedCriteria.andOperator(expectedManagedCriteria);
    assertEquals(expectedCriteria, criteria);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testListScopeOnlyCustomFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    ResourceGroupFilterDTO resourceGroupFilterDTO = ResourceGroupFilterDTO.builder()
                                                        .accountIdentifier(accountIdentifier)
                                                        .orgIdentifier(orgIdentifier)
                                                        .managedFilter(ManagedFilter.ONLY_CUSTOM)
                                                        .build();
    resourceGroupV2Service.list(resourceGroupFilterDTO, pageRequest);

    Criteria criteria = getActualListCriteria(resourceGroupFilterDTO);
    Criteria expectedCriteria = new Criteria();
    Criteria scopeExpectedCriteria = getExpectedScopeCriteria(accountIdentifier, orgIdentifier, null);
    expectedCriteria.andOperator(scopeExpectedCriteria);
    assertEquals(expectedCriteria, criteria);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testListIdentifierFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    Set<String> identifierFilter = getRandomStrings(5);
    ResourceGroupFilterDTO resourceGroupFilterDTO = ResourceGroupFilterDTO.builder()
                                                        .accountIdentifier(accountIdentifier)
                                                        .orgIdentifier(orgIdentifier)
                                                        .identifierFilter(identifierFilter)
                                                        .build();
    resourceGroupV2Service.list(resourceGroupFilterDTO, pageRequest);

    Criteria criteria = getActualListCriteria(resourceGroupFilterDTO);
    Criteria expectedCriteria = Criteria.where(ResourceGroupV2Keys.identifier).in(identifierFilter);
    Criteria scopeExpectedCriteria = getExpectedScopeCriteria(accountIdentifier, orgIdentifier, null);
    Criteria expectedManagedCriteria = getExpectedManagedCriteria();
    expectedManagedCriteria.and(ResourceGroupV2Keys.allowedScopeLevels)
        .is(ScopeLevel.of(accountIdentifier, orgIdentifier, null).toString().toLowerCase());
    expectedCriteria.andOperator(new Criteria().orOperator(scopeExpectedCriteria, expectedManagedCriteria));
    assertEquals(expectedCriteria, criteria);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testListNullScopeAndScopeLevelFilter() {
    Set<String> scopeLevelFilter = getRandomStrings(3);
    ResourceGroupFilterDTO resourceGroupFilterDTO =
        ResourceGroupFilterDTO.builder().managedFilter(ManagedFilter.ONLY_MANAGED).build();
    resourceGroupFilterDTO.setScopeLevelFilter(scopeLevelFilter);
    resourceGroupV2Service.list(resourceGroupFilterDTO, pageRequest);

    Criteria criteria = getActualListCriteria(resourceGroupFilterDTO);
    Criteria expectedCriteria = new Criteria();
    expectedCriteria.and(ResourceGroupV2Keys.allowedScopeLevels).in(scopeLevelFilter);
    Criteria expectedManagedCriteria = getExpectedManagedCriteria();

    expectedCriteria.andOperator(expectedManagedCriteria);
    assertEquals(expectedCriteria, criteria);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testListScopeAndScopeLevelFilter() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    Set<String> scopeLevelFilter = getRandomStrings(5);
    ResourceGroupFilterDTO resourceGroupFilterDTO =
        ResourceGroupFilterDTO.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgIdentifier).build();
    resourceGroupFilterDTO.setScopeLevelFilter(scopeLevelFilter);
    resourceGroupV2Service.list(resourceGroupFilterDTO, pageRequest);

    Criteria criteria = getActualListCriteria(resourceGroupFilterDTO);
    Criteria expectedCriteria = new Criteria();
    Criteria scopeExpectedCriteria = getExpectedScopeCriteria(accountIdentifier, orgIdentifier, null);
    Criteria expectedManagedCriteria = getExpectedManagedCriteria();
    if (isNotEmpty(accountIdentifier)) {
      expectedManagedCriteria.and(ResourceGroupV2Keys.allowedScopeLevels)
          .is(ScopeLevel.of(accountIdentifier, orgIdentifier, null).toString().toLowerCase());
    }
    expectedCriteria.andOperator(new Criteria().orOperator(scopeExpectedCriteria, expectedManagedCriteria));
    assertEquals(expectedCriteria, criteria);
  }

  private Criteria getExpectedScopeCriteria(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return Criteria.where(ResourceGroupV2Keys.accountIdentifier)
        .is(accountIdentifier)
        .and(ResourceGroupV2Keys.orgIdentifier)
        .is(orgIdentifier)
        .and(ResourceGroupV2Keys.projectIdentifier)
        .is(projectIdentifier)
        .and(ResourceGroupV2Keys.harnessManaged)
        .ne(true);
  }

  private Criteria getExpectedManagedCriteria() {
    return Criteria.where(ResourceGroupV2Keys.accountIdentifier)
        .is(null)
        .and(ResourceGroupV2Keys.orgIdentifier)
        .is(null)
        .and(ResourceGroupV2Keys.projectIdentifier)
        .is(null)
        .and(ResourceGroupV2Keys.harnessManaged)
        .is(true);
  }

  private Criteria getActualListCriteria(ResourceGroupFilterDTO resourceGroupFilterDTO) {
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(resourceGroupV2RepositoryMock.findAll(any(), any())).thenReturn(PageTestUtils.getPage(emptyList(), 0));
    resourceGroupV2ServiceMockRepo.list(resourceGroupFilterDTO, pageRequest);
    verify(resourceGroupV2RepositoryMock).findAll(criteriaArgumentCaptor.capture(), any());
    return criteriaArgumentCaptor.getValue();
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testDeleteManaged() {
    String identifier = randomAlphabetic(10);
    when(transactionTemplate.execute(any())).thenReturn(true);
    resourceGroupV2Service.deleteManaged(identifier);
    verify(transactionTemplate, times(0)).execute(any());

    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(resourceGroupV2RepositoryMock.find(any()))
        .thenReturn(Optional.of(ResourceGroupV2.builder().identifier(identifier).build()));
    resourceGroupV2ServiceMockRepo.deleteManaged(identifier);
    verify(resourceGroupV2RepositoryMock).find(criteriaArgumentCaptor.capture());

    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertGetOnlyManagedFilterCriteria(criteria, null, null, null, identifier);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testUpdateNotFound() {
    String identifier = randomAlphabetic(10);
    ResourceGroupV2 resourceGroupV2Update = ResourceGroupV2.builder().identifier(identifier).build();
    ResourceGroupV2DTO resourceGroupV2UpdateDTO = ResourceGroupV2Mapper.toDTO(resourceGroupV2Update);
    when(transactionTemplate.execute(any())).thenReturn(resourceGroupV2Update);
    try {
      resourceGroupV2Service.update(resourceGroupV2UpdateDTO, true);
      fail();
    } catch (InvalidRequestException invalidRequestException) {
      verify(transactionTemplate, times(0)).execute(any());
    }

    try {
      resourceGroupV2Service.update(resourceGroupV2UpdateDTO, false);
      fail();
    } catch (InvalidRequestException invalidRequestException) {
      verify(transactionTemplate, times(0)).execute(any());
    }
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testUpdateManagedTrue() {
    String identifier = randomAlphabetic(10);
    ResourceGroupV2 resourceGroupV2 =
        ResourceGroupV2.builder().identifier(identifier).allowedScopeLevels(new HashSet<>()).build();
    ResourceGroupV2DTO resourceGroupV2UpdateDTO = ResourceGroupV2DTO.builder().identifier(identifier).build();
    resourceGroupV2UpdateDTO.setAllowedScopeLevels(Sets.newHashSet("account"));
    ResourceGroupV2 resourceGroupV2Update = ResourceGroupV2Mapper.fromDTO(resourceGroupV2UpdateDTO);
    when(transactionTemplate.execute(any())).thenReturn(resourceGroupV2Update);

    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(resourceGroupV2RepositoryMock.find(any())).thenReturn(Optional.of(resourceGroupV2));
    resourceGroupV2ServiceMockRepo.update(resourceGroupV2UpdateDTO, true);
    verify(resourceGroupV2RepositoryMock).find(criteriaArgumentCaptor.capture());

    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertGetOnlyManagedFilterCriteria(criteria, null, null, null, identifier);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testUpdateManagedFalse() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ResourceGroupV2 resourceGroupV2 = ResourceGroupV2.builder()
                                          .accountIdentifier(accountIdentifier)
                                          .identifier(identifier)
                                          .allowedScopeLevels(new HashSet<>())
                                          .build();
    ResourceGroupV2DTO resourceGroupV2UpdateDTO =
        ResourceGroupV2DTO.builder().accountIdentifier(accountIdentifier).identifier(identifier).build();

    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(resourceGroupV2RepositoryMock.find(any())).thenReturn(Optional.of(resourceGroupV2));
    resourceGroupV2ServiceMockRepo.update(resourceGroupV2UpdateDTO, false);
    verify(resourceGroupV2RepositoryMock).find(criteriaArgumentCaptor.capture());

    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertGetOnlyCustomFilterCriteria(criteria, accountIdentifier, null, null, identifier);
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testUpdateManagedFalseScopeLevelUpdateNotAllowed() {
    String accountIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ResourceGroupV2 resourceGroupV2 = ResourceGroupV2.builder()
                                          .accountIdentifier(accountIdentifier)
                                          .identifier(identifier)
                                          .allowedScopeLevels(Sets.newHashSet("account"))
                                          .build();
    ResourceGroupV2DTO resourceGroupV2UpdateDTO =
        ResourceGroupV2DTO.builder().accountIdentifier(accountIdentifier).identifier(identifier).build();
    resourceGroupV2UpdateDTO.setAllowedScopeLevels(Sets.newHashSet("account", "organization"));

    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(resourceGroupV2RepositoryMock.find(any())).thenReturn(Optional.of(resourceGroupV2));
    try {
      resourceGroupV2ServiceMockRepo.update(resourceGroupV2UpdateDTO, false);
      fail();
    } catch (InvalidRequestException invalidRequestException) {
      verify(resourceGroupV2RepositoryMock).find(criteriaArgumentCaptor.capture());
    }

    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertGetOnlyCustomFilterCriteria(criteria, accountIdentifier, null, null, identifier);
  }
}
