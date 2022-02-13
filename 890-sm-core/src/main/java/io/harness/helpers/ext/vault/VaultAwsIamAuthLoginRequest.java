/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@OwnedBy(PL)
public class VaultAwsIamAuthLoginRequest {
  @JsonProperty("role") private String roleId;
  @JsonProperty("iam_http_request_method") private String iamHttpRequestMethod;
  @JsonProperty("iam_request_url") private String iamRequestUrl;
  @JsonProperty("iam_request_body") private String iamRequestBody;
  @JsonProperty("iam_request_headers") private String iamRequestHeaders;
}
