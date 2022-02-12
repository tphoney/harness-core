/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serverless;

import org.apache.commons.lang3.StringUtils;

public class DeployCommand extends AbstractExecutable {
  private ServerlessClient client;
  private String stage;
  private String region;
  private boolean forceDeployment;
  private boolean awsS3Accelerate;
  private boolean noAwsS3Accelerate;
  public DeployCommand(ServerlessClient client) {
    this.client = client;
  }
  public DeployCommand stage(String stage) {
    this.stage = stage;
    return this;
  }
  public DeployCommand region(String region) {
    this.region = region;
    return this;
  }
  public DeployCommand forceDeployment(boolean forceDeployment) {
    this.forceDeployment = forceDeployment;
    return this;
  }
  public DeployCommand awsS3Accelerate(boolean awsS3Accelerate) {
    this.awsS3Accelerate = awsS3Accelerate;
    return this;
  }
  public DeployCommand noAwsS3Accelerate(boolean noAwsS3Accelerate) {
    this.noAwsS3Accelerate = noAwsS3Accelerate;
    return this;
  }

  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("deploy ");
    if (StringUtils.isNotBlank(this.stage)) {
      command.append(ServerlessClient.option(Option.stage, this.stage));
    }
    if (StringUtils.isNotBlank(this.region)) {
      command.append(ServerlessClient.option(Option.region, this.region));
    }
    if (this.forceDeployment) {
      command.append(ServerlessClient.flag(Flag.force));
    }
    if (this.awsS3Accelerate) {
      command.append(ServerlessClient.flag(Flag.awsS3Accelerate));
    } else if (this.noAwsS3Accelerate) {
      command.append(ServerlessClient.flag(Flag.noAwsS3Accelerate));
    }
    return command.toString().trim();
  }
}
