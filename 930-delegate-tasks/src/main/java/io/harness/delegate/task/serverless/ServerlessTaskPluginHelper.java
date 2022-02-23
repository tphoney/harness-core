/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.LogCallback;
import io.harness.serverless.PluginCommand;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.ServerlessCommandTaskHelper;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessResult;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
@Singleton
public class ServerlessTaskPluginHelper {
  @Inject private ServerlessCommandTaskHelper serverlessCommandTaskHelper;

  public void installServerlessPlugin(ServerlessDelegateTaskParams serverlessDelegateTaskParams,
      ServerlessClient serverlessClient, String pluginName, LogCallback executionLogCallback) throws Exception {
    PluginCommand pluginCommand = serverlessClient.plugin().pluginName(pluginName);
    ProcessResult result = serverlessCommandTaskHelper.executeCommand(
        pluginCommand, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true);
    if (result.getExitValue() == 0) {
      // todo: handle success case
    }
    //  todo: // add error handling
  }
}
