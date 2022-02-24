/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.utils.FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cistatus.service.GithubAppConfig;
import io.harness.cistatus.service.GithubService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketUsernameTokenApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubAppSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.exception.InvalidArgumentsException;
import io.harness.git.GitClientHelper;
import io.harness.product.ci.scm.proto.BitbucketCloudProvider;
import io.harness.product.ci.scm.proto.BitbucketServerProvider;
import io.harness.product.ci.scm.proto.GithubProvider;
import io.harness.product.ci.scm.proto.GitlabProvider;
import io.harness.product.ci.scm.proto.Provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@Slf4j
@OwnedBy(DX)
public class ScmGitProviderMapper {
  @Inject(optional = true) GithubService githubService;
  private static final String SCM_SKIP_SSL = "SCM_SKIP_SSL";
  private static final String ADDITIONAL_CERTS_PATH = "ADDITIONAL_CERTS_PATH";

  public Provider mapToSCMGitProvider(ScmConnector scmConnector) {
    return mapToSCMGitProvider(scmConnector, false);
  }

  public Provider mapToSCMGitProvider(ScmConnector scmConnector, boolean debug) {
    if (scmConnector instanceof GithubConnectorDTO) {
      return mapToGithubProvider((GithubConnectorDTO) scmConnector, debug);
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      return mapToGitLabProvider((GitlabConnectorDTO) scmConnector, debug);
    } else if (scmConnector instanceof BitbucketConnectorDTO) {
      return mapToBitbucketProvider((BitbucketConnectorDTO) scmConnector, debug);
    } else {
      throw new NotImplementedException(
          String.format("The scm apis for the provider type %s is not supported", scmConnector.getClass()));
    }
  }

  private Provider mapToBitbucketProvider(BitbucketConnectorDTO bitbucketConnector, boolean debug) {
    boolean skipVerify = checkScmSkipVerify();
    String bitBucketApiURL = GitClientHelper.getBitBucketApiURL(bitbucketConnector.getUrl());
    Provider.Builder builder = Provider.newBuilder().setEndpoint(bitBucketApiURL).setDebug(debug);
    if (GitClientHelper.isBitBucketSAAS(bitbucketConnector.getUrl())) {
      builder.setBitbucketCloud(createBitbucketCloudProvider(bitbucketConnector));
    } else {
      builder.setBitbucketServer(createBitbucketServerProvider(bitbucketConnector));
    }
    return builder.setSkipVerify(skipVerify).setAdditionalCertsPath(getAdditionalCertsPath()).build();
  }

  private boolean checkScmSkipVerify() {
    final String scm_skip_ssl = System.getenv(SCM_SKIP_SSL);
    boolean skipVerify = "true".equals(scm_skip_ssl);
    if (skipVerify) {
      log.info("Skipping verification");
    }
    return skipVerify;
  }

  private String getAdditionalCertsPath() {
    String additionalCertsPath = "";
    try {
      additionalCertsPath = System.getenv(ADDITIONAL_CERTS_PATH);
    } catch (SecurityException e) {
      log.error("Don't have sufficient permission to query ADDITIONAL_CERTS_PATH", e);
    }
    if (additionalCertsPath == null) {
      additionalCertsPath = "";
    }
    return additionalCertsPath;
  }

  private BitbucketCloudProvider createBitbucketCloudProvider(BitbucketConnectorDTO bitbucketConnector) {
    BitbucketApiAccessDTO apiAccess = bitbucketConnector.getApiAccess();
    BitbucketUsernameTokenApiAccessDTO bitbucketUsernameTokenApiAccessDTO =
        (BitbucketUsernameTokenApiAccessDTO) apiAccess.getSpec();
    String username = getSecretAsStringFromPlainTextOrSecretRef(
        bitbucketUsernameTokenApiAccessDTO.getUsername(), bitbucketUsernameTokenApiAccessDTO.getUsernameRef());
    String appPassword = String.valueOf(bitbucketUsernameTokenApiAccessDTO.getTokenRef().getDecryptedValue());

    return BitbucketCloudProvider.newBuilder().setUsername(username).setAppPassword(appPassword).build();
  }

  private BitbucketServerProvider createBitbucketServerProvider(BitbucketConnectorDTO bitbucketConnector) {
    BitbucketApiAccessDTO apiAccess = bitbucketConnector.getApiAccess();
    BitbucketUsernameTokenApiAccessDTO bitbucketUsernameTokenApiAccessDTO =
        (BitbucketUsernameTokenApiAccessDTO) apiAccess.getSpec();
    String username = getSecretAsStringFromPlainTextOrSecretRef(
        bitbucketUsernameTokenApiAccessDTO.getUsername(), bitbucketUsernameTokenApiAccessDTO.getUsernameRef());
    String personalAccessToken = String.valueOf(bitbucketUsernameTokenApiAccessDTO.getTokenRef().getDecryptedValue());

    return BitbucketServerProvider.newBuilder()
        .setUsername(username)
        .setPersonalAccessToken(personalAccessToken)
        .build();
  }

  private Provider mapToGitLabProvider(GitlabConnectorDTO gitlabConnector, boolean debug) {
    boolean skipVerify = checkScmSkipVerify();
    return Provider.newBuilder()
        .setGitlab(createGitLabProvider(gitlabConnector))
        .setDebug(debug)
        .setEndpoint(GitClientHelper.getGitlabApiURL(gitlabConnector.getUrl()))
        .setSkipVerify(skipVerify)
        .setAdditionalCertsPath(getAdditionalCertsPath())
        .build();
  }

  private GitlabProvider createGitLabProvider(GitlabConnectorDTO gitlabConnector) {
    String accessToken = getAccessToken(gitlabConnector);
    return GitlabProvider.newBuilder().setPersonalToken(accessToken).build();
  }

  private String getAccessToken(GitlabConnectorDTO gitlabConnector) {
    GitlabApiAccessDTO apiAccess = gitlabConnector.getApiAccess();
    GitlabTokenSpecDTO apiAccessDTO = (GitlabTokenSpecDTO) apiAccess.getSpec();
    return String.valueOf(apiAccessDTO.getTokenRef().getDecryptedValue());
  }

  private Provider mapToGithubProvider(GithubConnectorDTO githubConnector, boolean debug) {
    boolean skipVerify = checkScmSkipVerify();
    return Provider.newBuilder()
        .setGithub(createGithubProvider(githubConnector))
        .setDebug(debug)
        .setEndpoint(GitClientHelper.getGithubApiURL(githubConnector.getUrl()))
        .setSkipVerify(skipVerify)
        .setAdditionalCertsPath(getAdditionalCertsPath())
        .build();
  }

  private GithubProvider createGithubProvider(GithubConnectorDTO githubConnector) {
    switch (githubConnector.getApiAccess().getType()) {
      case GITHUB_APP:
        // todo @aradisavljevic: switch to scm provider for github app after it is implemented
        String token = getAccessTokenFromGithubApp(githubConnector);
        return GithubProvider.newBuilder().setAccessToken(token).build();
      case TOKEN:
        String accessToken = getAccessToken(githubConnector);
        return GithubProvider.newBuilder().setAccessToken(accessToken).build();
      default:
        throw new NotImplementedException(String.format(
            "The scm apis for the api access type %s is not supported", githubConnector.getApiAccess().getType()));
    }
  }

  private String getAccessTokenFromGithubApp(GithubConnectorDTO githubConnector) {
    GithubApiAccessDTO apiAccess = githubConnector.getApiAccess();
    GithubAppSpecDTO apiAccessDTO = (GithubAppSpecDTO) apiAccess.getSpec();
    if (githubService == null) {
      throw new NotImplementedException("Token for Github App is only supported on delegate");
    }

    try {
      return githubService.getToken(GithubAppConfig.builder()
                                        .appId(apiAccessDTO.getApplicationId())
                                        .installationId(apiAccessDTO.getInstallationId())
                                        .privateKey(String.valueOf(apiAccessDTO.getPrivateKeyRef().getDecryptedValue()))
                                        .githubUrl(GitClientHelper.getGithubApiURL(githubConnector.getUrl()))
                                        .build());
    } catch (Exception ex) {
      throw new InvalidArgumentsException("Failed to generate token for github connector via git hub app ");
    }
  }

  private String getAccessToken(GithubConnectorDTO githubConnector) {
    GithubApiAccessDTO apiAccess = githubConnector.getApiAccess();
    GithubTokenSpecDTO apiAccessDTO = (GithubTokenSpecDTO) apiAccess.getSpec();
    if (apiAccessDTO.getTokenRef() == null || apiAccessDTO.getTokenRef().getDecryptedValue() == null) {
      throw new InvalidArgumentsException("The personal access token is not set");
    }
    return String.valueOf(apiAccessDTO.getTokenRef().getDecryptedValue());
  }
}
