/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.app.modules;

import static io.harness.grpc.utils.DelegateGrpcConfigExtractor.extractAuthority;
import static io.harness.grpc.utils.DelegateGrpcConfigExtractor.extractScheme;
import static io.harness.grpc.utils.DelegateGrpcConfigExtractor.extractTarget;

import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.grpc.client.AbstractManagerGrpcClientModule;
import io.harness.grpc.client.ManagerGrpcClientModule;

import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DelegateGrpcClientModule extends AbstractManagerGrpcClientModule {
  private final DelegateConfiguration configuration;

  @Override
  public ManagerGrpcClientModule.Config config() {
    return ManagerGrpcClientModule.Config.builder()
        .target(Optional.ofNullable(configuration.getManagerTarget())
                    .orElseGet(() -> extractTarget(configuration.getManagerUrl())))
        .authority(Optional.ofNullable(configuration.getManagerAuthority())
                       .orElseGet(() -> extractAuthority(configuration.getManagerUrl(), "manager")))
        .scheme(extractScheme(configuration.getManagerUrl()))
        .accountId(configuration.getAccountId())
        .accountSecret(configuration.getAccountSecret())
        .build();
  }

  @Override
  public String application() {
    return "Delegate";
  }
}
