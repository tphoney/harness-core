/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.accountsetting.services;

import io.harness.ng.core.accountsetting.dto.AccountSettingResponseDTO;
import io.harness.ng.core.accountsetting.dto.AccountSettingType;
import io.harness.ng.core.accountsetting.dto.AccountSettingsDTO;

import java.util.List;

public interface NGAccountSettingService {
  AccountSettingResponseDTO update(AccountSettingsDTO accountSettings, String accountIdentifier);

  AccountSettingResponseDTO create(AccountSettingsDTO accountSettingsDTO, String accountIdentifier);

  List<AccountSettingsDTO> list(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, AccountSettingType type);

  AccountSettingResponseDTO get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, AccountSettingType type);

  void setUpDefaultAccountSettings(String accountIdentifier);

  boolean getIsBuiltInSMDisabled(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, AccountSettingType type);
}
