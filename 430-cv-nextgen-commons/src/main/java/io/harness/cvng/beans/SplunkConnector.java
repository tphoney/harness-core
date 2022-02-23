/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.delegate.beans.connector.cvng.Connector;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Map;

@Value
@Builder
public class SplunkConnector implements Connector {
  String accountId;
  String baseUrl;
  String username;
  String password;

  @Override
  @JsonIgnore
  public Map<String, String> collectionHeaders() {
    return Collections.emptyMap();
  }

  @Override
  @JsonIgnore
  public Map<String, String> collectionParams() {
    return Collections.emptyMap();
  }
}
