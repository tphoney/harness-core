/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.entities;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Operator", description = "List of all possible Operators")
public enum CCMOperator {
  NOT_IN,
  IN,
  EQUALS,
  NOT_NULL,
  NULL,
  LIKE,
  GREATER_THAN,
  LESS_THAN,
  GREATER_THAN_EQUALS_TO,
  LESS_THAN_EQUALS_TO,
  AFTER,
  BEFORE
}
