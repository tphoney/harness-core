/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class PmsExecutionSummaryReadHelper {
  @Inject @Named("secondary-mongo") public MongoTemplate secondaryMongoTemplate;

  public long findCount(Query query) {
    return secondaryMongoTemplate.count(Query.of(query).limit(-1).skip(-1), PipelineExecutionSummaryEntity.class);
  }

  public List<PipelineExecutionSummaryEntity> find(Query query) {
    return secondaryMongoTemplate.find(query, PipelineExecutionSummaryEntity.class);
  }
}
