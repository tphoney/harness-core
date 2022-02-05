/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.accountsetting.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TypeAlias("io.harness.ng.core.accountsetting.dto.SecretSettings")
public class SecretSettings extends AccountSettingConfig {
  @Builder
  public SecretSettings(boolean var) {
    this.var = var;
  }
  boolean var;
  @Override
  public AccountSettingConfig getDefaultConfig() {
    return SecretSettings.builder().var(true).build();
  }
}
