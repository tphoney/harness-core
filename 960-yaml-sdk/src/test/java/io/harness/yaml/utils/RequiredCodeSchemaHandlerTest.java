/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.utils;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.validator.RequiredCodeSchemaHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidatorTypeCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RequiredCodeSchemaHandlerTest extends CategoryTest {
  private final RequiredCodeSchemaHandler requiredCodeSchemaHandler = new RequiredCodeSchemaHandler();

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testHandle() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    String path = "path";
    List<ValidationMessage> validationMessages = new ArrayList<>();
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.REQUIRED, path, "template"));
    JsonNode jsonNode = objectMapper.readTree("{\"path\":{\"name\": \"value\"}}");
    List<ValidationMessage> response = requiredCodeSchemaHandler.handle(validationMessages, jsonNode);
    assertEquals(response.size(), 1);

    jsonNode = objectMapper.readTree("{\"path\":{\"type\": \"value\"}}");
    response = requiredCodeSchemaHandler.handle(validationMessages, jsonNode);
    assertEquals(response.size(), 0);

    validationMessages.clear();
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.REQUIRED, path, "id"));
    jsonNode = objectMapper.readTree("{\"path\":{\"name\": \"value\"}}");
    response = requiredCodeSchemaHandler.handle(validationMessages, jsonNode);
    assertEquals(response.size(), 1);

    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.REQUIRED, path, "name"));
    response = requiredCodeSchemaHandler.handle(validationMessages, jsonNode);
    assertEquals(response.size(), 2);
    jsonNode = objectMapper.readTree("{\"path\":{\"type\": \"value\"}}");
    response = requiredCodeSchemaHandler.handle(validationMessages, jsonNode);
    assertEquals(response.size(), 2);
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.REQUIRED, path, "template"));
    response = requiredCodeSchemaHandler.handle(validationMessages, jsonNode);
    assertEquals(response.size(), 2);
  }
}
