/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.delegatetasks.cv.beans.appd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppDynamicsTier implements Comparable<AppDynamicsTier> {
  long id;
  String name;

  @Override
  public int compareTo(@NotNull AppDynamicsTier o) {
    return name.compareTo(o.name);
  }
}
