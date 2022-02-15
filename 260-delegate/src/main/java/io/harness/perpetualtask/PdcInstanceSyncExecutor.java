/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.network.SafeHttpCall.execute;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.PdcInstanceSyncPerpetualTaskParams;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.HostReachabilityInfo;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.utils.HostValidationService;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class PdcInstanceSyncExecutor implements PerpetualTaskExecutor {
  @Inject private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private HostValidationService hostValidationService;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    final PdcInstanceSyncPerpetualTaskParams instanceSyncParams =
        AnyUtils.unpack(params.getCustomizedParams(), PdcInstanceSyncPerpetualTaskParams.class);

    final SettingAttribute settingAttribute =
        (SettingAttribute) kryoSerializer.asObject(instanceSyncParams.getSettingAttribute().toByteArray());
    List<HostReachabilityInfo> hostReachabilityInfos = hostValidationService.validateReachability(
        Collections.singletonList(instanceSyncParams.getHostName()), settingAttribute);
    HostReachabilityInfo hostReachabilityInfo = hostReachabilityInfos.get(0);
    try {
      execute(delegateAgentManagerClient.publishInstanceSyncResult(
          taskId.getId(), settingAttribute.getAccountId(), hostReachabilityInfo));
    } catch (Exception e) {
      log.error(String.format("Failed to publish the instance collection result to manager for aws ssh for taskId [%s]",
                    taskId.getId()),
          e);
    }

    return getPerpetualTaskResponse(hostReachabilityInfo);
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(HostReachabilityInfo hostReachabilityInfo) {
    String message = "success";
    if (Boolean.FALSE.equals(hostReachabilityInfo.getReachable())) {
      message = "failure";
    }

    return PerpetualTaskResponse.builder().responseCode(Response.SC_OK).responseMessage(message).build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
