/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.scalyr;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ScalyrConfig;
import software.wings.delegatetasks.cv.beans.CustomLogResponseMapper;
import software.wings.service.intfc.scalyr.ScalyrService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ScalyrServiceImpl implements ScalyrService {
  @Override
  public Map<String, Map<String, CustomLogResponseMapper>> createLogCollectionMapping(
      String hostnameField, String messageField, String timestampField) {
    Map<String, Map<String, CustomLogResponseMapper>> logCollectionMapping = new HashMap<>();
    Map<String, CustomLogResponseMapper> responseMap = new HashMap<>();
    responseMap.put("host",
        CustomLogResponseMapper.builder().fieldName("host").jsonPath(Collections.singletonList(hostnameField)).build());
    responseMap.put("timestamp",
        CustomLogResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Collections.singletonList(timestampField))
            .build());
    responseMap.put("logMessage",
        CustomLogResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Collections.singletonList(messageField))
            .build());
    logCollectionMapping.put(ScalyrConfig.QUERY_URL, responseMap);

    return logCollectionMapping;
  }
}
