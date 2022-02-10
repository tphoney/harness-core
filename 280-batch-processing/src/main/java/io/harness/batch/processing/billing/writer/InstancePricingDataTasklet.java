/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.billing.writer;

import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceFamilyAndRegion;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.pricing.gcp.bigquery.BigQueryHelperService;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.ccm.commons.beans.Pricing;
import io.harness.ccm.commons.beans.PricingSource;
import io.harness.ccm.commons.beans.billing.InstanceCategory;
import io.harness.ccm.commons.constants.CloudProvider;
import io.harness.ccm.commons.constants.InstanceMetaDataConstants;
import io.harness.ccm.commons.entities.batch.InstanceData;
import io.harness.pricing.client.CloudInfoPricingClient;
import io.harness.pricing.dto.cloudinfo.ProductDetailResponse;
import io.harness.pricing.dto.cloudinfo.ProductDetails;
import io.harness.pricing.dto.cloudinfo.ProductDetailsResponse;
import io.harness.pricing.dto.cloudinfo.ZonePrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData;

@OwnedBy(HarnessTeam.CE)
@Slf4j
@Singleton
public class InstancePricingDataTasklet implements Tasklet {
  @Autowired private InstanceDataDao instanceDataDao;
  @Autowired private BatchMainConfig config;
  @Autowired private CustomBillingMetaDataService customBillingMetaDataService;
  @Autowired private BigQueryHelperService bigQueryHelperService;
  @Autowired private CloudInfoPricingClient banzaiPricingClient;

  private JobParameters parameters;
  private int batchSize;
  private BatchJobType batchJobType;
  private final String COMPUTE_SERVICE = "compute";

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    batchSize = config.getBatchQueryConfig().getInstanceDataBatchSize();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    log.info("Instance Pricing Job Started for Account ID: {}", accountId);
    Instant startTime = getFieldValueFromJobParams(CCMJobConstants.JOB_START_DATE);
    Instant endTime = getFieldValueFromJobParams(CCMJobConstants.JOB_END_DATE);
    batchJobType = CCMJobConstants.getBatchJobTypeFromJobParams(parameters, CCMJobConstants.BATCH_JOB_TYPE);
    List<InstanceData> instanceDataLists;
    Instant activeInstanceIterator = startTime;

    do {
      instanceDataLists =
          instanceDataDao.getInstanceDataListForPricingUpdate(accountId, batchSize, activeInstanceIterator, endTime);
      log.info("Processing {} instances", instanceDataLists.size());
      for (InstanceData instanceDataList : instanceDataLists) {
        if (!instanceDataList.getMetaData().get(InstanceMetaDataConstants.CLOUD_PROVIDER).equals("AWS")) continue;
        log.info("InstanceId: {}, Instance Family: {}, Region: {}, Cloud Provider: {}", instanceDataList.getCloudProviderInstanceId(),
            instanceDataList.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY),
            instanceDataList.getMetaData().get(InstanceMetaDataConstants.REGION),
            instanceDataList.getMetaData().get(InstanceMetaDataConstants.CLOUD_PROVIDER));
      }
      if (!instanceDataLists.isEmpty()) {
        activeInstanceIterator = instanceDataLists.get(instanceDataLists.size() - 1).getActiveInstanceIterator();
        if (instanceDataLists.get(0).getActiveInstanceIterator().equals(activeInstanceIterator)) {
          activeInstanceIterator = activeInstanceIterator.plus(1, ChronoUnit.MILLIS);
        }
      }
      Map<String, List<InstanceData>> awsInstances =
          instanceDataLists.stream().filter((InstanceData instanceData) ->
              getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLOUD_PROVIDER, instanceData)
                  .equals(CloudProvider.AWS.name())
          ).collect(Collectors.groupingBy((InstanceData instanceData) -> {
            if (instanceData.getCloudProviderInstanceId() != null)
              return instanceData.getCloudProviderInstanceId();
            return "";
          }));
      log.info("AWS Instances size: {}", awsInstances.size());
      Set<String> leftOverInstances = awsInstances.keySet();
      // call BQHelperSerivce with awsInstances.keySet
      String awsDataSetId = customBillingMetaDataService.getAwsDataSetId(accountId);
      Map<String, Pricing> pricingDataByResourceId = bigQueryHelperService.getAwsPricingDataByResourceIds(
          new ArrayList<>(leftOverInstances), startTime, endTime, awsDataSetId);
      log.info("Got response from BQ ResourceID, map: {}, size: {}", pricingDataByResourceId, pricingDataByResourceId.size());
      // update awsInstances
      pricingDataByResourceId.forEach(
          (String resourceId, Pricing pricing) -> awsInstances.get(resourceId).forEach(
              (InstanceData instanceData) -> instanceDataDao.updateInstancePricingData(instanceData, pricing))
      );
      leftOverInstances.removeAll(pricingDataByResourceId.keySet());
      Set<InstanceFamilyAndRegion> instanceFamilyAndRegions = new HashSet<>();
      for (String resourceId: leftOverInstances) {
        if (!resourceId.equals("")) {
          InstanceData instanceData = awsInstances.get(resourceId).get(0);
          instanceFamilyAndRegions.add(new InstanceFamilyAndRegion(
              instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY),
              instanceData.getMetaData().get(InstanceMetaDataConstants.REGION)));
        } else {
          awsInstances.get(resourceId).forEach(
              (instanceData -> instanceFamilyAndRegions.add(new InstanceFamilyAndRegion(
                  instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY),
                  instanceData.getMetaData().get(InstanceMetaDataConstants.REGION)))))
          ;
        }
      }
      Map<InstanceFamilyAndRegion, Pricing> pricingDataByInstanceFamilyAndRegion =
          bigQueryHelperService.getAwsPricingDataByInstanceFamilyAndRegion(new ArrayList<>(instanceFamilyAndRegions),
              startTime, endTime, awsDataSetId);
      log.info("Got response from BQ Family and Region, map: {}, size: {}", pricingDataByInstanceFamilyAndRegion, pricingDataByInstanceFamilyAndRegion.size());
      // update awsInstances
      leftOverInstances.forEach((String resourceId) ->
          awsInstances.get(resourceId).forEach((InstanceData instanceData) -> {
            InstanceFamilyAndRegion instanceFamilyAndRegion = new InstanceFamilyAndRegion(
                instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY),
                instanceData.getMetaData().get(InstanceMetaDataConstants.REGION));
            if (pricingDataByInstanceFamilyAndRegion.containsKey(instanceFamilyAndRegion)) {
              instanceDataDao.updateInstancePricingData(instanceData, pricingDataByInstanceFamilyAndRegion.get(instanceFamilyAndRegion));
            }
          })
      );
      Set<String> instanceFamilies = new HashSet<>();
      leftOverInstances.removeIf((String resourceId) -> {
        if (resourceId.equals("")) return false;
        InstanceData instanceData = awsInstances.get(resourceId).get(0);
        String instanceFamily = instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY);
        String region = instanceData.getMetaData().get(InstanceMetaDataConstants.REGION);
        if (pricingDataByInstanceFamilyAndRegion.containsKey(new InstanceFamilyAndRegion(instanceFamily, region))) {
          return true;
        } else {
          instanceFamilies.add(instanceFamily);
          return false;
        }
      });
      Map<String, Pricing> pricingDataByInstanceFamily = bigQueryHelperService.getAwsPricingDataByInstanceFamily(
          new ArrayList<>(instanceFamilies), startTime, endTime, awsDataSetId);
      log.info("Got response from BQ Family and Region, map: {}, size: {}", pricingDataByInstanceFamily, pricingDataByInstanceFamily.size());
      // update awsInstances
      leftOverInstances.forEach((String resourceId) -> {
        awsInstances.get(resourceId).forEach((InstanceData instanceData) -> {
          InstanceFamilyAndRegion instanceFamilyAndRegion = new InstanceFamilyAndRegion(
              instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY),
              instanceData.getMetaData().get(InstanceMetaDataConstants.REGION));
          String instanceFamily = instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY);
          if (pricingDataByInstanceFamily.containsKey(instanceFamily) &&
              !pricingDataByInstanceFamilyAndRegion.containsKey(instanceFamilyAndRegion)) {
            instanceDataDao.updateInstancePricingData(instanceData, pricingDataByInstanceFamily.get(instanceFamily));
          }
        });
      });
      leftOverInstances.removeIf((String resourceId) -> pricingDataByInstanceFamily.containsKey(
          awsInstances.get(resourceId).get(0).getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY))
          && !resourceId.equals(""));
      // get pricing from public api and update instances
      for (String resourceId: leftOverInstances) {
        try {
          Pricing pricing = getPublicPricing(awsInstances.get(resourceId).get(0));
          if (pricing == null) continue;;
          if (!resourceId.equals("")) {
            awsInstances.get(resourceId).forEach((InstanceData instanceData) ->
                instanceDataDao.updateInstancePricingData(instanceData, pricing));
          }
          else {
            instanceDataDao.updateInstancePricingData(awsInstances.get(resourceId).get(0), pricing);
            for (int i=1; i<awsInstances.get(resourceId).size(); i++) {
              InstanceData instanceData = awsInstances.get(resourceId).get(i);
              InstanceFamilyAndRegion instanceFamilyAndRegion = new InstanceFamilyAndRegion(
                  instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY),
                  instanceData.getMetaData().get(InstanceMetaDataConstants.REGION));
              String instanceFamily = instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY);
              if (!pricingDataByInstanceFamilyAndRegion.containsKey(instanceFamilyAndRegion) &&
                  !pricingDataByInstanceFamily.containsKey(instanceFamily)) {
                instanceDataDao.updateInstancePricingData(instanceData, getPublicPricing(instanceData));
              }
            }
          }
        } catch (IOException e) {
          log.error("Exception in pricing service ", e);
        }
      }

      Map<String, List<InstanceData>> azureInstances =
          instanceDataLists.stream().filter((InstanceData instanceData) ->
              getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.CLOUD_PROVIDER, instanceData)
                  .equals(CloudProvider.AZURE.name())
          ).collect(Collectors.groupingBy((InstanceData instanceData) -> {
            if (instanceData.getCloudProviderInstanceId() != null)
              return instanceData.getCloudProviderInstanceId();
            return "";
          }));
      log.info("Azure Instances size: {}", azureInstances.size());

      leftOverInstances = azureInstances.keySet();
      // call BQHelperSerivce with awsInstances.keySet
      String azureDataSetId = customBillingMetaDataService.getAzureDataSetId(accountId);
      pricingDataByResourceId = bigQueryHelperService.getAzurePricingDataByResourceIds(
          new ArrayList<>(leftOverInstances), startTime, endTime, azureDataSetId);
      log.info("Got response from BQ ResourceID, map: {}, size: {}", pricingDataByResourceId, pricingDataByResourceId.size());
      // update awsInstances
      pricingDataByResourceId.forEach(
          (String resourceId, Pricing pricing) -> azureInstances.get(resourceId).forEach(
              (InstanceData instanceData) -> instanceDataDao.updateInstancePricingData(instanceData, pricing))
      );
      leftOverInstances.removeAll(pricingDataByResourceId.keySet());

      // get pricing from public api and update instances
      for (String resourceId: leftOverInstances) {
        try {
          Pricing pricing = getPublicPricing(azureInstances.get(resourceId).get(0));
          if (pricing == null) continue;;
          if (!resourceId.equals("")) {
            azureInstances.get(resourceId).forEach((InstanceData instanceData) ->
                instanceDataDao.updateInstancePricingData(instanceData, pricing));
          }
          else {
            instanceDataDao.updateInstancePricingData(azureInstances.get(resourceId).get(0), pricing);
            for (int i=1; i<azureInstances.get(resourceId).size(); i++) {
              InstanceData instanceData = azureInstances.get(resourceId).get(i);
              instanceDataDao.updateInstancePricingData(instanceData, getPublicPricing(instanceData));
            }
          }
        } catch (IOException e) {
          log.error("Exception in pricing service ", e);
        }
      }

    } while (instanceDataLists.size() == batchSize);
    log.info("Instance Pricing Job Finished");
    return null;
  }

  private Instant getFieldValueFromJobParams(String fieldName) {
    return Instant.ofEpochMilli(Long.parseLong(parameters.getString(fieldName)));
  }

  private Pricing getPublicPricing(InstanceData instanceData) throws IOException {
    String cloudProvider = instanceData.getMetaData().get(InstanceMetaDataConstants.CLOUD_PROVIDER);
    String k8sService = (cloudProvider.equalsIgnoreCase(CloudProvider.AWS.getCloudProviderName())) ?
        CloudProvider.AWS.getK8sService() : CloudProvider.AZURE.getK8sService();
    log.info("Getting public Pricing for cloudProvider: {}, service: {}, region: {}, instanceFamily: {}", cloudProvider, k8sService, instanceData.getMetaData().get(InstanceMetaDataConstants.REGION), instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY));
    Call<ProductDetailResponse> pricingInfoCall = banzaiPricingClient.getPricingInfo(
        cloudProvider,
        instanceData.getMetaData().get(InstanceMetaDataConstants.CLUSTER_TYPE).equals(ClusterType.K8S.name()) ?
            k8sService : COMPUTE_SERVICE,
        instanceData.getMetaData().get(InstanceMetaDataConstants.REGION),
        instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY));
    double pricePerHour = 0;
    Response<ProductDetailResponse> pricingInfo = pricingInfoCall.execute();
    if (null != pricingInfo.body() && null != pricingInfo.body().getProduct()) {
      ProductDetails product = pricingInfo.body().getProduct();
      pricePerHour = product.getOnDemandPrice();
      if (InstanceCategory.valueOf(instanceData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_CATEGORY))
          .equals(InstanceCategory.SPOT)) {
        pricePerHour = product.getSpotPrice()
            .stream()
            .filter(zonePrice -> zonePrice.getZone().equals(instanceData.getMetaData().get(InstanceMetaDataConstants.ZONE)))
            .findFirst()
            .map(ZonePrice::getPrice)
            .orElse(pricePerHour);
      }
      return new Pricing(new BigDecimal(pricePerHour), PricingSource.PUBLIC_API);
    } else {
      log.info("Null response from banzai service");
    }
    return null;
  }
}
