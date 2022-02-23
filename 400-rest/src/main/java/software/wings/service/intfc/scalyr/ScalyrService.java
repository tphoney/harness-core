/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.scalyr;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.delegatetasks.cv.beans.CustomLogResponseMapper;

import java.util.Map;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface ScalyrService {
  Map<String, Map<String, CustomLogResponseMapper>> createLogCollectionMapping(
      String hostnameField, String messageField, String timestampField);
}
