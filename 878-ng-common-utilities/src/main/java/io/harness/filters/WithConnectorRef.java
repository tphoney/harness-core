/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.filters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface WithConnectorRef {
  /**
   *
   * @return a map of relative fqn from step to the connector ref parameter field value
   */
  Map<String, ParameterField<String>> extractConnectorRefs();
}
