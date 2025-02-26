/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.fullsync.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "FullSyncEntityInfo", description = "This contains full sync details of a Git Sync Entity")
@OwnedBy(PL)
public class GitFullSyncEntityInfoDTO {
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) String accountIdentifier;
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) String orgIdentifier;
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier;
  @Schema(description = GitSyncApiConstants.FILEPATH_PARAM_MESSAGE) String filePath;
  @Schema(description = GitSyncApiConstants.ENTITY_TYPE_PARAM_MESSAGE) EntityType entityType;
  @Schema(description = GitSyncApiConstants.SYNC_STATUS_PARAM_MESSAGE) GitFullSyncEntityInfo.SyncStatus syncStatus;
  @Schema(description = "Name of the Entity") String name;
  @Schema(description = "Identifier of the Entity") String identifier;
  @Schema(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE) String branch;
  @Schema(description = GitSyncApiConstants.REPO_NAME_PARAM_MESSAGE) String repoName;
  @Schema(description = GitSyncApiConstants.REPO_URL_PARAM_MESSAGE) String repoUrl;
  @Schema(description = GitSyncApiConstants.FOLDER_PATH_PARAM_MESSAGE) String rootFolder;
  @Schema(description = "This is the number of Full Sync retry attempts") long retryCount;
  @Schema(description = "Contains the error while syncing the entity") String errorMessage;
}
