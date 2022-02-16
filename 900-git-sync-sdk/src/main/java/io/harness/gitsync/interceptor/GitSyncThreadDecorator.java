/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.interceptor.GitSyncConstants.DEFAULT;

import static javax.ws.rs.Priorities.HEADER_DECORATOR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.gitsync.sdk.GitSyncApiConstants;
import io.harness.manage.GlobalContextManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Priority(HEADER_DECORATOR)
@Slf4j
@OwnedBy(DX)
public class GitSyncThreadDecorator implements ContainerRequestFilter, ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
    final String branchName =
        getRequestParamFromContext(GitSyncApiConstants.BRANCH_KEY, pathParameters, queryParameters);
    final String folderPath =
        getRequestParamFromContext(GitSyncApiConstants.FOLDER_PATH, pathParameters, queryParameters);
    final String filePath =
        getRequestParamFromContext(GitSyncApiConstants.FILE_PATH_KEY, pathParameters, queryParameters);
    final String yamlGitConfigId =
        getRequestParamFromContext(GitSyncApiConstants.REPO_IDENTIFIER_KEY, pathParameters, queryParameters);
    final String commitMsg =
        getRequestParamFromContext(GitSyncApiConstants.COMMIT_MSG_KEY, pathParameters, queryParameters);
    final String lastObjectId =
        getRequestParamFromContext(GitSyncApiConstants.LAST_OBJECT_ID_KEY, pathParameters, queryParameters);
    final String isNewBranch =
        getRequestParamFromContext(GitSyncApiConstants.NEW_BRANCH, pathParameters, queryParameters);
    final String findDefaultFromOtherBranches =
        getRequestParamFromContext(GitSyncApiConstants.DEFAULT_FROM_OTHER_REPO, pathParameters, queryParameters);
    final String baseBranch =
        getRequestParamFromContext(GitSyncApiConstants.BASE_BRANCH, pathParameters, queryParameters);
    final String resolvedConflictCommitId =
        getRequestParamFromContext(GitSyncApiConstants.RESOLVED_CONFLICT_COMMIT_ID, pathParameters, queryParameters);
    final GitEntityInfo branchInfo = GitEntityInfo.builder()
                                         .branch(branchName)
                                         .filePath(filePath)
                                         .yamlGitConfigId(yamlGitConfigId)
                                         .commitMsg(commitMsg)
                                         .lastObjectId(lastObjectId)
                                         .folderPath(folderPath)
                                         .isNewBranch(Boolean.valueOf(isNewBranch))
                                         .findDefaultFromOtherRepos(Boolean.valueOf(findDefaultFromOtherBranches))
                                         .baseBranch(baseBranch)
                                         .resolvedConflictCommitId(resolvedConflictCommitId)
                                         .build();
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().gitBranchInfo(branchInfo).build());
  }

  @VisibleForTesting
  String getRequestParamFromContext(
      String key, MultivaluedMap<String, String> pathParameters, MultivaluedMap<String, String> queryParameters) {
    try {
      return URLDecoder.decode(
          queryParameters.getFirst(key) != null ? queryParameters.getFirst(key) : DEFAULT, Charsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      log.error("Error in setting request param for {}", key);
    }
    return DEFAULT;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext)
      throws IOException {
    GlobalContextManager.unset();
  }
}
