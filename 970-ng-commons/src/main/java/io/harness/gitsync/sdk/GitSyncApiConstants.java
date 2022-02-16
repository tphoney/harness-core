/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class GitSyncApiConstants {
  public static final String BRANCH_KEY = "branch";
  public static final String REPO_IDENTIFIER_KEY = "repoIdentifier";
  public static final String FILE_PATH_KEY = "filePath";
  public static final String COMMIT_MSG_KEY = "commitMsg";
  public static final String CREATE_PR_KEY = "createPr";
  public static final String LAST_OBJECT_ID_KEY = "lastObjectId";
  public static final String RESOLVED_CONFLICT_COMMIT_ID = "resolvedConflictCommitId";
  public static final String FOLDER_PATH = "rootFolder";
  public static final String NEW_BRANCH = "isNewBranch";
  public static final String TARGET_BRANCH_FOR_PR = "targetBranchForPr";
  public static final String DEFAULT_FROM_OTHER_REPO = "getDefaultFromOtherRepo";
  public static final String BASE_BRANCH = "baseBranch";
  public static final String PR_TITLE = "prTitle";
  public static final String ENTITY_TYPE = "entityType";
  public static final String SYNC_STATUS = "syncStatus";

  public static final String BRANCH_PARAM_MESSAGE = "Branch Name";
  public static final String FILEPATH_PARAM_MESSAGE = "File Path of the Entity";
  public static final String REPOID_PARAM_MESSAGE = "Git Sync Config Id";
  public static final String REPO_URL_PARAM_MESSAGE = "Repo URL";
  public static final String REPO_NAME_PARAM_MESSAGE = "Repo Name";
  public static final String FOLDER_PATH_PARAM_MESSAGE = "Root Folder Path of the Entity";
  public static final String COMMIT_MESSAGE_PARAM_MESSAGE = "Commit Message";
  public static final String DEFAULT_BRANCH_PARAM_MESSAGE = "Default Branch";
  public static final String ENTITY_TYPE_PARAM_MESSAGE = "Entity Type";
  public static final String SYNC_STATUS_PARAM_MESSAGE = "Sync Status of the Entity";
  public static final String SEARCH_TERM_PARAM_MESSAGE = "Search Term";
}
