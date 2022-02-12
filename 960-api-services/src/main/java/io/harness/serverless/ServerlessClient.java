/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serverless;

import org.apache.commons.lang3.StringUtils;

public class ServerlessClient {
  private String serverlessPath;
  private String homeDirectoryPath;

  private ServerlessClient(String serverlessPath, String homeDirectoryPath) {
    this.serverlessPath = serverlessPath;
    this.homeDirectoryPath = homeDirectoryPath;
  }

  public static ServerlessClient client(String serverlessPath, String homeDirectoryPath) {
    return new ServerlessClient(serverlessPath, homeDirectoryPath);
  }

  public VersionCommand version() {
    return new VersionCommand(this);
  }

  public DeployCommand deploy() {
    return new DeployCommand(this);
  }

  public ConfigCredentialCommand configCredential() {
    return new ConfigCredentialCommand(this);
  }

  public String command() {
    StringBuilder command = new StringBuilder(256);
    if (StringUtils.isNotBlank(homeDirectoryPath)) {
      command.append(home(ServerlessUtils.encloseWithQuotesIfNeeded(homeDirectoryPath)));
    }
    if (StringUtils.isNotBlank(serverlessPath)) {
      command.append(ServerlessUtils.encloseWithQuotesIfNeeded(serverlessPath));
    } else {
      command.append("serverless ");
    }
    return command.toString();
  }

  public static String option(Option type, String value) {
    return "--" + type.toString() + " " + value + " ";
  }

  public static String flag(Flag type) {
    return "--" + type.toString() + " ";
  }

  public static String home(String directory) {
    return "HOME=" + directory + " ";
  }
}
