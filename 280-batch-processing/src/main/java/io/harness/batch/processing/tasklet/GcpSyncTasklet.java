/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.hazelcast.util.Preconditions.checkFalse;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.ccm.commons.entities.batch.BatchJobScheduledData;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpServiceAccount;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.filter.FilterType;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RestCallToNGManagerClientUtils;

import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.paging.Page;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class GcpSyncTasklet implements Tasklet {
  public static final String SERVICE_ACCOUNT = "serviceAccount";
  public static final String SOURCE_DATA_SET_ID = "sourceDataSetId";
  public static final String SOURCE_GCP_PROJECT_ID = "sourceGcpProjectId";
  public static final String SOURCE_DATA_SET_REGION = "sourceDataSetRegion";
  public static final String CONNECTOR_ID = "connectorId";
  public static final String ACCOUNT_ID = "accountId";
  public static final String GCP_BILLING_EXPORT_V_1 = "gcp_billing_export_v1";
  public static final String TABLE_NAME = "sourceGcpTableName";
  @Autowired private BatchMainConfig mainConfig;
  @Autowired private ConnectorResourceClient connectorResourceClient;
  @Autowired protected CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private BatchJobScheduledDataDao batchJobScheduledDataDao;
  private static final String GOOGLE_CREDENTIALS_PATH = "CE_GCP_CREDENTIALS_PATH";

  private final Cache<CacheKey, Boolean> gcpSyncInfo =
      Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

  @Value
  private static class CacheKey {
    private String accountId;
    private String projectId;
    private String datasetId;
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    JobConstants jobConstants = CCMJobConstants.fromContext(chunkContext);
    String accountId = jobConstants.getAccountId();
    Long endTime = jobConstants.getJobEndTime();

    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(accountId, BatchJobType.SYNC_BILLING_REPORT_GCP);
    if (null != batchJobScheduledData) {
      endTime = batchJobScheduledData.getEndAt().toEpochMilli();
    }
    BillingDataPipelineConfig billingDataPipelineConfig = mainConfig.getBillingDataPipelineConfig();

    if (billingDataPipelineConfig.isGcpSyncEnabled()) {
      List<ConnectorResponseDTO> nextGenGCPConnectorResponses = getNextGenGCPConnectorResponses(accountId);
      for (ConnectorResponseDTO connector : nextGenGCPConnectorResponses) {
        ConnectorInfoDTO connectorInfo = connector.getConnector();
        GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO =
            (GcpCloudCostConnectorDTO) connectorInfo.getConnectorConfig();
        log.info("ServiceAccountEmail: {}, DatasetId: {}, "
                + "ProjectId: {}, TableId: {}"
                + "ConnectorIdentifier: {}",
            gcpCloudCostConnectorDTO.getServiceAccountEmail(),
            gcpCloudCostConnectorDTO.getBillingExportSpec().getDatasetId(), gcpCloudCostConnectorDTO.getProjectId(),
            gcpCloudCostConnectorDTO.getBillingExportSpec().getTableId(), connectorInfo.getIdentifier());
        try {
          processGCPConnector(billingDataPipelineConfig, gcpCloudCostConnectorDTO.getServiceAccountEmail(),
              gcpCloudCostConnectorDTO.getBillingExportSpec().getDatasetId(), gcpCloudCostConnectorDTO.getProjectId(),
              gcpCloudCostConnectorDTO.getBillingExportSpec().getTableId(), accountId, connectorInfo.getIdentifier(),
              endTime);
        } catch (Exception e) {
          log.error("Exception processing NG GCP Connector: {}", connectorInfo.getIdentifier(), e);
        }
      }

      List<GcpBillingAccount> gcpBillingAccounts =
          cloudToHarnessMappingService.listGcpBillingAccountUpdatedInDuration(accountId);
      log.info("Processing batch size of {} in GCP Sync Job for CG Connectors", gcpBillingAccounts.size());
      for (GcpBillingAccount gcpBillingAccount : gcpBillingAccounts) {
        GcpServiceAccount gcpServiceAccount = cloudToHarnessMappingService.getGcpServiceAccount(accountId);
        log.info("ServiceAccountEmail: {}, DatasetId: {}, "
                + "ProjectId: {},"
                + "ConnectorIdentifier: {}",
            gcpServiceAccount.getEmail(), gcpBillingAccount.getBqDatasetId(), gcpBillingAccount.getBqProjectId(),
            gcpBillingAccount.getUuid());
        try {
          processGCPConnector(billingDataPipelineConfig, gcpServiceAccount.getEmail(),
              gcpBillingAccount.getBqDatasetId(), gcpBillingAccount.getBqProjectId(), "", accountId,
              gcpBillingAccount.getUuid(), endTime);
        } catch (Exception e) {
          log.error("Exception processing CG GCP Connector: {}", gcpBillingAccount.getUuid(), e);
        }
      }
    }
    return null;
  }

  private void processGCPConnector(BillingDataPipelineConfig billingDataPipelineConfig, String serviceAccountEmail,
      String datasetId, String projectId, String tableName, String accountId, String connectorId, Long endTime) {
    ServiceAccountCredentials sourceCredentials = getCredentials(GOOGLE_CREDENTIALS_PATH);
    Credentials credentials = getImpersonatedCredentials(sourceCredentials, serviceAccountEmail);
    BigQuery bigQuery = BigQueryOptions.newBuilder().setCredentials(credentials).build().getService();
    DatasetId datasetIdFullyQualified = DatasetId.of(projectId, datasetId);
    Dataset dataset = bigQuery.getDataset(datasetIdFullyQualified);
    if (isEmpty(tableName)) {
      // Older way to get the tableName
      Page<Table> tableList = dataset.list(BigQuery.TableListOption.pageSize(1000));
      tableList.getValues().forEach(table -> {
        if (table.getTableId().getTable().contains(GCP_BILLING_EXPORT_V_1)) {
          TableId tableId1 = TableId.of(projectId, datasetId, table.getTableId().getTable());
          Table tableGranularData = bigQuery.getTable(tableId1);
          Long lastModifiedTime = tableGranularData.getLastModifiedTime();
          lastModifiedTime = lastModifiedTime != null ? lastModifiedTime : table.getCreationTime();
          log.info("Sync condition {} {}", lastModifiedTime, endTime);
          if (lastModifiedTime > endTime) {
            CacheKey cacheKey = new CacheKey(accountId, projectId, datasetId);
            gcpSyncInfo.get(cacheKey,
                key
                -> publishMessage(sourceCredentials, billingDataPipelineConfig.getGcpProjectId(),
                    billingDataPipelineConfig.getGcpSyncPubSubTopic(), dataset.getLocation(), serviceAccountEmail,
                    datasetId, projectId, accountId, connectorId, table.getTableId().getTable()));
            return;
          }
        }
      });
    } else {
      log.info("tableName exists in config");
      TableId tableId = TableId.of(projectId, datasetId, tableName);
      Table tableGranularData = bigQuery.getTable(tableId);
      Long lastModifiedTime = tableGranularData.getLastModifiedTime();
      lastModifiedTime = lastModifiedTime != null ? lastModifiedTime : tableGranularData.getCreationTime();
      log.info("Sync condition {} {}", lastModifiedTime, endTime);
      if (lastModifiedTime > endTime) {
        CacheKey cacheKey = new CacheKey(accountId, projectId, datasetId);
        gcpSyncInfo.get(cacheKey,
            key
            -> publishMessage(sourceCredentials, billingDataPipelineConfig.getGcpProjectId(),
                billingDataPipelineConfig.getGcpSyncPubSubTopic(), dataset.getLocation(), serviceAccountEmail,
                datasetId, projectId, accountId, connectorId, tableGranularData.getTableId().getTable()));
      }
    }
  }

  public List<ConnectorResponseDTO> getNextGenGCPConnectorResponses(String accountId) {
    List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
        ConnectorFilterPropertiesDTO.builder()
            .types(Arrays.asList(ConnectorType.GCP_CLOUD_COST))
            .ccmConnectorFilter(CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.BILLING)).build())
            .build();
    connectorFilterPropertiesDTO.setFilterType(FilterType.CONNECTOR);
    int page = 0;
    int size = 100;
    do {
      response = getConnectors(accountId, page, size, connectorFilterPropertiesDTO);
      if (response != null && isNotEmpty(response.getContent())) {
        nextGenConnectorResponses.addAll(response.getContent());
      }
      page++;
    } while (response != null && isNotEmpty(response.getContent()));
    log.info("Processing batch size of {} in GCP Sync Job", nextGenConnectorResponses.size());
    return nextGenConnectorResponses;
  }

  PageResponse getConnectors(
      String accountId, int page, int size, ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO) {
    return RestCallToNGManagerClientUtils.execute(
        connectorResourceClient.listConnectors(accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
  }

  // read the credential path from env variables
  public static ServiceAccountCredentials getCredentials(String googleCredentialPathSystemEnv) {
    String googleCredentialsPath = System.getenv(googleCredentialPathSystemEnv);
    checkFalse(isEmpty(googleCredentialsPath), "Missing environment variable for GCP credentials.");
    File credentialsFile = new File(googleCredentialsPath);
    ServiceAccountCredentials credentials = null;
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      log.error("Failed to find Google credential file for the CE service account in the specified path.", e);
    } catch (IOException e) {
      log.error("Failed to get Google credential file for the CE service account.", e);
    }
    return credentials;
  }

  public static Credentials getImpersonatedCredentials(
      ServiceAccountCredentials sourceCredentials, String impersonatedServiceAccount) {
    if (impersonatedServiceAccount == null) {
      return sourceCredentials;
    } else {
      return ImpersonatedCredentials.create(sourceCredentials, impersonatedServiceAccount, null,
          Arrays.asList("https://www.googleapis.com/auth/cloud-platform"), 300);
    }
  }

  public static boolean publishMessage(ServiceAccountCredentials sourceCredentials, String harnessProjectId,
      String topicId, String location, String serviceAccountEmail, String datasetId, String projectId, String accountId,
      String connectorId, String tableName) {
    TopicName topicName = TopicName.of(harnessProjectId, topicId);
    Publisher publisher = null;

    try {
      // Create a publisher instance with default settings bound to the topic
      publisher = Publisher.newBuilder(topicName)
                      .setCredentialsProvider(FixedCredentialsProvider.create(sourceCredentials))
                      .build();
      ImmutableMap<String, String> customAttributes = ImmutableMap.<String, String>builder()
                                                          .put(SERVICE_ACCOUNT, serviceAccountEmail)
                                                          .put(SOURCE_DATA_SET_ID, datasetId)
                                                          .put(SOURCE_GCP_PROJECT_ID, projectId)
                                                          .put(SOURCE_DATA_SET_REGION, location)
                                                          .put(ACCOUNT_ID, accountId)
                                                          .put(CONNECTOR_ID, connectorId)
                                                          .put(TABLE_NAME, tableName)
                                                          .build();
      ObjectMapper objectMapper = new ObjectMapper();
      String message = objectMapper.writeValueAsString(customAttributes);
      log.info("Sending GCP Sync Pub Sub Event with data: {}", message);
      ByteString data = ByteString.copyFromUtf8(message);
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
      // Once published, returns a server-assigned message id (unique within the topic)
      ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
      String messageId = messageIdFuture.get();
      log.info("Published a message with custom attributes: " + messageId);
    } catch (Exception ex) {
      log.error("Exception while publishing", ex);
    } finally {
      if (publisher != null) {
        // When finished with the publisher, shutdown to free up resources.
        publisher.shutdown();
        try {
          publisher.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
          log.error("InterruptedException ", e);
        }
      }
    }
    return true;
  }
}
