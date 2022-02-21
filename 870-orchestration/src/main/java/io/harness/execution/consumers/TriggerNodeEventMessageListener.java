/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.consumers;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;
import io.harness.execution.TriggerNodeHandler;
import io.harness.pms.contracts.execution.events.TriggerNodeEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class TriggerNodeEventMessageListener extends PmsAbstractMessageListener<TriggerNodeEvent, TriggerNodeHandler> {
  @Inject
  public TriggerNodeEventMessageListener(
      TriggerNodeHandler triggerNodeHandler, @Named("EngineExecutorService") ExecutorService executorService) {
    super(ModuleType.PMS.name(), TriggerNodeEvent.class, triggerNodeHandler, executorService);
  }

  @Override
  public boolean isProcessable(Message message) {
    return true;
  }
}
