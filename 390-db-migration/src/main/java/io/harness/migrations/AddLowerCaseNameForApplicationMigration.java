/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.mongo.MongoUtils.setUnset;

import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class AddLowerCaseNameForApplicationMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (
        HIterator<Application> appIterator = new HIterator<>(wingsPersistence.createQuery(Application.class).fetch())) {
      while (appIterator.hasNext()) {
        Application application = appIterator.next();
        String LowerCasedApplicationName = application.getName().toLowerCase();
        if (application.getLowerCaseName() == null) {
          UpdateOperations<Application> operations = wingsPersistence.createUpdateOperations(Application.class);
          setUnset(operations, "lowerCaseName", LowerCasedApplicationName);
          wingsPersistence.update(application, operations);
          log.info("Updated application {}", application.getName());
        }
      }
    } catch (Exception e) {
      log.error("Could not run migration for lower casing application name", e);
    }
  }
}
