/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.service.intfc;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.event.payloads.Lifecycle;

import java.util.List;

public interface InstanceDataBulkWriteService {
  boolean updateLifecycle(List<Lifecycle> lifecycleList);

  boolean upsertInstanceInfo(List<InstanceInfo> instanceInfos);

  // The update events is Unordered to utilize mongo parallel threads
  boolean updateInstanceEvent(List<InstanceEvent> instanceEvents);
}
