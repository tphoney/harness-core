/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serverless.model;

import lombok.Builder;
import lombok.Data;

@Data
public class ServerlessAwsConfig implements ServerlessConfig {
  private String provider;
  private String accessKey;
  private String secretKey;

  @Builder
  public ServerlessAwsConfig(String provider, String accessKey, String secretKey) {
    this.provider = provider;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }
}
