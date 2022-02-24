/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.PrometheusMetricDefinition;
import io.harness.cvng.core.beans.PrometheusMetricDefinition.PrometheusFilter;
import io.harness.cvng.core.utils.analysisinfo.DevelopmentVerificationTransformer;
import io.harness.cvng.core.utils.analysisinfo.LiveMonitoringTransformer;
import io.harness.cvng.core.utils.analysisinfo.SLIMetricTransformer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("PROMETHEUS")
@Data
@SuperBuilder
@FieldNameConstants(innerTypeName = "PrometheusCVConfigKeys")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PrometheusCVConfig extends MetricCVConfig {
  @NotNull private String groupName;
  @NotNull private List<MetricInfo> metricInfoList;

  @Data
  @SuperBuilder
  @FieldNameConstants(innerTypeName = "MetricInfoKeys")
  public static class MetricInfo extends AnalysisInfo {
    private String metricName;
    private String query;
    private String prometheusMetricName;
    private List<PrometheusFilter> serviceFilter;
    private List<PrometheusFilter> envFilter;
    private List<PrometheusFilter> additionalFilters;
    private String aggregation;
    private List<String> tags;
    private TimeSeriesMetricType metricType;
    boolean isManualQuery;
    private String serviceInstanceFieldName;

    public String getQuery() {
      if (isManualQuery) {
        return query;
      }
      String filters = getFilters();
      String queryString = prometheusMetricName + "{" + filters + "}";
      if (isEmpty(aggregation)) {
        return queryString;
      }
      return aggregation + "(" + queryString + ")";
    }

    public String getFilters() {
      if (isManualQuery) {
        int firstIdx = query.indexOf('{');
        int lastIdx = query.lastIndexOf('}');
        return query.substring(firstIdx + 1, lastIdx);
      }
      String filters = getQueryFilterStringFromList(serviceFilter) + "," + getQueryFilterStringFromList(envFilter);

      if (isNotEmpty(additionalFilters)) {
        filters += "," + getQueryFilterStringFromList(additionalFilters);
      }
      return filters;
    }

    private String getQueryFilterStringFromList(List<PrometheusFilter> filters) {
      StringBuffer stringBuffer = new StringBuffer();
      if (isNotEmpty(filters)) {
        filters.forEach(additionalFilter -> {
          if (stringBuffer.length() != 0) {
            stringBuffer.append(',');
          }
          stringBuffer.append(additionalFilter.getQueryFilterString());
        });
      }
      return stringBuffer.toString();
    }
  }

  public void populateFromMetricDefinitions(
      List<PrometheusMetricDefinition> metricDefinitions, CVMonitoringCategory category) {
    if (metricInfoList == null) {
      metricInfoList = new ArrayList<>();
    }
    Preconditions.checkNotNull(metricDefinitions);
    MetricPack metricPack = MetricPack.builder()
                                .category(category)
                                .accountId(getAccountId())
                                .dataSourceType(DataSourceType.PROMETHEUS)
                                .projectIdentifier(getProjectIdentifier())
                                .identifier(category.getDisplayName())
                                .build();

    metricDefinitions.forEach(prometheusMetricDefinition -> {
      TimeSeriesMetricType metricType = prometheusMetricDefinition.getRiskProfile().getMetricType();
      metricInfoList.add(
          MetricInfo.builder()
              .metricName(prometheusMetricDefinition.getMetricName())
              .serviceFilter(prometheusMetricDefinition.getServiceFilter())
              .envFilter(prometheusMetricDefinition.getEnvFilter())
              .query(prometheusMetricDefinition.getQuery())
              .isManualQuery(prometheusMetricDefinition.isManualQuery())
              .metricType(metricType)
              .additionalFilters(prometheusMetricDefinition.getAdditionalFilters())
              .serviceInstanceFieldName(prometheusMetricDefinition.getServiceInstanceFieldName())
              .prometheusMetricName(prometheusMetricDefinition.getPrometheusMetric())
              .identifier(prometheusMetricDefinition.getIdentifier())
              .aggregation(prometheusMetricDefinition.getAggregation())
              .sli(SLIMetricTransformer.transformDTOtoEntity(prometheusMetricDefinition.getSli()))
              .liveMonitoring(LiveMonitoringTransformer.transformDTOtoEntity(prometheusMetricDefinition.getAnalysis()))
              .deploymentVerification(
                  DevelopmentVerificationTransformer.transformDTOtoEntity(prometheusMetricDefinition.getAnalysis()))
              .build());

      // add the relevant thresholds to metricPack
      Set<TimeSeriesThreshold> thresholds =
          getThresholdsToCreateOnSaveForCustomProviders(prometheusMetricDefinition.getMetricName(), metricType,
              prometheusMetricDefinition.getRiskProfile().getThresholdTypes());
      metricPack.addToMetrics(MetricPack.MetricDefinition.builder()
                                  .thresholds(new ArrayList<>(thresholds))
                                  .type(metricType)
                                  .name(prometheusMetricDefinition.getMetricName())
                                  .identifier(prometheusMetricDefinition.getIdentifier())
                                  .included(true)
                                  .build());
    });
    this.setMetricPack(metricPack);
  }

  @Override
  protected void validateParams() {
    checkNotNull(groupName, generateErrorMessageFromParam(PrometheusCVConfigKeys.groupName));
    checkNotNull(metricInfoList, generateErrorMessageFromParam(PrometheusCVConfigKeys.metricInfoList));
  }

  @Override
  public DataSourceType getType() {
    return DataSourceType.PROMETHEUS;
  }

  @Override
  public String getDataCollectionDsl() {
    return getMetricPack().getDataCollectionDsl();
  }

  public static class PrometheusUpdatableEntity
      extends MetricCVConfigUpdatableEntity<PrometheusCVConfig, PrometheusCVConfig> {
    @Override
    public void setUpdateOperations(
        UpdateOperations<PrometheusCVConfig> updateOperations, PrometheusCVConfig cvConfig) {
      setCommonOperations(updateOperations, cvConfig);
      updateOperations.set(PrometheusCVConfigKeys.groupName, cvConfig.getGroupName())
          .set(PrometheusCVConfigKeys.metricInfoList, cvConfig.getMetricInfoList());
    }
  }
}
