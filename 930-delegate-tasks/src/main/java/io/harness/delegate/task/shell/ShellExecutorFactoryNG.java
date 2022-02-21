/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.logging.LogCallback;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.ShellExecutorConfig;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class ShellExecutorFactoryNG {
  public ScriptProcessExecutor getExecutor(ShellExecutorConfig shellExecutorConfig,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    return new ScriptProcessExecutor(
        getExecutionLogCallback(shellExecutorConfig, logStreamingTaskClient, commandUnitsProgress), true,
        shellExecutorConfig);
  }

  public FileBasedScriptExecutorNG getFileBasedExecutor(ShellExecutorConfig shellExecutorConfig,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    return new FileBasedProcessScriptExecutorNG(
        getExecutionLogCallback(shellExecutorConfig, logStreamingTaskClient, commandUnitsProgress), true);
  }

  private static LogCallback getExecutionLogCallback(ShellExecutorConfig shellExecutorConfig,
      ILogStreamingTaskClient logStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(
        logStreamingTaskClient, shellExecutorConfig.getCommandUnitName(), true, commandUnitsProgress);
  }
}
