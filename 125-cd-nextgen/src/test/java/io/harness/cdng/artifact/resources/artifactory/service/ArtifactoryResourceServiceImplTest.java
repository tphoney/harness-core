/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.artifact.resources.artifactory.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.MLUKIC;
import static io.harness.rule.OwnerRule.TMACARI;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.resources.artifactory.dtos.ArtifactoryArtifactBuildDetailsDTO;
import io.harness.cdng.artifact.resources.artifactory.dtos.ArtifactoryBuildDetailsDTO;
import io.harness.cdng.artifact.resources.artifactory.dtos.ArtifactoryRepoDetailsDTO;
import io.harness.cdng.artifact.resources.artifactory.dtos.ArtifactoryRequestDTO;
import io.harness.cdng.artifact.resources.artifactory.dtos.ArtifactoryResponseDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.artifactory.ArtifactoryFetchBuildsResponse;
import io.harness.delegate.beans.artifactory.ArtifactoryFetchRepositoriesResponse;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.artifactory.ArtifactoryArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.exception.ArtifactoryServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.RepositoryFormat;

import io.fabric8.utils.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(CDP)
public class ArtifactoryResourceServiceImplTest extends CategoryTest {
  private static String ACCOUNT_ID = "accountId";
  private static String REPO_NAME = "repoName";
  private static String IMAGE_PATH = "imagePath";
  private static String ORG_IDENTIFIER = "orgIdentifier";
  private static String PROJECT_IDENTIFIER = "projectIdentifier";

  @Mock private ConnectorService connectorService;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Mock private SecretManagerClientService secretManagerClientService;

  @Spy @InjectMocks private ArtifactoryResourceServiceImpl artifactoryResourceService;

  private final IdentifierRef connectorRef = IdentifierRef.builder().identifier("aws").build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void tesGetRepositoriesExceptionThrownWhenNoConnectorFound() {
    doReturn(Optional.empty()).when(connectorService).get(any(), any(), any(), any());
    assertThatThrownBy(() -> artifactoryResourceService.getRepositories("any", connectorRef, "orgId", "projectId"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Connector not found for identifier");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetRepositories() {
    ConnectorResponseDTO connectorResponseDTO = getConnectorResponseDTO();
    doReturn(ArtifactoryFetchRepositoriesResponse.builder()
                 .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                 .repositories(Collections.singletonMap("repo", "repo"))
                 .build())
        .when(delegateGrpcClientWrapper)
        .executeSyncTask(any());
    doReturn(Collections.singletonList(EncryptedDataDetail.builder().build()))
        .when(secretManagerClientService)
        .getEncryptionDetails(any(), any());
    doReturn(Optional.of(connectorResponseDTO)).when(connectorService).get(any(), any(), any(), any());

    ArtifactoryRepoDetailsDTO artifactoryRepoDetailsDTO =
        artifactoryResourceService.getRepositories("any", connectorRef, "orgId", "projectId");

    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
    verify(secretManagerClientService, times(1)).getEncryptionDetails(any(), any());
    assertThat(artifactoryRepoDetailsDTO.getRepositories().keySet().size()).isEqualTo(1);
    assertThat(artifactoryRepoDetailsDTO.getRepositories().get("repo")).isEqualTo("repo");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetRepositoriesErrorNotifyResponseDataReturned() {
    ConnectorResponseDTO connectorResponseDTO = getConnectorResponseDTO();
    doReturn(ErrorNotifyResponseData.builder().errorMessage("error message").build())
        .when(delegateGrpcClientWrapper)
        .executeSyncTask(any());
    doReturn(Collections.singletonList(EncryptedDataDetail.builder().build()))
        .when(secretManagerClientService)
        .getEncryptionDetails(any(), any());
    doReturn(Optional.of(connectorResponseDTO)).when(connectorService).get(any(), any(), any(), any());

    assertThatThrownBy(() -> artifactoryResourceService.getRepositories("any", connectorRef, "orgId", "projectId"))
        .isInstanceOf(ArtifactoryServerException.class)
        .hasMessageContaining("Failed to fetch repositories - error message");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetRepositoriesFailedArtifactoryFetchResponse() {
    ConnectorResponseDTO connectorResponseDTO = getConnectorResponseDTO();
    doReturn(
        ArtifactoryFetchRepositoriesResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build())
        .when(delegateGrpcClientWrapper)
        .executeSyncTask(any());
    doReturn(Collections.singletonList(EncryptedDataDetail.builder().build()))
        .when(secretManagerClientService)
        .getEncryptionDetails(any(), any());
    doReturn(Optional.of(connectorResponseDTO)).when(connectorService).get(any(), any(), any(), any());

    assertThatThrownBy(() -> artifactoryResourceService.getRepositories("any", connectorRef, "orgId", "projectId"))
        .isInstanceOf(ArtifactoryServerException.class)
        .hasMessageContaining("Failed to fetch repositories");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void tesGetBuildDetailsExceptionThrownWhenNoConnectorFound() {
    doReturn(Optional.empty()).when(connectorService).get(any(), any(), any(), any());
    assertThatThrownBy(()
                           -> artifactoryResourceService.getBuildDetails(
                               "repoName", "filepath", 10, connectorRef, "orgId", "projectId"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Connector not found for identifier");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetBuildDetails() {
    List<BuildDetails> buildDetails =
        Collections.singletonList(aBuildDetails().withArtifactPath("artifactPath").build());
    ConnectorResponseDTO connectorResponseDTO = getConnectorResponseDTO();
    doReturn(ArtifactoryFetchBuildsResponse.builder()
                 .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                 .buildDetails(buildDetails)
                 .build())
        .when(delegateGrpcClientWrapper)
        .executeSyncTask(any());
    doReturn(Collections.singletonList(EncryptedDataDetail.builder().build()))
        .when(secretManagerClientService)
        .getEncryptionDetails(any(), any());
    doReturn(Optional.of(connectorResponseDTO)).when(connectorService).get(any(), any(), any(), any());

    List<ArtifactoryArtifactBuildDetailsDTO> response =
        artifactoryResourceService.getBuildDetails("repoName", "filepath", 10, connectorRef, "orgId", "projectId");

    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
    verify(secretManagerClientService, times(1)).getEncryptionDetails(any(), any());
    assertThat(response.get(0).getArtifactPath()).isEqualTo("artifactPath");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetBuildDetailsErrorNotifyResponseDataReturned() {
    ConnectorResponseDTO connectorResponseDTO = getConnectorResponseDTO();
    doReturn(ErrorNotifyResponseData.builder().errorMessage("error message").build())
        .when(delegateGrpcClientWrapper)
        .executeSyncTask(any());
    doReturn(Collections.singletonList(EncryptedDataDetail.builder().build()))
        .when(secretManagerClientService)
        .getEncryptionDetails(any(), any());
    doReturn(Optional.of(connectorResponseDTO)).when(connectorService).get(any(), any(), any(), any());

    assertThatThrownBy(()
                           -> artifactoryResourceService.getBuildDetails(
                               "repoName", "filepath", 10, connectorRef, "orgId", "projectId"))
        .isInstanceOf(ArtifactoryServerException.class)
        .hasMessageContaining("Failed to fetch artifacts - error message");
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
    verify(secretManagerClientService, times(1)).getEncryptionDetails(any(), any());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetBuildDetailsFailureArtifactoryFetchBuildsResponse() {
    ConnectorResponseDTO connectorResponseDTO = getConnectorResponseDTO();
    doReturn(ArtifactoryFetchBuildsResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build())
        .when(delegateGrpcClientWrapper)
        .executeSyncTask(any());
    doReturn(Collections.singletonList(EncryptedDataDetail.builder().build()))
        .when(secretManagerClientService)
        .getEncryptionDetails(any(), any());
    doReturn(Optional.of(connectorResponseDTO)).when(connectorService).get(any(), any(), any(), any());

    assertThatThrownBy(()
                           -> artifactoryResourceService.getBuildDetails(
                               "repoName", "filepath", 10, connectorRef, "orgId", "projectId"))
        .isInstanceOf(ArtifactoryServerException.class)
        .hasMessageContaining("Failed to fetch artifacts");
    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
    verify(secretManagerClientService, times(1)).getEncryptionDetails(any(), any());
  }

  private ConnectorResponseDTO getConnectorResponseDTO() {
    return ConnectorResponseDTO.builder()
        .connector(ConnectorInfoDTO.builder()
                       .connectorType(ConnectorType.ARTIFACTORY)
                       .connectorConfig(ArtifactoryConnectorDTO.builder()
                                            .auth(ArtifactoryAuthenticationDTO.builder()
                                                      .credentials(ArtifactoryUsernamePasswordAuthDTO.builder().build())
                                                      .build())
                                            .build())
                       .build())
        .build();
  }

  private ConnectorResponseDTO getConnector() {
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                            .connectorType(ConnectorType.ARTIFACTORY)
                                            .connectorConfig(ArtifactoryConnectorDTO.builder()
                                                                 .delegateSelectors(Collections.emptySet())
                                                                 .auth(ArtifactoryAuthenticationDTO.builder().build())
                                                                 .build())
                                            .orgIdentifier("dummyOrg")
                                            .projectIdentifier("dummyProject")
                                            .build();
    return ConnectorResponseDTO.builder().connector(connectorInfoDTO).build();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetBuildDetails2() {
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();
    ConnectorResponseDTO connectorResponse = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier"))
        .thenReturn(Optional.of(connectorResponse));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            ArtifactTaskResponse.builder()
                .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                .artifactTaskExecutionResponse(
                    ArtifactTaskExecutionResponse.builder().artifactDelegateResponses(new ArrayList<>()).build())
                .build());

    ArtifactoryResponseDTO artifactoryResponseDTO = artifactoryResourceService.getBuildDetails(
        identifierRef, REPO_NAME, IMAGE_PATH, RepositoryFormat.docker.name(), null, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(artifactoryResponseDTO).isNotNull();

    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier");
    verify(delegateGrpcClientWrapper).executeSyncTask(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    ArtifactTaskParameters artifactTaskParameters = (ArtifactTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(artifactTaskParameters.getArtifactTaskType()).isEqualTo(ArtifactTaskType.GET_BUILDS);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetSuccessfulBuild() {
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();
    ArtifactoryRequestDTO artifactoryRequestDTO = ArtifactoryRequestDTO.builder().build();
    ConnectorResponseDTO connectorDTO = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier"))
        .thenReturn(Optional.of(connectorDTO));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            ArtifactTaskResponse.builder()
                .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                .artifactTaskExecutionResponse(ArtifactTaskExecutionResponse.builder()
                                                   .artifactDelegateResponses(Lists.newArrayList(
                                                       ArtifactoryArtifactDelegateResponse.builder()
                                                           .buildDetails(ArtifactBuildDetailsNG.builder().build())
                                                           .build()))
                                                   .build())
                .build());

    ArtifactoryBuildDetailsDTO dockerBuildDetailsDTO =
        artifactoryResourceService.getSuccessfulBuild(identifierRef, REPO_NAME, IMAGE_PATH,
            RepositoryFormat.docker.name(), null, artifactoryRequestDTO, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(dockerBuildDetailsDTO).isNotNull();

    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier");
    verify(delegateGrpcClientWrapper).executeSyncTask(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    ArtifactTaskParameters artifactTaskParameters = (ArtifactTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(artifactTaskParameters.getArtifactTaskType()).isEqualTo(ArtifactTaskType.GET_LAST_SUCCESSFUL_BUILD);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testValidateArtifactServer() {
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(ACCOUNT_ID)
                                      .identifier("identifier")
                                      .projectIdentifier(PROJECT_IDENTIFIER)
                                      .orgIdentifier(ORG_IDENTIFIER)
                                      .build();
    ConnectorResponseDTO connectorDTO = getConnector();
    when(connectorService.get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier"))
        .thenReturn(Optional.of(connectorDTO));
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManagerClientService.getEncryptionDetails(any(), any()))
        .thenReturn(Lists.newArrayList(encryptedDataDetail));
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            ArtifactTaskResponse.builder()
                .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                .artifactTaskExecutionResponse(ArtifactTaskExecutionResponse.builder()
                                                   .artifactDelegateResponses(Lists.newArrayList(
                                                       ArtifactoryArtifactDelegateResponse.builder()
                                                           .buildDetails(ArtifactBuildDetailsNG.builder().build())
                                                           .build()))
                                                   .build())
                .build());

    boolean response =
        artifactoryResourceService.validateArtifactServer(identifierRef, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    assertThat(response).isFalse();

    ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestCaptor = ArgumentCaptor.forClass(DelegateTaskRequest.class);
    verify(connectorService).get(ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "identifier");
    verify(delegateGrpcClientWrapper).executeSyncTask(delegateTaskRequestCaptor.capture());
    DelegateTaskRequest delegateTaskRequest = delegateTaskRequestCaptor.getValue();
    ArtifactTaskParameters artifactTaskParameters = (ArtifactTaskParameters) delegateTaskRequest.getTaskParameters();
    assertThat(artifactTaskParameters.getArtifactTaskType()).isEqualTo(ArtifactTaskType.VALIDATE_ARTIFACT_SERVER);
  }
}
