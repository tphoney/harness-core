/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

@OwnedBy(CDC)
public enum ServiceNowFieldTypeNG {
  @JsonProperty("glide_date_time") DATE_TIME(Arrays.asList("glide_date_time", "due_date", "glide_date", "glide_time")),
  @JsonProperty("integer") INTEGER(Collections.singletonList("integer")),
  @JsonProperty("boolean") BOOLEAN(Collections.singletonList("boolean")),
  @JsonProperty("string") STRING(Collections.singletonList("string"));

  @Getter private List<String> snowInternalTypes;
  ServiceNowFieldTypeNG(List<String> types) {
    snowInternalTypes = types;
  }

  public static ServiceNowFieldTypeNG fromTypeString(String typeStr) {
    Optional<ServiceNowFieldTypeNG> serviceNowFieldTypeNG =
        Arrays.stream(ServiceNowFieldTypeNG.values())
            .filter(type -> type.getSnowInternalTypes().contains(typeStr))
            .findFirst();
    if (serviceNowFieldTypeNG.isPresent()) {
      return serviceNowFieldTypeNG.get();
    } else {
      throw new InvalidArgumentsException("Invalid type");
    }
  }
}
