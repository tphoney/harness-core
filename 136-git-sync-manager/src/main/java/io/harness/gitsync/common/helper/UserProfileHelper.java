/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.UserPrincipal;
import io.harness.ng.userprofile.commons.GithubSCMDTO;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.services.api.SourceCodeManagerService;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Singleton
@OwnedBy(DX)
@Slf4j
public class UserProfileHelper {
  private final SourceCodeManagerService sourceCodeManagerService;
  private final String ERROR_MSG_USER_PRINCIPAL_NOT_SET = "User not set for push event.";

  @Inject
  public UserProfileHelper(SourceCodeManagerService sourceCodeManagerService) {
    this.sourceCodeManagerService = sourceCodeManagerService;
  }

  public void setConnectorDetailsFromUserProfile(
      YamlGitConfigDTO yamlGitConfig, UserPrincipal userPrincipal, ConnectorResponseDTO connector) {
    if (connector.getConnector().getConnectorType() != ConnectorType.GITHUB) {
      throw new InvalidRequestException("Git Sync only supported for github connector");
    }
    final GithubConnectorDTO githubConnectorDTO = (GithubConnectorDTO) connector.getConnector().getConnectorConfig();
    githubConnectorDTO.setUrl(yamlGitConfig.getRepo());
    final GithubSCMDTO githubUserProfile =
        (GithubSCMDTO) getScmUserProfile(yamlGitConfig.getAccountIdentifier(), userPrincipal, SCMType.GITHUB);
    final SecretRefData tokenRef;
    try {
      tokenRef =
          ((GithubUsernameTokenDTO) ((GithubHttpCredentialsDTO) githubUserProfile.getAuthentication().getCredentials())
                  .getHttpCredentialsSpec())
              .getTokenRef();
    } catch (Exception e) {
      throw new InvalidRequestException(
          "User Profile should contain github username token credentials for git sync", e);
    }
    githubConnectorDTO.setApiAccess(GithubApiAccessDTO.builder()
                                        .type(GithubApiAccessType.TOKEN)
                                        .spec(GithubTokenSpecDTO.builder().tokenRef(tokenRef).build())
                                        .build());
  }

  public UserPrincipal getUserPrincipal() {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      io.harness.security.dto.UserPrincipal userPrincipal =
          (io.harness.security.dto.UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
      return UserPrincipalMapper.toProto(userPrincipal);
    }
    log.error(ERROR_MSG_USER_PRINCIPAL_NOT_SET);
    throw new InvalidRequestException(ERROR_MSG_USER_PRINCIPAL_NOT_SET);
  }

  public String getScmUserName(String accountIdentifier) {
    final GithubSCMDTO githubUserProfile =
        (GithubSCMDTO) getScmUserProfile(accountIdentifier, getUserPrincipal(), SCMType.GITHUB);
    try {
      return (
          (GithubUsernameTokenDTO) ((GithubHttpCredentialsDTO) githubUserProfile.getAuthentication().getCredentials())
              .getHttpCredentialsSpec())
          .getUsername();
    } catch (Exception e) {
      log.error("User Profile should contain github username name for git sync", e);
      throw new InvalidRequestException("User Profile should contain github username name for git sync", e);
    }
  }

  private SourceCodeManagerDTO getScmUserProfile(String accountId, UserPrincipal userPrincipal, SCMType scmType) {
    if (userPrincipal == null) {
      log.error(ERROR_MSG_USER_PRINCIPAL_NOT_SET);
      throw new InvalidRequestException(ERROR_MSG_USER_PRINCIPAL_NOT_SET);
    }
    final List<SourceCodeManagerDTO> sourceCodeManager =
        sourceCodeManagerService.get(userPrincipal.getUserId().getValue(), accountId);
    final Optional<SourceCodeManagerDTO> sourceCodeManagerDTO =
        sourceCodeManager.stream().filter(scm -> scm.getType().equals(scmType)).findFirst();
    if (!sourceCodeManagerDTO.isPresent()) {
      log.error("User profile doesn't contain github scm details");
      throw new InvalidRequestException("User profile doesn't contain github scm details");
    }
    return sourceCodeManagerDTO.get();
  }
}
