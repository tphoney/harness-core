/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.fullsync.dtos;

import io.harness.NGCommonEntityConstants;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(name = "GitFullSyncConfig", description = "This has config details specific to Git full sync with Harness")
public class GitFullSyncConfigDTO {
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) private String accountIdentifier;
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) private String orgIdentifier;
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) private String projectIdentifier;
  @Schema(description = "Name of the branch from which new branch will be fork out") private String baseBranch;
  @Schema(description = "Name of the branch to which Entities was pushed and from which pull request was created")
  private String branch;
  @Schema(description = "Title of the pull request") private String prTitle;
  @Schema(description = "Determines if pull request was created") private boolean createPullRequest;
  @Schema(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE) private String repoIdentifier;
  @Schema(description = "Determines if new branch was created") boolean isNewBranch;
  @Schema(description = "Name of the branch to which pull request was created") String targetBranch;
  @Schema(description = "Path to the root folder in which entities was pushed") String rootFolder;
}
