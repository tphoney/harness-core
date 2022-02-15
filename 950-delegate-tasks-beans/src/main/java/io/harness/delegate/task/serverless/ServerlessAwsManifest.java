/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServerlessAwsManifest implements ServerlessManifest {
  @Expression(ALLOW_SECRETS) String yamlContent;
  String artifactPathVariable;

  @Override
  public ServerlessManifestType getServerlessManifestType() {
    return ServerlessManifestType.AWS_SERVERLESS_MANIFEST;
  }
}
