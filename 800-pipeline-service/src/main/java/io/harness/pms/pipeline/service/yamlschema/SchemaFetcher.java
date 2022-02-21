/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service.yamlschema;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.SchemaCacheKey;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.pms.pipeline.service.yamlschema.cache.PartialSchemaDTOWrapperValue;
import io.harness.pms.pipeline.service.yamlschema.cache.SchemaCacheUtils;
import io.harness.pms.pipeline.service.yamlschema.cache.YamlSchemaDetailsWrapperValue;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaDetailsWrapper;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import java.util.List;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
@Singleton
public class SchemaFetcher {
  private static final Duration THRESHOLD_PROCESS_DURATION = Duration.ofSeconds(5);
  @Inject @Named("schemaDetailsCache") Cache<SchemaCacheKey, YamlSchemaDetailsWrapperValue> schemaDetailsCache;
  @Inject @Named("partialSchemaCache") Cache<SchemaCacheKey, PartialSchemaDTOWrapperValue> schemaCache;
  @Inject private SchemaGetterFactory schemaGetterFactory;

  /**
   * Schema is taken from cache, so every modification will affect cache value.
   * In order to avoid that, we do deep copy of cached object
   */
  @Nullable
  public List<PartialSchemaDTO> fetchSchema(
      String accountId, ModuleType moduleType, List<YamlSchemaWithDetails> yamlSchemaWithDetailsList) {
    log.info("[PMS] Fetching schema for {}", moduleType.name());
    long startTs = System.currentTimeMillis();
    try {
      SchemaCacheKey schemaCacheKey =
          SchemaCacheKey.builder().accountIdentifier(accountId).moduleType(moduleType).build();

      if (schemaCache.containsKey(schemaCacheKey)) {
        return SchemaCacheUtils.getPartialSchemaDTOList(schemaCache.get(schemaCacheKey));
      }

      SchemaGetter schemaGetter = schemaGetterFactory.obtainGetter(accountId, moduleType);
      List<PartialSchemaDTO> partialSchemaDTOS = schemaGetter.getSchema(yamlSchemaWithDetailsList);

      schemaCache.put(schemaCacheKey, SchemaCacheUtils.getPartialSchemaWrapperValue(partialSchemaDTOS));

      log.info("[PMS] Successfully fetched schema for {}", moduleType.name());
      logWarnIfExceedsThreshold(moduleType, startTs);

      return partialSchemaDTOS;
    } catch (Exception e) {
      log.warn(format("[PMS] Unable to get %s schema", moduleType.name()), e);
      return null;
    }
  }

  public YamlSchemaDetailsWrapper fetchSchemaDetail(String accountId, ModuleType moduleType) {
    log.info("[PMS] Fetching schema information for {}", moduleType.name());
    try {
      SchemaCacheKey schemaCacheKey =
          SchemaCacheKey.builder().accountIdentifier(accountId).moduleType(moduleType).build();
      if (schemaDetailsCache.containsKey(schemaCacheKey)) {
        return SchemaCacheUtils.toYamlSchemaDetailsWrapper(schemaDetailsCache.get(schemaCacheKey));
      }

      SchemaGetter schemaGetter = schemaGetterFactory.obtainGetter(accountId, moduleType);
      YamlSchemaDetailsWrapper yamlSchemaDetailsWrapper = schemaGetter.getSchemaDetails();
      schemaDetailsCache.put(schemaCacheKey, SchemaCacheUtils.toYamlSchemaDetailCacheValue(yamlSchemaDetailsWrapper));
      return yamlSchemaDetailsWrapper;
    } catch (Exception e) {
      log.warn(format("[PMS] Unable to get %s schema information", moduleType.name()), e);
      return null;
    }
  }

  public void invalidateAllCache() {
    log.info("[PMS] Invalidating yaml schema cache");
    schemaCache.clear();
    schemaDetailsCache.clear();
    log.info("[PMS] Yaml schema cache was successfully invalidated");
  }

  private void logWarnIfExceedsThreshold(ModuleType moduleType, long startTs) {
    Duration processDuration = Duration.ofMillis(System.currentTimeMillis() - startTs);
    if (THRESHOLD_PROCESS_DURATION.compareTo(processDuration) < 0) {
      log.warn("[PMS] Fetching schema for {} service took {}s which is more than threshold of {}s", moduleType.name(),
          processDuration.getSeconds(), THRESHOLD_PROCESS_DURATION.getSeconds());
    }
  }

  // TODO: introduce cache while fetching step schema
  public JsonNode fetchStepYamlSchema(String accountId, String orgIdentifier, String projectIdentifier, Scope scope,
      EntityType entityType, String yamlGroup, List<YamlSchemaWithDetails> yamlSchemaWithDetailsList) {
    SchemaGetter schemaGetter = schemaGetterFactory.obtainGetter(accountId, entityType.getEntityProduct());
    return schemaGetter.fetchStepYamlSchema(
        orgIdentifier, projectIdentifier, scope, entityType, yamlGroup, yamlSchemaWithDetailsList);
  }
}
