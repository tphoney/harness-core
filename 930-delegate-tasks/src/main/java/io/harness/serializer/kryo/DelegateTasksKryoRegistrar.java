/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskParameters;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskResponse;
import io.harness.delegate.task.winrm.AuthenticationScheme;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.trigger.WebHookTriggerResponseData;
import software.wings.beans.trigger.WebhookTriggerParameters;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.delegatetasks.cv.beans.CustomLogResponseMapper;
import software.wings.delegatetasks.cv.beans.NewRelicMetricDataRecord;
import software.wings.delegatetasks.cv.beans.analysis.ClusterLevel;
import software.wings.delegatetasks.cv.beans.analysis.CustomLogDataCollectionInfo;
import software.wings.delegatetasks.cv.beans.analysis.DataCollectionTaskResult;
import software.wings.delegatetasks.cv.beans.analysis.LogElement;
import software.wings.delegatetasks.cv.beans.analysis.SetupTestNodeData;
import software.wings.delegatetasks.cv.beans.analysis.TimeSeriesMlAnalysisType;
import software.wings.delegatetasks.cv.beans.appd.AppDynamicsConfig;
import software.wings.delegatetasks.cv.beans.appd.AppdynamicsDataCollectionInfo;
import software.wings.delegatetasks.cv.beans.appd.AppdynamicsSetupTestNodeData;

import com.esotericsoftware.kryo.Kryo;

public class DelegateTasksKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ExecutionLogCallback.class, 5044);
    kryo.register(DataCollectionException.class, 7298);
    kryo.register(BatchCapabilityCheckTaskParameters.class, 8200);
    kryo.register(BatchCapabilityCheckTaskResponse.class, 8201);
    kryo.register(WebhookTriggerParameters.class, 8550);
    kryo.register(WebHookTriggerResponseData.class, 8552);
    kryo.register(AuthenticationScheme.class, 8600);
    kryo.register(AppDynamicsConfig.class, 8601);
    kryo.register(CustomLogDataCollectionInfo.class, 8602);
    kryo.register(DataCollectionTaskResult.class, 8603);
    kryo.register(LogElement.class, 8604);
    kryo.register(SetupTestNodeData.class, 8605);
    kryo.register(AppdynamicsSetupTestNodeData.class, 8606);
    kryo.register(AppdynamicsDataCollectionInfo.class, 8607);
    kryo.register(ClusterLevel.class, 8608);
    kryo.register(DataCollectionTaskResult.DataCollectionTaskStatus.class, 8609);
    kryo.register(TimeSeriesMlAnalysisType.class, 8610);
    kryo.register(CustomLogResponseMapper.class, 8611);
    kryo.register(NewRelicMetricDataRecord.class, 8612);
    kryo.register(SetupTestNodeData.Instance.class, 7470);
  }
}
