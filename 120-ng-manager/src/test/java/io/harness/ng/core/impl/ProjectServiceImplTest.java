/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.impl;

import static io.harness.ModuleType.CD;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.ng.core.remote.ProjectMapper.toProject;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.KARAN;
import static io.harness.rule.OwnerRule.MEET;
import static io.harness.utils.PageTestUtils.getPage;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;

import io.harness.CategoryTest;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import io.harness.exception.InvalidRequestException;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.beans.ProjectsPerOrganizationCount;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.remote.utils.ScopeAccessHelper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.core.spring.ProjectRepository;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;
import io.harness.telemetry.helpers.ProjectInstrumentationHelper;

import io.dropwizard.jersey.validation.JerseyViolationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
public class ProjectServiceImplTest extends CategoryTest {
  @Mock private ProjectRepository projectRepository;
  @Mock private OrganizationService organizationService;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private OutboxService outboxService;
  @Mock private NgUserService ngUserService;
  @Mock private AccessControlClient accessControlClient;
  @Mock private ScopeAccessHelper scopeAccessHelper;
  @InjectMocks ProjectInstrumentationHelper instrumentationHelper;
  private ProjectServiceImpl projectService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    projectService = spy(new ProjectServiceImpl(projectRepository, organizationService, transactionTemplate,
        outboxService, ngUserService, accessControlClient, scopeAccessHelper, instrumentationHelper));
    when(scopeAccessHelper.getPermittedScopes(any())).then(returnsFirstArg());
  }

  private ProjectDTO createProjectDTO(String orgIdentifier, String identifier) {
    return ProjectDTO.builder()
        .orgIdentifier(orgIdentifier)
        .identifier(identifier)
        .name(randomAlphabetic(10))
        .color(randomAlphabetic(10))
        .build();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateProject_CorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(orgIdentifier, randomAlphabetic(10));
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    setContextData(accountIdentifier);

    when(projectRepository.save(project)).thenReturn(project);
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(random(Organization.class)));

    projectService.create(accountIdentifier, orgIdentifier, projectDTO);
    try {
      verify(transactionTemplate, times(1)).execute(any());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setContextData(String accountIdentifier) {
    GlobalContext globalContext = new GlobalContext();
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder()
            .principal(new UserPrincipal("user", "admin@harness.io", "user", accountIdentifier))
            .build();
    globalContext.setGlobalContextRecord(sourcePrincipalContextData);
    GlobalContextManager.set(globalContext);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testCreateProject_IncorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(orgIdentifier, randomAlphabetic(10));
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    setContextData(accountIdentifier);

    projectService.create(accountIdentifier, orgIdentifier + randomAlphabetic(1), projectDTO);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateExistentProject() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    String id = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(orgIdentifier, identifier);
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    project.setIdentifier(identifier);
    project.setId(id);
    when(projectRepository.save(any())).thenReturn(project);
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(random(Organization.class)));
    when(projectService.get(accountIdentifier, orgIdentifier, identifier)).thenReturn(Optional.of(project));
    projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);

    try {
      verify(transactionTemplate, times(1)).execute(any());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test(expected = JerseyViolationException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateProject_IncorrectPayload() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(orgIdentifier, identifier);
    projectDTO.setName("");
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    project.setName(randomAlphabetic(10));
    project.setId(randomAlphabetic(10));
    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(random(Organization.class)));
    when(projectService.get(accountIdentifier, orgIdentifier, identifier)).thenReturn(Optional.of(project));
    projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testUpdateNonExistentProject() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ProjectDTO projectDTO = createProjectDTO(orgIdentifier, identifier);
    Project project = toProject(projectDTO);
    project.setAccountIdentifier(accountIdentifier);
    project.setOrgIdentifier(orgIdentifier);
    project.setIdentifier(identifier);

    when(organizationService.get(accountIdentifier, orgIdentifier)).thenReturn(Optional.of(random(Organization.class)));
    when(projectService.get(accountIdentifier, orgIdentifier, identifier)).thenReturn(Optional.empty());

    Project updatedProject = projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO);

    assertNull(updatedProject);
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testListProject() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String searchTerm = randomAlphabetic(5);
    ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);
    when(projectRepository.findAllProjects(any(Criteria.class))).thenReturn(Collections.emptyList());
    when(projectRepository.findAll(any(Criteria.class), any(Pageable.class))).thenReturn(getPage(emptyList(), 0));

    Set<String> orgIdentifiers = Collections.singleton(orgIdentifier);
    Page<Project> projectPage = projectService.listPermittedProjects(accountIdentifier, unpaged(),
        ProjectFilterDTO.builder().orgIdentifiers(orgIdentifiers).searchTerm(searchTerm).moduleType(CD).build());

    verify(projectRepository, times(1)).findAllProjects(criteriaArgumentCaptor.capture());

    Criteria criteria = criteriaArgumentCaptor.getValue();
    Document criteriaObject = criteria.getCriteriaObject();
    System.out.println(criteriaObject);
    assertEquals(5, criteriaObject.size());
    assertEquals(accountIdentifier, criteriaObject.get(ProjectKeys.accountIdentifier));
    assertTrue(criteriaObject.containsKey(ProjectKeys.orgIdentifier));
    assertTrue(criteriaObject.containsKey(ProjectKeys.deleted));
    assertTrue(criteriaObject.containsKey(ProjectKeys.modules));

    assertEquals(0, projectPage.getTotalElements());
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testProjectsCount() throws NoSuchFieldException, IllegalAccessException {
    String accountIdentifier = randomAlphabetic(10);
    ArgumentCaptor<Aggregation> aggregationArgumentCaptor = ArgumentCaptor.forClass(Aggregation.class);
    List<ProjectsPerOrganizationCount> projectsCount = new ArrayList<>();
    AggregationResults<ProjectsPerOrganizationCount> aggregationResults = mock(AggregationResults.class);
    when(projectRepository.aggregate(any(), eq(ProjectsPerOrganizationCount.class))).thenReturn(aggregationResults);
    when(aggregationResults.getMappedResults()).thenReturn(projectsCount);
    projectService.getProjectsCountPerOrganization(accountIdentifier, null);

    verify(projectRepository, times(1))
        .aggregate(aggregationArgumentCaptor.capture(), eq(ProjectsPerOrganizationCount.class));
    Aggregation aggregation = aggregationArgumentCaptor.getValue();
    assertNotNull(aggregation);

    Field f = aggregation.getClass().getDeclaredField("operations");
    f.setAccessible(true);
    List<AggregationOperation> operations = (List<AggregationOperation>) f.get(aggregation);
    assertEquals(4, operations.size());
    assertEquals(MatchOperation.class, operations.get(0).getClass());
    assertEquals(SortOperation.class, operations.get(1).getClass());
    assertEquals(GroupOperation.class, operations.get(2).getClass());
    assertEquals(ProjectionOperation.class, operations.get(3).getClass());
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testListProjects() {
    String user = generateUuid();
    Principal principal = mock(Principal.class);
    when(principal.getType()).thenReturn(PrincipalType.USER);
    when(principal.getName()).thenReturn(user);
    SourcePrincipalContextBuilder.setSourcePrincipal(principal);
    Project proj1 =
        Project.builder().name("P1").accountIdentifier("accId1").orgIdentifier("orgId1").identifier("id1").build();
    Project proj2 =
        Project.builder().name("P2").accountIdentifier("accId1").orgIdentifier("orgId2").identifier("id2").build();
    List<Project> projects = Arrays.asList(proj1, proj2);
    UserMembership userMembership1 =
        UserMembership.builder()
            .userId(user)
            .scope(Scope.builder().accountIdentifier("accId1").orgIdentifier("orgId1").projectIdentifier("id1").build())
            .build();
    UserMembership userMembership2 =
        UserMembership.builder()
            .userId(user)
            .scope(Scope.builder().accountIdentifier("accId1").orgIdentifier("orgId2").projectIdentifier("id2").build())
            .build();
    doReturn(new PageImpl<>(Arrays.asList(userMembership1, userMembership2)))
        .when(ngUserService)
        .listUserMemberships(any(), any());
    doReturn(projects).when(projectService).list(any());
    doReturn(new PageImpl<>(projects, Pageable.unpaged(), 100)).when(projectRepository).findAll(any(), any());
    PageResponse<ProjectDTO> projectsResponse =
        projectService.listProjectsForUser(user, "account", PageRequest.builder().pageSize(2).pageIndex(0).build());
    assertNotNull(projectsResponse);
    assertEquals(
        projectsResponse.getContent(), projects.stream().map(ProjectMapper::writeDTO).collect(Collectors.toList()));
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testListAllProjectsForUser() {
    String user = generateUuid();
    Principal principal = mock(Principal.class);
    when(principal.getType()).thenReturn(PrincipalType.USER);
    when(principal.getName()).thenReturn(user);
    SourcePrincipalContextBuilder.setSourcePrincipal(principal);
    Project proj1 =
        Project.builder().name("P1").accountIdentifier("accId1").orgIdentifier("orgId1").identifier("id1").build();
    Project proj2 =
        Project.builder().name("P2").accountIdentifier("accId1").orgIdentifier("orgId2").identifier("id2").build();
    List<Project> projects = Arrays.asList(proj1, proj2);
    UserMembership userMembership1 =
        UserMembership.builder()
            .userId(user)
            .scope(Scope.builder().accountIdentifier("accId1").orgIdentifier("orgId1").projectIdentifier("id1").build())
            .build();
    UserMembership userMembership2 =
        UserMembership.builder()
            .userId(user)
            .scope(Scope.builder().accountIdentifier("accId1").orgIdentifier("orgId2").projectIdentifier("id2").build())
            .build();
    doReturn(new PageImpl<>(Arrays.asList(userMembership1, userMembership2)))
        .when(ngUserService)
        .listUserMemberships(any(), any());
    doReturn(projects).when(projectService).list(any());
    doReturn(projects).when(projectRepository).findAll((Criteria) any());
    List<ProjectDTO> projectsResponse = projectService.listProjectsForUser(user, "account");
    assertNotNull(projectsResponse);
    assertEquals(projectsResponse, projects.stream().map(ProjectMapper::writeDTO).collect(Collectors.toList()));
  }
}
