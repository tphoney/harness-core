/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.DynatraceDataCollectionInfo;
import io.harness.cvng.core.entities.DynatraceCVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.core.services.api.DataCollectionInfoMapper;
import io.harness.cvng.core.services.api.DataCollectionSLIInfoMapper;
import io.harness.cvng.core.utils.dataCollection.MetricDataCollectionUtils;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

public class DynatraceDataCollectionInfoMapper
    implements DataCollectionInfoMapper<DynatraceDataCollectionInfo, DynatraceCVConfig>,
               DataCollectionSLIInfoMapper<DynatraceDataCollectionInfo, DynatraceCVConfig> {
  @Override
  public DynatraceDataCollectionInfo toDataCollectionInfo(
      DynatraceCVConfig cvConfig, VerificationTask.TaskType taskType) {
    DynatraceDataCollectionInfo dynatraceDataCollectionInfo =
        DynatraceDataCollectionInfo.builder()
            .metricPack(cvConfig.getMetricPack().toDTO())
            .groupName(cvConfig.getGroupName())
            .serviceId(cvConfig.getDynatraceServiceId())
            .serviceMethodIds(cvConfig.getServiceMethodIds())
            .customMetrics(
                CollectionUtils.emptyIfNull(cvConfig.getMetricInfos())
                    .stream()
                    .filter(metricInfo
                        -> MetricDataCollectionUtils.isMetricApplicableForDataCollection(metricInfo, taskType))
                    .map(metricInfo
                        -> DynatraceDataCollectionInfo.MetricCollectionInfo.builder()
                               .metricName(metricInfo.getMetricName())
                               .identifier(metricInfo.getIdentifier())
                               .metricSelector(metricInfo.getMetricSelector())
                               .build())
                    .collect(Collectors.toList()))
            .build();
    dynatraceDataCollectionInfo.setDataCollectionDsl(cvConfig.getDataCollectionDsl());

    return dynatraceDataCollectionInfo;
  }

  @Override
  public DynatraceDataCollectionInfo toDataCollectionInfo(
      List<DynatraceCVConfig> cvConfigs, ServiceLevelIndicator serviceLevelIndicator) {
    List<String> sliMetricIdentifiers = serviceLevelIndicator.getMetricNames();
    cvConfigs =
        CollectionUtils.emptyIfNull(cvConfigs)
            .stream()
            .filter(
                cvConfig -> cvConfig.getMetricPack().getIdentifier().equals(CVNextGenConstants.CUSTOM_PACK_IDENTIFIER))
            .collect(Collectors.toList());
    Preconditions.checkArgument(!cvConfigs.isEmpty(), "Metrics not found in the health-source");
    DynatraceCVConfig baseCvConfig = cvConfigs.get(0);

    DynatraceDataCollectionInfo dynatraceDataCollectionInfo =
        DynatraceDataCollectionInfo.builder()
            .serviceId(baseCvConfig.getDynatraceServiceName())
            .serviceMethodIds(baseCvConfig.getServiceMethodIds())
            .metricPack(baseCvConfig.getMetricPack().toDTO())
            .groupName(baseCvConfig.getGroupName())
            .customMetrics(cvConfigs.stream()
                               .flatMap(cvConfig -> CollectionUtils.emptyIfNull(cvConfig.getMetricInfos()).stream())
                               .filter(metricInfo -> sliMetricIdentifiers.contains(metricInfo.getIdentifier()))
                               .map(metricInfo
                                   -> DynatraceDataCollectionInfo.MetricCollectionInfo.builder()
                                          .metricName(metricInfo.getMetricName())
                                          .identifier(metricInfo.getIdentifier())
                                          .metricSelector(metricInfo.getMetricSelector())
                                          .build())
                               .collect(Collectors.toList()))
            .build();
    dynatraceDataCollectionInfo.setDataCollectionDsl(baseCvConfig.getDataCollectionDsl());
    return dynatraceDataCollectionInfo;
  }
}
