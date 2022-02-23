/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

public class PipelineExecutionSummaryConsumer implements ChangeConsumer<PipelineExecutionSummaryEntity> {
  @Override
  public void consumeUpdateEvent(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {}

  @Override
  public void consumeDeleteEvent(String id) {}

  @Override
  public void consumeCreateEvent(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {}
}
