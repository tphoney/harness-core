/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import io.harness.exception.InvalidArgumentsException;
import io.harness.pms.yaml.ParameterField;

import software.wings.ngmigration.NGYamlFile;

import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class MigratorUtility {
  public static String generateIdentifier(String name) {
    return name.trim().toLowerCase().replaceAll("[^A-Za-z0-9]", "");
  }

  public static ParameterField<String> getParameterField(String value) {
    if (StringUtils.isBlank(value)) {
      return ParameterField.createValueField("");
    }
    return ParameterField.createValueField(value);
  }

  public static void sort(List<NGYamlFile> files) {
    files.sort(Comparator.comparingInt(MigratorUtility::toInt));
  }

  // This is for sorting entities while creating
  private static int toInt(NGYamlFile file) {
    switch (file.getType()) {
      case SECRET_MANAGER:
        return 1;
      case SECRET:
        return 5;
      case CONNECTOR:
        return 10;
      case SERVICE:
        return 20;
      case ENVIRONMENT:
        return 25;
      case PIPELINE:
        return 50;
      default:
        throw new InvalidArgumentsException("Unknown type found: " + file.getType());
    }
  }
}
