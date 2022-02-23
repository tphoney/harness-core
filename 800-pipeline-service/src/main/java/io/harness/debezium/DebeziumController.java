/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.harness.persistence.PersistentEntity;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Singleton;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.serde.DebeziumSerdes;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;

@Singleton
@Slf4j
public class DebeziumController implements Runnable {
  private static final String MONGO_DB_CONNECTOR = "io.debezium.connector.mongodb.MongoDbConnector";
  private static final String CONNECTOR_NAME = "name";
  private static final String OFFSET_STORAGE = "offset.storage";
  private static final String OFFSET_STORAGE_FILE_FILENAME = "offset.storage.file.filename";
  private static final String OFFSET_STORAGE_COLLECTION = "offset.storage.topic";
  private static final String KEY_CONVERTER_SCHEMAS_ENABLE = "key.converter.schemas.enable";
  private static final String VALUE_CONVERTER_SCHEMAS_ENABLE = "value.converter.schemas.enable";
  private static final String OFFSET_FLUSH_INTERVAL_MS = "offset.flush.interval.ms";
  private static final String CONNECTOR_CLASS = "connector.class";
  private static final String MONGODB_HOSTS = "mongodb.hosts";
  private static final String MONGODB_NAME = "mongodb.name";
  private static final String MONGODB_USER = "mongodb.user";
  private static final String MONGODB_PASSWORD = "mongodb.password";
  private static final String MONGODB_SSL_ENABLED = "mongodb.ssl.enabled";
  private static final String DATABASE_INCLUDE_LIST = "database.include.list";
  private static final String COLLECTION_INCLUDE_LIST = "collection.include.list";
  private static final String TRANSFORMS = "transforms";
  private static final String TRANSFORMS_UNWRAP_TYPE = "transforms.unwrap.type";
  private static final String TRANSFORMS_UNWRAP_DROP_TOMBSTONES = "transforms.unwrap.drop.tombstones";
  private static final String TRANSFORMS_UNWRAP_ADD_HEADERS = "transforms.unwrap.add.headers";
  private static final String DEBEZIUM_CONNECTOR_MONGODB_TRANSFORMS_EXTRACT_NEW_DOCUMENT_STATE =
      "io.debezium.connector.mongodb.transforms.ExtractNewDocumentState";
  private static final String UNKNOWN_PROPERTIES_IGNORED = "unknown.properties.ignored";
  private static final String PLAN_EXECUTIONS_SUMMARY = "planExecutionsSummary";
  private Map<String, ChangeConsumer<? extends PersistentEntity>> collectionToConsumerMap;
  protected final ExecutorService executorService =
      Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("debezium-controller-class").build());
  private DebeziumConfig debeziumConfig;

  public DebeziumController(DebeziumConfig debeziumConfig) {
    this.debeziumConfig = debeziumConfig;
    ChangeConsumer<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntityChangeConsumer =
        new PipelineExecutionSummaryConsumer();
    collectionToConsumerMap = new HashMap<>();
    collectionToConsumerMap.put(PLAN_EXECUTIONS_SUMMARY, pipelineExecutionSummaryEntityChangeConsumer);
  }

  protected DebeziumChangeConsumer buildDebeziumChangeConsumer() {
    Map<String, Deserializer<? extends PersistentEntity>> collectionToDeserializerMap = new HashMap<>();
    Map<String, String> valueDeserializerConfig = Maps.newHashMap(ImmutableMap.of(UNKNOWN_PROPERTIES_IGNORED, "true"));
    // configuring id deserializer
    Serde<String> idSerde = DebeziumSerdes.payloadJson(String.class);
    idSerde.configure(Maps.newHashMap(ImmutableMap.of("from.field", "id")), true);
    Deserializer<String> idDeserializer = idSerde.deserializer();
    // configuring pipeline deserializer
    Serde<PipelineExecutionSummaryEntity> pipelineSerde =
        DebeziumSerdes.payloadJson(PipelineExecutionSummaryEntity.class);
    pipelineSerde.configure(valueDeserializerConfig, false);
    collectionToDeserializerMap.put(PLAN_EXECUTIONS_SUMMARY, pipelineSerde.deserializer());
    // configuring debezium change consumer
    return new DebeziumChangeConsumer(idDeserializer, collectionToDeserializerMap, collectionToConsumerMap);
  }

  @Override
  public void run() {
    DebeziumEngine<ChangeEvent<String, String>> debeziumEngine;
    DebeziumChangeConsumer debeziumChangeConsumer = buildDebeziumChangeConsumer();
    debeziumEngine = getEngine(debeziumConfig, debeziumChangeConsumer);
    executorService.submit(debeziumEngine);
  }

  protected DebeziumEngine<ChangeEvent<String, String>> getEngine(
      DebeziumConfig debeziumConfig, DebeziumChangeConsumer debeziumChangeConsumer) {
    File offsetStorageTempFile = null;
    try {
      offsetStorageTempFile = File.createTempFile("offsets_", ".dat");
    } catch (IOException e) {
      e.printStackTrace();
    }
    Properties props = new Properties();
    String offsetCollection = "offset_collection";
    props.setProperty(CONNECTOR_NAME, debeziumConfig.getConnectorName());
    props.setProperty(OFFSET_STORAGE, "org.apache.kafka.connect.storage.FileOffsetBackingStore");
    props.setProperty(OFFSET_STORAGE_FILE_FILENAME, offsetStorageTempFile.getAbsolutePath());
    props.setProperty(OFFSET_STORAGE_COLLECTION, offsetCollection);
    props.setProperty(KEY_CONVERTER_SCHEMAS_ENABLE, debeziumConfig.getKeyConverterSchemasEnable());
    props.setProperty(VALUE_CONVERTER_SCHEMAS_ENABLE, debeziumConfig.getValueConverterSchemasEnable());
    props.setProperty(OFFSET_FLUSH_INTERVAL_MS, debeziumConfig.getOffsetFlushIntervalMillis());

    /* begin connector properties */
    props.setProperty(CONNECTOR_CLASS, MONGO_DB_CONNECTOR);
    props.setProperty(MONGODB_HOSTS, debeziumConfig.getMongodbHosts());
    props.setProperty(MONGODB_NAME, debeziumConfig.getMongodbName());
    Optional.ofNullable(debeziumConfig.getMongodbUser())
        .filter(x -> !x.isEmpty())
        .ifPresent(x -> props.setProperty(MONGODB_USER, x));
    Optional.ofNullable(debeziumConfig.getMongodbPassword())
        .filter(x -> !x.isEmpty())
        .ifPresent(x -> props.setProperty(MONGODB_PASSWORD, x));
    props.setProperty(MONGODB_SSL_ENABLED, debeziumConfig.getSslEnabled());
    props.setProperty(DATABASE_INCLUDE_LIST, debeziumConfig.getDatabaseIncludeList());
    props.setProperty(COLLECTION_INCLUDE_LIST, debeziumConfig.getCollectionIncludeList());
    props.setProperty(TRANSFORMS, "unwrap");
    props.setProperty(TRANSFORMS_UNWRAP_TYPE, DEBEZIUM_CONNECTOR_MONGODB_TRANSFORMS_EXTRACT_NEW_DOCUMENT_STATE);
    props.setProperty(TRANSFORMS_UNWRAP_DROP_TOMBSTONES, "false");
    props.setProperty(TRANSFORMS_UNWRAP_ADD_HEADERS, "op");

    return DebeziumEngine.create(Json.class).using(props).notifying(debeziumChangeConsumer).build();
  }
}
