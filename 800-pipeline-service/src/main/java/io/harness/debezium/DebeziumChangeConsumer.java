/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.harness.exception.DuplicateFieldException;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Singleton;
import io.debezium.embedded.EmbeddedEngineChangeEvent;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@Slf4j
public class DebeziumChangeConsumer implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
  private static final String OP_FIELD = "__op";
  private final Deserializer<String> idDeserializer;
  private final Retry retry;
  private final Map<String, Deserializer<? extends PersistentEntity>> collectionToDeserializerMap;
  private final Map<String, ChangeConsumer<? extends PersistentEntity>> collectionToConsumerMap;

  public DebeziumChangeConsumer(Deserializer<String> idDeserializer,
      Map<String, Deserializer<? extends PersistentEntity>> collectionToDeserializerMap,
      Map<String, ChangeConsumer<? extends PersistentEntity>> collectionToConsumerMap) {
    this.idDeserializer = idDeserializer;
    this.collectionToDeserializerMap = collectionToDeserializerMap;
    this.collectionToConsumerMap = collectionToConsumerMap;
    IntervalFunction intervalFunction = IntervalFunction.ofExponentialBackoff(1000, 2);
    RetryConfig retryConfig = RetryConfig.custom()
                                  .ignoreExceptions(DuplicateKeyException.class, DuplicateFieldException.class)
                                  .intervalFunction(intervalFunction)
                                  .maxAttempts(10)
                                  .build();
    retry = Retry.of("debeziumEngineRetry", retryConfig);
  }

  private boolean handleEvent(ChangeEvent<String, String> changeEvent) {
    String id = idDeserializer.deserialize(null, changeEvent.key().getBytes());
    Optional<String> collectionName = getCollectionName(changeEvent.destination());
    Optional<OpType> opType =
        getOperationType(((EmbeddedEngineChangeEvent<String, String>) changeEvent).sourceRecord());
    if (opType.isPresent() && collectionName.isPresent()) {
      log.info("Handling {} event for entity: {}.{}", opType.get(), collectionName.get(), id);
      ChangeConsumer<? extends PersistentEntity> changeConsumer = collectionToConsumerMap.get(collectionName.get());
      changeConsumer.consumeEvent(opType.get(), id, deserialize(collectionName.get(), changeEvent));
    }
    return true;
  }

  @Override
  public void handleBatch(List<ChangeEvent<String, String>> changeEvents,
      DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> recordCommitter) throws InterruptedException {
    for (ChangeEvent<String, String> changeEvent : changeEvents) {
      try {
        retry.executeSupplier(() -> handleEvent(changeEvent));
      } catch (Exception exception) {
        log.error(
            String.format(
                "Exception caught when trying to process event: [%s]. Retrying this event with exponential backoff now...",
                changeEvent),
            exception);
      }
      recordCommitter.markProcessed(changeEvent);
    }
    recordCommitter.markBatchFinished();
  }

  private <T extends PersistentEntity> T deserialize(String collectionName, ChangeEvent<String, String> changeEvent) {
    return (T) collectionToDeserializerMap.get(collectionName).deserialize(null, getValue(changeEvent));
  }

  private byte[] getValue(ChangeEvent<String, String> changeEvent) {
    return changeEvent.value() == null ? null : changeEvent.value().getBytes();
  }

  private Optional<String> getCollectionName(String sourceRecordTopic) {
    return Optional.of(sourceRecordTopic.split("\\.")).filter(x -> x.length >= 2).map(x -> x[2]);
  }

  private Optional<OpType> getOperationType(SourceRecord sourceRecord) {
    return Optional.ofNullable(sourceRecord.headers().lastWithName(OP_FIELD))
        .flatMap(x -> OpType.fromString((String) x.value()));
  }
}
