/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import io.harness.delegate.configuration.InstallUtils;
import io.harness.serverless.ServerlessGlobalConfigService;

public class ServerlessGlobalConfigServiceImpl implements ServerlessGlobalConfigService {
  @Override
  public String getServerlessClientPath() {
    // todo: need to change with actual path
    return null;
    //        return InstallUtils.getServerlessClientPath();
  }
}
