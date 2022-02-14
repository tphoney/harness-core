/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.serverless.ServerlessCommandTaskHandler;
import io.harness.delegate.task.serverless.request.ServerlessCommandRequest;
import io.harness.delegate.task.serverless.response.ServerlessCommandResponse;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ServerlessDelegateTaskHelper {
  @Inject private Map<String, ServerlessCommandTaskHandler> commandTaskTypeToTaskHandlerMap;
  @Inject private ServerlessInfraConfigHelper serverlessInfraConfigHelper;

  private static final String WORKING_DIR_BASE = "./repository/serverless/";

  public ServerlessCommandResponse getServerlessCommandResponse(
      ServerlessCommandRequest serverlessCommandRequest, ILogStreamingTaskClient iLogStreamingTaskClient) {
    // todo: add commandUnit Progress
    log.info("Starting task execution for command: {}", serverlessCommandRequest.getServerlessCommandType().name());
    decryptRequestDTOs(serverlessCommandRequest);
    return null;
  }

  private void decryptRequestDTOs(ServerlessCommandRequest serverlessCommandRequest) {
    serverlessInfraConfigHelper.decryptServerlessInfraConfig(serverlessCommandRequest.getServerlessInfraConfig());
  }
}
