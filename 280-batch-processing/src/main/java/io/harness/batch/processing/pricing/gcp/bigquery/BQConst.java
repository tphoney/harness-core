/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.pricing.gcp.bigquery;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CE)
public class BQConst {
  private BQConst() {}

  public static final String AWS_EC2_BILLING_QUERY =
      "SELECT SUM(unblendedcost) as cost, sum(effectivecost) as effectivecost, resourceid, servicecode, productfamily  "
      + "FROM `%s` "
      + "WHERE resourceid IN "
      + "( '%s' )  AND "
      + "usagestartdate  >= '%s' AND usagestartdate < '%s' "
      + "GROUP BY  resourceid, servicecode, productfamily; ";

  public static final String CLUSTER_DATA_QUERY = "SELECT count(*) AS count,  sum(billingamount) AS billingamountsum"
      + " FROM  `%s`  "
      + "WHERE accountid = '%s' and starttime = %s ; ";

  public static final String AZURE_VM_BILLING_QUERY =
      "SELECT SUM(cost) as cost, MAX(azureResourceRate) as azureRate, azureInstanceId, azureVMProviderId, azureMeterCategory "
      + "FROM `%s` "
      + "WHERE  azureVMProviderId IN ( '%s' )  AND "
      + "startTime  >= '%s' AND startTime < '%s' AND cloudProvider = 'AZURE' "
      + "GROUP BY  azureInstanceId, azureVMProviderId, azureMeterCategory; ";

  public static final String EKS_FARGATE_BILLING_QUERY = "SELECT SUM(unblendedcost) as cost, resourceid, usagetype  "
      + "FROM `%s` "
      + "WHERE  "
      + " (%s) AND "
      + "usagestartdate  >= '%s' AND usagestartdate < '%s' "
      + "GROUP BY  resourceid, usagetype; ";

  public static final String AWS_BILLING_DATA = "SELECT resourceid, productfamily  "
      + "FROM `%s` "
      + "WHERE productfamily = 'Compute Instance' AND "
      + "usagestartdate  >= '%s' AND usagestartdate < '%s' AND resourceid IS NOT NULL LIMIT 1";

  public static final String CLOUD_PROVIDER_AGG_DATA =
      "SELECT count(*) AS count, cloudProvider FROM `%s` GROUP BY cloudProvider";

  public static final String CLOUD_PROVIDER_ENTITY_TAGS_INSERT =
      "DELETE FROM `%s` WHERE cloudProviderId='%s' and entityId='%s' and entityType='%s';"
      + " INSERT INTO `%s` (cloudProviderId, entityId, entityType, entityName, labels, updatedAt) "
      + " VALUES ('%s', '%s', '%s', '%s', %s, '%s');";

  public static final String CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME = "cloudProviderEntityTags";

  public static final String cost = "cost";
  public static final String azureRate = "azureRate";
  public static final String effectiveCost = "effectivecost";
  public static final String resourceId = "resourceid";
  public static final String count = "count";
  public static final String billingAmountSum = "billingamountsum";
  public static final String serviceCode = "servicecode";
  public static final String productFamily = "productfamily";
  public static final String usageType = "usagetype";

  public static final String networkProductFamily = "Data Transfer";
  public static final String computeProductFamily = "Compute Instance";

  public static final String eksNetworkInstanceType = "DataTransfer";
  public static final String eksCpuInstanceType = "vCPU-Hours";
  public static final String eksMemoryInstanceType = "Fargate-GB-Hours";
  public static final String azureMeterCategory = "azureMeterCategory";
  public static final String azureVMProviderId = "azureVMProviderId";
  public static final String azureVMMeterCategory = "Virtual Machines";
}
