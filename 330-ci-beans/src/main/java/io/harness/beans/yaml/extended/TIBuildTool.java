/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.yaml.extended;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(HarnessTeam.CI)
public enum TIBuildTool {
  @JsonProperty("Maven") MAVEN("Maven"),
  @JsonProperty("Bazel") BAZEL("Bazel"),
  @JsonProperty("Gradle") GRADLE("Gradle"),
  @JsonProperty("Dotnet") DOTNET("Dotnet"),
  @JsonProperty("Nunitconsole") NUNITCONSOLE("Nunitconsole");

  private final String yamlName;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public static TIBuildTool getBuildTool(@JsonProperty("buildTool") String yamlName) {
    for (TIBuildTool buildTool : TIBuildTool.values()) {
      if (buildTool.yamlName.equalsIgnoreCase(yamlName)) {
        return buildTool;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlName);
  }

  TIBuildTool(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }

  @Override
  public String toString() {
    return yamlName;
  }

  public static TIBuildTool fromString(final String s) {
    return TIBuildTool.getBuildTool(s);
  }
}
