/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.environment.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.ng.core.environment.beans.EnvironmentType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(PIPELINE)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "EnvironmentRequest", description = "This is the EnvironmentRequest entity defined in Harness")
public class EnvironmentRequestDTO {
  String orgIdentifier;
  String projectIdentifier;
  @EntityIdentifier String identifier;
  Map<String, String> tags;
  @EntityName String name;
  String description;
  String color;
  @ApiModelProperty(required = true) EnvironmentType type;
}
