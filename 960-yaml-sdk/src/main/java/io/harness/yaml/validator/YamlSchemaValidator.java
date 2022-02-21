/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.validator;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.yaml.schema.beans.SchemaConstants.PARALLEL_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.PIPELINE_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.STAGES_NODE;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;
import io.harness.yaml.utils.SchemaValidationUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidatorTypeCode;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class YamlSchemaValidator {
  public static Map<EntityType, JsonSchema> schemas = new HashMap<>();
  public static final String ENUM_SCHEMA_ERROR_CODE = ValidatorTypeCode.ENUM.getErrorCode();
  public static final String REQUIRED_SCHEMA_ERROR_CODE = ValidatorTypeCode.REQUIRED.getErrorCode();
  ObjectMapper mapper;
  List<YamlSchemaRootClass> yamlSchemaRootClasses;
  EnumCodeSchemaHandler enumCodeSchemaHandler;
  RequiredCodeSchemaHandler requiredCodeSchemaHandler;

  @Inject
  public YamlSchemaValidator(List<YamlSchemaRootClass> yamlSchemaRootClasses,
      EnumCodeSchemaHandler enumCodeSchemaHandler, RequiredCodeSchemaHandler requiredCodeSchemaHandler) {
    mapper = new ObjectMapper(new YAMLFactory());
    this.yamlSchemaRootClasses = yamlSchemaRootClasses;
    this.enumCodeSchemaHandler = enumCodeSchemaHandler;
    this.requiredCodeSchemaHandler = requiredCodeSchemaHandler;
  }

  /**
   * @param yaml       The yaml String which is to be validated against schema of entity.
   * @param entityType The entityType against which yaml string needs to be validated.
   * @return Set of error messages. Will be empty if we don't encounter any error.
   * @throws IOException when yaml string could't be parsed.
   */
  public Set<String> validate(String yaml, EntityType entityType) throws IOException {
    if (!schemas.containsKey(entityType)) {
      throw new InvalidRequestException("No schema found for entityType.");
    }
    JsonSchema schema = schemas.get(entityType);
    return validate(yaml, schema);
  }

  public Set<String> validate(String yaml, JsonSchema schema) throws IOException {
    JsonNode jsonNode = mapper.readTree(yaml);
    Set<ValidationMessage> validateMsg = schema.validate(jsonNode);
    return validateMsg.stream().map(ValidationMessage::getMessage).collect(Collectors.toSet());
  }

  public Set<String> validate(String yaml, String stringSchema, boolean shouldValidateParallelStageCount,
      int allowedParallelStages) throws IOException {
    JsonNode jsonNode = mapper.readTree(yaml);
    try {
      validateParallelStagesCount(jsonNode, shouldValidateParallelStageCount, allowedParallelStages);
    } catch (Exception e) {
      return Collections.singleton(e.getMessage());
    }
    JsonSchemaFactory factory =
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).build();
    JsonSchema schema = factory.getSchema(stringSchema);
    Set<ValidationMessage> validateMsg = schema.validate(jsonNode);
    if (!validateMsg.isEmpty()) {
      log.error(validateMsg.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("\n")));
    }
    Set<ValidationMessage> processValidationMessages = processValidationMessages(validateMsg, jsonNode);
    return processValidationMessages.stream().map(ValidationMessage::getMessage).collect(Collectors.toSet());
  }

  protected void validateParallelStagesCount(
      JsonNode yaml, boolean shouldValidateParallelStageCount, int allowedParallelStages) {
    if (shouldValidateParallelStageCount) {
      return;
    }
    ArrayNode stages = (ArrayNode) yaml.get(PIPELINE_NODE).get(STAGES_NODE);
    for (int index = 0; index < stages.size(); index++) {
      JsonNode parallelNode = stages.get(index).get(PARALLEL_NODE);
      if (parallelNode == null) {
        continue;
      }
      if (parallelNode.size() > allowedParallelStages) {
        throw new InvalidRequestException(String.format(
            "More than %s parallel stages provided at $.pipeline.stages[%s].parallel. \nPlease check the pipeline and ensure that parallel stages count does not exceed allowed value of %s.",
            allowedParallelStages, index, allowedParallelStages));
      }
    }
  }

  public void populateSchemaInStaticMap(JsonNode schema, EntityType entityType) {
    JsonSchemaFactory factory =
        JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).build();
    try {
      final JsonSchema jsonSchema = factory.getSchema(schema);
      schemas.put(entityType, jsonSchema);
    } catch (Exception e) {
      throw new InvalidRequestException(String.format("Couldn't parse schema for entity: %s", entityType), e);
    }
  }

  /**
   * Initialises a static map which will help in fast validation against a schema.
   *
   */
  public void initializeValidatorWithSchema(Map<EntityType, JsonNode> schemas) {
    schemas.forEach((entityType, jsonNode) -> populateSchemaInStaticMap(jsonNode, entityType));
  }

  protected Set<ValidationMessage> processValidationMessages(
      Collection<ValidationMessage> validationMessages, JsonNode jsonNode) {
    Map<String, List<ValidationMessage>> validationMessageCodeMap =
        SchemaValidationUtils.getValidationMessageCodeMap(validationMessages);
    Set<ValidationMessage> validationMessageList = new HashSet<>();
    for (Map.Entry<String, List<ValidationMessage>> validationEntry : validationMessageCodeMap.entrySet()) {
      if (validationEntry.getKey().equals(ENUM_SCHEMA_ERROR_CODE)) {
        validationMessageList.addAll(enumCodeSchemaHandler.handle(validationEntry.getValue()));
      } else if (validationEntry.getKey().equals(REQUIRED_SCHEMA_ERROR_CODE)) {
        validationMessageList.addAll(requiredCodeSchemaHandler.handle(validationEntry.getValue(), jsonNode));
      } else {
        validationMessageList.addAll(validationEntry.getValue());
      }
    }
    return validationMessageList;
  }
}
