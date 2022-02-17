/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans.gitsync;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

import static io.harness.annotations.dev.HarnessTeam.DX;

@Value
@Builder
@OwnedBy(DX)
public class GitFileDetails {
  String filePath;
  String branch;
  String fileContent; // not needed in case of delete.
  String commitMessage;
  String oldFileSha; // not only in case of create file.
  String userEmail;
  String userName;
  String commitId; // current commit of file in Harness, needed in case of bitbucket

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("GitFileDetails{");
    sb.append("filePath='").append(filePath).append('\'');
    sb.append(", branch='").append(branch).append('\'');
    sb.append(", fileContent='").append(fileContent).append('\'');
    sb.append(", commitMessage='").append(commitMessage).append('\'');
    sb.append(", oldFileSha='").append(oldFileSha).append('\'');
    sb.append(", userEmail='").append(userEmail).append('\'');
    sb.append(", userName='").append(userName).append('\'');
    sb.append(", commitId='").append(commitId).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
