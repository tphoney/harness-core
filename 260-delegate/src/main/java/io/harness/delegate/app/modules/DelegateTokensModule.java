/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.app.modules;

import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.delegate.task.citasks.cik8handler.helper.DelegateServiceTokenHelper;
import io.harness.security.ServiceTokenGenerator;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
public class DelegateTokensModule extends AbstractModule {
  private final DelegateConfiguration configuration;

  @Override
  protected void configure() {
    String accountSecret = configuration.getAccountSecret();
    if (StringUtils.isEmpty(accountSecret)) {
      accountSecret = configuration.getDelegateToken();
    }
    bind(DelegateServiceTokenHelper.class)
        .toInstance(DelegateServiceTokenHelper.builder()
                        .serviceTokenGenerator(new ServiceTokenGenerator())
                        .accountSecret(accountSecret)
                        .build());
  }
}
