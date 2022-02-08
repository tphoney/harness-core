/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service;

import io.harness.pms.Dashboard.StatusAndTime;
import io.harness.timescaledb.Tables;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class PipelineDashboardQueryService {
  @Inject private DSLContext dsl;

  private String CD_TableName = "pipeline_execution_summary_cd";

  public List<StatusAndTime> getPipelineExecutionStatusAndTime(String accountId, String orgId, String projectId,
      String pipelineId, long startInterval, long endInterval, String tableName) {
    List<StatusAndTime> statusAndTime;
    Table<?> from;
    List<Condition> conditions = new ArrayList<>();
    SelectField<?>[] select;
    if (tableName == CD_TableName) {
      select =
          new SelectField[] {Tables.PIPELINE_EXECUTION_SUMMARY_CD.STATUS, Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS};
      from = Tables.PIPELINE_EXECUTION_SUMMARY_CD;
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq(orgId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PIPELINEIDENTIFIER.eq(pipelineId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(projectId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.ge(startInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lt(endInterval));
    } else {
      select =
          new SelectField[] {Tables.PIPELINE_EXECUTION_SUMMARY_CI.STATUS, Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS};
      from = Tables.PIPELINE_EXECUTION_SUMMARY_CI;
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ORGIDENTIFIER.eq(orgId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.PIPELINEIDENTIFIER.eq(pipelineId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.PROJECTIDENTIFIER.eq(projectId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ACCOUNTID.eq(accountId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS.ge(startInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS.lt(endInterval));
    }
    statusAndTime = this.dsl.select(select).from(from).where(conditions).fetch().into(StatusAndTime.class);
    return statusAndTime;
  }

  public long getPipelineExecutionMeanDuration(String accountId, String orgId, String projectId, String pipelineId,
      long startInterval, long endInterval, String tableName) {
    @NotNull List<Long> mean;
    Table<?> from;
    List<Condition> conditions = new ArrayList<>();
    SelectField<?>[] select;
    if (tableName == CD_TableName) {
      select = new SelectField[] {
          DSL.avg(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ENDTS.sub(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS))
              .div(1000)};
      from = Tables.PIPELINE_EXECUTION_SUMMARY_CD;
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq(orgId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PIPELINEIDENTIFIER.eq(pipelineId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(projectId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.ge(startInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lt(endInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ENDTS.isNotNull());
    } else {
      select = new SelectField[] {
          DSL.avg(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ENDTS.sub(Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS))
              .div(1000)};
      from = Tables.PIPELINE_EXECUTION_SUMMARY_CI;
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ORGIDENTIFIER.eq(orgId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.PIPELINEIDENTIFIER.eq(pipelineId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.PROJECTIDENTIFIER.eq(projectId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ACCOUNTID.eq(accountId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS.ge(startInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS.lt(endInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ENDTS.isNotNull());
    }
    mean = this.dsl.select(select).from(from).where(conditions).fetch().into(long.class);
    return mean.get(0);
  }

  public long getPipelineExecutionMedianDuration(String accountId, String orgId, String projectId, String pipelineId,
      long startInterval, long endInterval, String tableName) {
    @NotNull List<Long> median;
    Table<?> from;
    List<Condition> conditions = new ArrayList<>();
    SelectField<?>[] select;
    if (tableName == CD_TableName) {
      select = new SelectField[] {DSL.percentileDisc(0.5).withinGroupOrderBy(
          (Tables.PIPELINE_EXECUTION_SUMMARY_CD.ENDTS.sub(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS)).div(1000))};
      from = Tables.PIPELINE_EXECUTION_SUMMARY_CD;
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq(orgId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PIPELINEIDENTIFIER.eq(pipelineId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(projectId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.ge(startInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lt(endInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ENDTS.isNotNull());
    } else {
      select = new SelectField[] {DSL.percentileDisc(0.5).withinGroupOrderBy(
          (Tables.PIPELINE_EXECUTION_SUMMARY_CI.ENDTS.sub(Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS)).div(1000))};
      from = Tables.PIPELINE_EXECUTION_SUMMARY_CI;
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ORGIDENTIFIER.eq(orgId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.PIPELINEIDENTIFIER.eq(pipelineId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.PROJECTIDENTIFIER.eq(projectId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ACCOUNTID.eq(accountId));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS.ge(startInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.STARTTS.lt(endInterval));
      conditions.add(Tables.PIPELINE_EXECUTION_SUMMARY_CI.ENDTS.isNotNull());
    }
    median = this.dsl.select(select).from(from).where(conditions).fetch().into(long.class);
    return median.get(0);
  }
}
