/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serverless;

import java.io.File;
import java.io.OutputStream;
import lombok.experimental.UtilityClass;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@UtilityClass
public class ServerlessUtils {
  public static ProcessResult executeScript(
      String directoryPath, String command, OutputStream output, OutputStream error) throws Exception {
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .directory(new File(directoryPath))
                                          .commandSplit(command)
                                          .readOutput(true)
                                          .redirectOutput(output)
                                          .redirectError(error);
    return processExecutor.execute();
  }
}
