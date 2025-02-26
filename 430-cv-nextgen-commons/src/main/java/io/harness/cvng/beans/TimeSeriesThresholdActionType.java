/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TimeSeriesThresholdActionType {
  IGNORE("ignore"),
  FAIL("fail");
  private String displayName;

  TimeSeriesThresholdActionType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static TimeSeriesThresholdActionType fromDisplayName(String displayName) {
    for (TimeSeriesThresholdActionType timeSeriesThresholdActionType : TimeSeriesThresholdActionType.values()) {
      if (timeSeriesThresholdActionType.displayName.equals(displayName)) {
        return timeSeriesThresholdActionType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }
}
