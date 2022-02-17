/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.accountsetting.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.userprofile.commons.SCMType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.PL)
@Builder
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("GitSyncSettings")
public class GitSyncSettings extends AccountSettingConfig {
  private SCMType scmType;

  @Override
  public AccountSettingConfig getDefaultConfig() {
    return GitSyncSettings.builder().scmType(SCMType.GITHUB).build();
  }
}
