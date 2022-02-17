/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.serde.DebeziumSerdes;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.redisson.api.RLock;

@Singleton
@Slf4j
public class DebeziumController implements Runnable {
  private final PersistentLocker persistentLocker;
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
  private final Map<String, ChangeConsumer<? extends PipelineExecutionSummaryEntity>> collectionToConsumerMap;
  protected final DebeziumConfiguration debeziumConfiguration;
  protected final ExecutorService executorService;

  public DebeziumController(PersistentLocker persistentLocker,
      Map<String, ChangeConsumer<? extends PipelineExecutionSummaryEntity>> collectionToConsumerMap,
      DebeziumConfiguration debeziumConfiguration, ExecutorService executorService) {
    this.persistentLocker = persistentLocker;
    this.collectionToConsumerMap = collectionToConsumerMap;
    this.debeziumConfiguration = debeziumConfiguration;
    this.executorService = executorService;
  }

  protected DebeziumChangeConsumer buildDebeziumChangeConsumer() {
    Map<String, Deserializer<? extends PipelineExecutionSummaryEntity>> collectionToDeserializerMap = new HashMap<>();
    Map<String, String> valueDeserializerConfig = Maps.newHashMap(ImmutableMap.of(UNKNOWN_PROPERTIES_IGNORED, "true"));
    // configuring id deserializer
    Serde<String> idSerde = DebeziumSerdes.payloadJson(String.class);
    idSerde.configure(Maps.newHashMap(ImmutableMap.of("from.field", "id")), true);
    Deserializer<String> idDeserializer = idSerde.deserializer();
    // configuring pipeline deserializer
    Serde<PipelineExecutionSummaryEntity> pipelineSerde =
        DebeziumSerdes.payloadJson(PipelineExecutionSummaryEntity.class);
    pipelineSerde.configure(valueDeserializerConfig, false);
    collectionToDeserializerMap.put("pipeline", pipelineSerde.deserializer());
    // configuring debezium
    return new DebeziumChangeConsumer(idDeserializer, collectionToDeserializerMap, collectionToConsumerMap);
  }

  @Override
  public void run() {
    DebeziumEngine<ChangeEvent<String, String>> debeziumEngine = null;
    try (AcquiredLock<?> debeziumLock = acquireLock(true)) {
      if (debeziumLock == null) {
        return;
      }
      log.info("Acquired lock, initiating sync.");
      RLock rLock = (RLock) debeziumLock.getLock();
      DebeziumChangeConsumer debeziumChangeConsumer = buildDebeziumChangeConsumer();
      debeziumEngine = getEngine(debeziumConfiguration.getDebeziumConfig(), debeziumChangeConsumer);
      Future<?> debeziumEngineFuture = executorService.submit(debeziumEngine);

      while (!debeziumEngineFuture.isDone() && rLock.isHeldByCurrentThread()) {
        log.info("primary lock remaining ttl {}, isHeldByCurrentThread {}, holdCount {}, name {}",
            rLock.remainTimeToLive(), rLock.isHeldByCurrentThread(), rLock.getHoldCount(), rLock.getName());
        TimeUnit.SECONDS.sleep(30);
      }
      log.warn("The primary sync debezium engine has unexpectedly stopped or the lock is no longer held");

    } catch (InterruptedException e) {
      log.warn("Thread interrupted, stopping primary aggregator sync", e);
    } catch (Exception e) {
      log.error("Primary sync stopped due to exception", e);
    } finally {
      try {
        if (debeziumEngine != null) {
          debeziumEngine.close();
          TimeUnit.SECONDS.sleep(10);
        }
      } catch (IOException e) {
        log.error("Failed to close debezium engine due to IO exception", e);
      } catch (InterruptedException e) {
        log.warn("Interrupted while waiting for debezium engine to close", e);
      } catch (Exception e) {
        log.error("Failed to close debezium engine due to unexpected exception", e);
      }
    }
  }

  protected DebeziumEngine<ChangeEvent<String, String>> getEngine(
      DebeziumConfig debeziumConfig, DebeziumChangeConsumer debeziumChangeConsumer) {
    Properties props = new Properties();
    String offsetCollection = "offset_collection";
    props.setProperty(CONNECTOR_NAME, debeziumConfig.getConnectorName());
    props.setProperty(OFFSET_STORAGE, MongoOffsetBackingStore.class.getName());
    props.setProperty(OFFSET_STORAGE_FILE_FILENAME, debeziumConfig.getOffsetStorageFileName());
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

  protected AcquiredLock<?> acquireLock(boolean retryIndefinitely) throws InterruptedException {
    AcquiredLock<?> debeziumLock = null;
    String lockIdentifier = "debezium_lock";
    do {
      try {
        log.info("Trying to acquire {} lock with 5 seconds timeout", lockIdentifier);
        debeziumLock =
            persistentLocker.tryToAcquireInfiniteLockWithPeriodicRefresh(lockIdentifier, Duration.ofSeconds(5));
      } catch (Exception ex) {
        log.warn("Unable to get {} lock, due to the exception. Will retry again", lockIdentifier, ex);
      }
      if (debeziumLock == null) {
        TimeUnit.SECONDS.sleep(120);
      }
    } while (debeziumLock == null && retryIndefinitely);
    return debeziumLock;
  }
}
