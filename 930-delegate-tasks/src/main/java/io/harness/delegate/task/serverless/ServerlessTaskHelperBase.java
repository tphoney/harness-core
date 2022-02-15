/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.filesystem.FileIo.*;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.logging.LogCallback;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class ServerlessTaskHelperBase {
  public LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }
  public void createHomeDirectory(String directoryPath) throws IOException {
    createDirectoryIfDoesNotExist(directoryPath);
    waitForDirectoryToBeAccessibleOutOfProcess(directoryPath, 10);
  }

  public void putManifestFileToWorkingDirectory(
      String content, ServerlessDelegateTaskParams serverlessDelegateTaskParams) throws IOException {
    writeUtf8StringToFile(Paths.get(serverlessDelegateTaskParams.getWorkingDirectory(), "").toString(), content);
  }
}
