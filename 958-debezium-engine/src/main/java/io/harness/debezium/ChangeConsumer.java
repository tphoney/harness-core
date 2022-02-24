/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

public interface ChangeConsumer<T> {
  void consumeUpdateEvent(String id, T updatedEntity);

  void consumeDeleteEvent(String id);

  void consumeCreateEvent(String id, T createdEntity);

  default void consumeEvent(OpType opType, String id, T entity) {
    switch (opType) {
      case SNAPSHOT:
      case CREATE:
        consumeCreateEvent(id, entity);
        break;
      case UPDATE:
        consumeUpdateEvent(id, entity);
        break;
      case DELETE:
        consumeDeleteEvent(id);
        break;
      default:
        break;
    }
  }
}
