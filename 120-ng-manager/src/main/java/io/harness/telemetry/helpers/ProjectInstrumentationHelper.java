/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.entities.Project;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class ProjectInstrumentationHelper {
  @Inject TelemetryReporter telemetryReporter;
  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
  String PROJECT_ID = "project_id";
  String PROJECT_CREATION_TIME = "project_creation_time";
  String PROJECT_ORG = "project_org";
  String PROJECT_NAME = "project_name";
  String PROJECT_VERSION = "project_version";
  String ACCOUNT_ID = "account_id";
  String PROJECT_COLOR = "project_color";

  public void sendProjectCreationFinishedEvent(Project project, String accountId) {
    log.info("Platform SendProjectCreationFinishedEvent execution started.");
    try {
      if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(ACCOUNT_ID, project.getAccountIdentifier());
        map.put(PROJECT_ID, project.getIdentifier());
        map.put(PROJECT_CREATION_TIME, project.getCreatedAt());
        map.put(PROJECT_ORG, project.getOrgIdentifier());
        map.put(PROJECT_NAME, project.getName());
        map.put(PROJECT_VERSION, project.getVersion());
        map.put(PROJECT_COLOR, project.getColor());
        telemetryReporter.sendTrackEvent("Project Creation Finished", map,
            ImmutableMap.<Destination, Boolean>builder()
                .put(Destination.AMPLITUDE, true)
                .put(Destination.ALL, false)
                .build(),
            Category.COMMUNITY);
        log.info("Project Creation Finished event sent!");
      } else {
        log.info("There is no Account found!. Can not send Project Creation Finished event.");
      }
    } catch (Exception e) {
      log.error("Platform SendProjectCreationFinishedEvent execution failed.", e);
    } finally {
      log.info("Platform SendProjectCreationFinishedEvent execution finished.");
    }
  }
  public void sendProjectDeletionEvent(Project project, String accountId) {
    log.info("Platform SendProjectDeletionEvent execution started.");
    try {
      if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
        HashMap<String, Object> map = new HashMap<>();
        map.put(ACCOUNT_ID, project.getAccountIdentifier());
        map.put(PROJECT_ID, project.getIdentifier());
        map.put(PROJECT_CREATION_TIME, project.getCreatedAt());
        map.put(PROJECT_ORG, project.getOrgIdentifier());
        map.put(PROJECT_NAME, project.getName());
        map.put(PROJECT_VERSION, project.getVersion());
        map.put(PROJECT_COLOR, project.getColor());
        telemetryReporter.sendTrackEvent("Project Deletion", map,
            ImmutableMap.<Destination, Boolean>builder()
                .put(Destination.AMPLITUDE, true)
                .put(Destination.ALL, false)
                .build(),
            Category.COMMUNITY);
        log.info("Project deletion event sent!");
      } else {
        log.info("There is no Account found!. Can not send Project Deletion event.");
      }
    } catch (Exception e) {
      log.error("Platform SendProjectDeletionEvent execution failed.", e);
    } finally {
      log.info("Platform SendProjectDeletionEvent execution finished.");
    }
  }
}
