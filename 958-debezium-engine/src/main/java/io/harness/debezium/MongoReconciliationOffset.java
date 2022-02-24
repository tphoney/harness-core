/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import static io.harness.ng.DbAliases.PMS;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

@FieldNameConstants(innerTypeName = "keys")
@Data
@Builder
@Entity(value = "mongoReconciliationOffset", noClassnameStored = true)
@StoreIn(PMS)
public class MongoReconciliationOffset implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id private String id;
  private byte[] key;
  private byte[] value;
  @FdIndex @CreatedDate private long createdAt;
}
