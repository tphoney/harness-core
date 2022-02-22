/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.remote.utils;

import io.harness.ccm.commons.entities.CCMField;
import io.harness.ccm.commons.entities.CCMOperator;
import io.harness.ccm.commons.entities.CCMSortOrder;
import io.harness.ccm.commons.entities.CCMStringFilter;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.graphql.QLCESortOrder;
import io.harness.ccm.views.graphql.QLCEViewAggregateOperation;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewField;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewSortType;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewTimeGroupType;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.utils.ViewFieldUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.joda.time.DateTime;

public class RESTToGraphQLHelper {
  public static final String COST = "cost";

  public static List<QLCEViewFilterWrapper> convertFilters(
      List<CCMStringFilter> filters, String perspectiveId, DateTime startTime, DateTime endTime) throws Exception {
    List<QLCEViewFilterWrapper> modifiedFilters = new ArrayList<>();

    // convertPerspectiveId Query Param to Filter
    modifiedFilters.add(
        QLCEViewFilterWrapper.builder()
            .viewMetadataFilter(QLCEViewMetadataFilter.builder().isPreview(false).viewId(perspectiveId).build())
            .build());

    // Add startTime Filter
    modifiedFilters.add(QLCEViewFilterWrapper.builder()
                            .timeFilter(QLCEViewTimeFilter.builder()
                                            .field(QLCEViewFieldInput.builder()
                                                       .fieldId("startTime")
                                                       .fieldName("startTime")
                                                       .identifier(ViewFieldIdentifier.COMMON)
                                                       .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
                                                       .build())
                                            .operator(QLCEViewTimeFilterOperator.AFTER)
                                            .value(startTime.getMillis())
                                            .build())
                            .build());

    // Add EndTime Filter
    modifiedFilters.add(QLCEViewFilterWrapper.builder()
                            .timeFilter(QLCEViewTimeFilter.builder()
                                            .field(QLCEViewFieldInput.builder()
                                                       .fieldId("startTime")
                                                       .fieldName("startTime")
                                                       .identifier(ViewFieldIdentifier.COMMON)
                                                       .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
                                                       .build())
                                            .operator(QLCEViewTimeFilterOperator.BEFORE)
                                            .value(endTime.getMillis())
                                            .build())
                            .build());

    if (filters != null) {
      for (CCMStringFilter filter : filters) {
        modifiedFilters.add(QLCEViewFilterWrapper.builder()
                                .idFilter(QLCEViewFilter.builder()
                                              .field(getViewFieldInputFromCCMField(filter.getField()))
                                              .operator(mapCCMOperatorToQLCEViewFilterOperator(filter.getOperator()))
                                              .values(getStringArray(filter.getValues()))
                                              .build())
                                .build());
      }
    }

    return modifiedFilters;
  }

  public static List<QLCEViewAggregation> getCostAggregation() {
    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(
        QLCEViewAggregation.builder().operationType(QLCEViewAggregateOperation.SUM).columnName(COST).build());
    return aggregations;
  }

  private static String[] getStringArray(List<String> values) {
    return values.toArray(new String[values.size()]);
  }

  private static QLCEViewFilterOperator mapCCMOperatorToQLCEViewFilterOperator(CCMOperator operator) throws Exception {
    if (operator.equals(CCMOperator.IN)) {
      return QLCEViewFilterOperator.IN;
    } else if (operator.equals(CCMOperator.NOT_IN)) {
      return QLCEViewFilterOperator.NOT_IN;
    } else if (operator.equals(CCMOperator.NOT_NULL)) {
      return QLCEViewFilterOperator.NOT_NULL;
    } else if (operator.equals(CCMOperator.NULL)) {
      return QLCEViewFilterOperator.NULL;
    } else if (operator.equals(CCMOperator.LIKE)) {
      return QLCEViewFilterOperator.LIKE;
    }
    throw new Exception(String.format(
        "CCM Operator input [%s] is not supported, supported operators are IN, NOT_IN, NOT_NULL, NULL, LIKE",
        operator.toString()));
  }

  public static List<QLCEViewSortCriteria> getCostSortingCriteria(CCMSortOrder sortOrderInput) {
    List<QLCEViewSortCriteria> sortCriteriaList = new ArrayList<>();
    QLCESortOrder sortOrder = QLCESortOrder.DESCENDING;
    if (sortOrderInput != null && sortOrderInput == CCMSortOrder.ASCENDING) {
      sortOrder = QLCESortOrder.ASCENDING;
    }
    sortCriteriaList.add(QLCEViewSortCriteria.builder().sortType(QLCEViewSortType.COST).sortOrder(sortOrder).build());
    return sortCriteriaList;
  }

  public static List<QLCEViewGroupBy> convertGroupBy(List<CCMField> groupBys) throws Exception {
    List<QLCEViewGroupBy> groupByList = new ArrayList<>();
    for (CCMField field : groupBys) {
      groupByList.add(QLCEViewGroupBy.builder().entityGroupBy(getViewFieldInputFromCCMField(field)).build());
    }
    return groupByList;
  }

  public static QLCEViewGroupBy convertTimeSeriesGroupBy(QLCEViewTimeGroupType timeResolution) {
    if (timeResolution == null) {
      timeResolution = QLCEViewTimeGroupType.DAY;
    }
    return QLCEViewGroupBy.builder()
        .timeTruncGroupBy(QLCEViewTimeTruncGroupBy.builder().resolution(timeResolution).build())
        .build();
  }

  private static QLCEViewFieldInput getViewFieldInputFromCCMField(CCMField field) throws Exception {
    HashMap<String, QLCEViewFieldInput> viewFieldsHashMap = getViewFieldsHashMap();
    QLCEViewFieldInput viewField = viewFieldsHashMap.get(field.toString());
    if (viewField == null) {
      throw new Exception(
          String.format("CCM Field input [%s] is not supported, please provide a valid Field.", field.toString()));
    }
    return viewField;
  }

  private static HashMap<String, QLCEViewFieldInput> getViewFieldsHashMap() {
    HashMap<String, QLCEViewFieldInput> viewFieldHashMap = new HashMap<>();
    List<QLCEViewField> commonFields = ViewFieldUtils.getCommonFields();
    viewFieldHashMap.put("COMMON_REGION",
        QLCEViewFieldInput.builder()
            .fieldId(commonFields.get(0).getFieldId())
            .fieldName(commonFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.COMMON)
            .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
            .build());
    viewFieldHashMap.put("COMMON_PRODUCT",
        QLCEViewFieldInput.builder()
            .fieldId(commonFields.get(1).getFieldId())
            .fieldName(commonFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.COMMON)
            .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
            .build());
    viewFieldHashMap.put("COMMON_NONE",
        QLCEViewFieldInput.builder()
            .fieldId(commonFields.get(3).getFieldId())
            .fieldName(commonFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.COMMON)
            .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
            .build());

    List<QLCEViewField> awsFields = ViewFieldUtils.getAwsFields();
    viewFieldHashMap.put("AWS_SERVICE",
        QLCEViewFieldInput.builder()
            .fieldId(awsFields.get(0).getFieldId())
            .fieldName(awsFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.AWS)
            .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
            .build());
    viewFieldHashMap.put("AWS_ACCOUNT",
        QLCEViewFieldInput.builder()
            .fieldId(awsFields.get(1).getFieldId())
            .fieldName(awsFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.AWS)
            .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
            .build());
    viewFieldHashMap.put("AWS_INSTANCE_TYPE",
        QLCEViewFieldInput.builder()
            .fieldId(awsFields.get(2).getFieldId())
            .fieldName(awsFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.AWS)
            .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
            .build());
    viewFieldHashMap.put("AWS_USAGE_TYPE",
        QLCEViewFieldInput.builder()
            .fieldId(awsFields.get(3).getFieldId())
            .fieldName(awsFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.AWS)
            .identifierName(ViewFieldIdentifier.AWS.getDisplayName())
            .build());

    List<QLCEViewField> gcpFields = ViewFieldUtils.getGcpFields();
    viewFieldHashMap.put("GCP_PRODUCT",
        QLCEViewFieldInput.builder()
            .fieldId(gcpFields.get(0).getFieldId())
            .fieldName(gcpFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.GCP)
            .identifierName(ViewFieldIdentifier.GCP.getDisplayName())
            .build());
    viewFieldHashMap.put("GCP_PROJECT",
        QLCEViewFieldInput.builder()
            .fieldId(gcpFields.get(1).getFieldId())
            .fieldName(gcpFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.GCP)
            .identifierName(ViewFieldIdentifier.GCP.getDisplayName())
            .build());
    viewFieldHashMap.put("GCP_SKU_DESCRIPTION",
        QLCEViewFieldInput.builder()
            .fieldId(gcpFields.get(2).getFieldId())
            .fieldName(gcpFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.GCP)
            .identifierName(ViewFieldIdentifier.GCP.getDisplayName())
            .build());

    List<QLCEViewField> azureFields = ViewFieldUtils.getAzureFields();
    List<QLCEViewField> variableAzureFields = ViewFieldUtils.getVariableAzureFields();
    viewFieldHashMap.put("AZURE_SUBSCRIPTION_GUID",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(0).getFieldId())
            .fieldName(azureFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_METER_NAME",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(1).getFieldId())
            .fieldName(azureFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_METER_CATEGORY",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(2).getFieldId())
            .fieldName(azureFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_METER_SUBCATEGORY",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(3).getFieldId())
            .fieldName(azureFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_RESOURCE_ID",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(4).getFieldId())
            .fieldName(azureFields.get(4).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_RESOURCE_GROUP_NAME",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(5).getFieldId())
            .fieldName(azureFields.get(5).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_RESOURCE_TYPE",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(6).getFieldId())
            .fieldName(azureFields.get(6).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_RESOURCE",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(7).getFieldId())
            .fieldName(azureFields.get(7).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_SERVICE_NAME",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(8).getFieldId())
            .fieldName(azureFields.get(8).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_SERVICE_TIER",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(9).getFieldId())
            .fieldName(azureFields.get(9).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_INSTANCE_ID",
        QLCEViewFieldInput.builder()
            .fieldId(azureFields.get(10).getFieldId())
            .fieldName(azureFields.get(10).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_SUBSCRIPTION_NAME",
        QLCEViewFieldInput.builder()
            .fieldId(variableAzureFields.get(0).getFieldId())
            .fieldName(variableAzureFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_PUBLISHER_NAME",
        QLCEViewFieldInput.builder()
            .fieldId(variableAzureFields.get(1).getFieldId())
            .fieldName(variableAzureFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_PUBLISHER_TYPE",
        QLCEViewFieldInput.builder()
            .fieldId(variableAzureFields.get(2).getFieldId())
            .fieldName(variableAzureFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_RESERVATION_ID",
        QLCEViewFieldInput.builder()
            .fieldId(variableAzureFields.get(3).getFieldId())
            .fieldName(variableAzureFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_RESERVATION_NAME",
        QLCEViewFieldInput.builder()
            .fieldId(variableAzureFields.get(4).getFieldId())
            .fieldName(variableAzureFields.get(4).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());
    viewFieldHashMap.put("AZURE_FREQUENCY",
        QLCEViewFieldInput.builder()
            .fieldId(variableAzureFields.get(5).getFieldId())
            .fieldName(variableAzureFields.get(5).getFieldId())
            .identifier(ViewFieldIdentifier.AZURE)
            .identifierName(ViewFieldIdentifier.AZURE.getDisplayName())
            .build());

    List<QLCEViewField> clusterFields = ViewFieldUtils.getClusterFields();
    viewFieldHashMap.put("CLUSTER_NAME",
        QLCEViewFieldInput.builder()
            .fieldId(clusterFields.get(0).getFieldId())
            .fieldName(clusterFields.get(0).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .identifierName(ViewFieldIdentifier.CLUSTER.getDisplayName())
            .build());
    viewFieldHashMap.put("CLUSTER_NAMESPACE",
        QLCEViewFieldInput.builder()
            .fieldId(clusterFields.get(1).getFieldId())
            .fieldName(clusterFields.get(1).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .identifierName(ViewFieldIdentifier.CLUSTER.getDisplayName())
            .build());
    viewFieldHashMap.put("CLUSTER_WORKLOAD",
        QLCEViewFieldInput.builder()
            .fieldId(clusterFields.get(2).getFieldId())
            .fieldName(clusterFields.get(2).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .identifierName(ViewFieldIdentifier.CLUSTER.getDisplayName())
            .build());
    viewFieldHashMap.put("CLUSTER_APPLICATION",
        QLCEViewFieldInput.builder()
            .fieldId(clusterFields.get(3).getFieldId())
            .fieldName(clusterFields.get(3).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .identifierName(ViewFieldIdentifier.CLUSTER.getDisplayName())
            .build());
    viewFieldHashMap.put("CLUSTER_ENVIRONMENT",
        QLCEViewFieldInput.builder()
            .fieldId(clusterFields.get(4).getFieldId())
            .fieldName(clusterFields.get(4).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .identifierName(ViewFieldIdentifier.CLUSTER.getDisplayName())
            .build());
    viewFieldHashMap.put("CLUSTER_SERVICE",
        QLCEViewFieldInput.builder()
            .fieldId(clusterFields.get(5).getFieldId())
            .fieldName(clusterFields.get(5).getFieldId())
            .identifier(ViewFieldIdentifier.CLUSTER)
            .identifierName(ViewFieldIdentifier.CLUSTER.getDisplayName())
            .build());
    return viewFieldHashMap;
  }
}
