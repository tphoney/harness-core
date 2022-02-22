/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.config;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class SchedulerJobsConfig {
  private String budgetAlertsJobCron;
  private String weeklyReportsJobCron;
  private String budgetCostUpdateJobCron;
  private String connectorHealthUpdateJobCron;
  private String awsAccountTagsCollectionJobCron;
}
