/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.shell.AbstractScriptExecutor;
import io.harness.shell.FileBasedSshScriptExecutorHelper;
import io.harness.shell.SshSessionConfig;

@OwnedBy(HarnessTeam.CDC)
public class FileBasedSshScriptExecutorNG extends FileBasedAbstractScriptExecutorNG {
  private SshSessionConfig config;

  public FileBasedSshScriptExecutorNG(
      LogCallback logCallback, boolean shouldSaveExecutionLogs, SshSessionConfig config) {
    super(logCallback, shouldSaveExecutionLogs);
    this.config = config;
  }

  @Override
  public CommandExecutionStatus scpOneFile(String remoteFilePath, AbstractScriptExecutor.FileProvider fileProvider) {
    return FileBasedSshScriptExecutorHelper.scpOneFile(
        remoteFilePath, fileProvider, config, logCallback, shouldSaveExecutionLogs);
  }
}
