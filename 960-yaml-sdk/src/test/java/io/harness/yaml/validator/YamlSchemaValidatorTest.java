/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml.validator;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.yaml.TestClass;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidatorTypeCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;

public class YamlSchemaValidatorTest extends CategoryTest {
  YamlSchemaValidator yamlSchemaValidator;
  @Mock EnumCodeSchemaHandler enumCodeSchemaHandler;
  @Mock RequiredCodeSchemaHandler requiredCodeSchemaHandler;
  @Before
  public void setup() throws IOException {
    initMocks(this);
    final List<YamlSchemaRootClass> yamlSchemaRootClasses =
        Collections.singletonList(YamlSchemaRootClass.builder()
                                      .entityType(EntityType.CONNECTORS)
                                      .clazz(TestClass.ClassWhichContainsInterface.class)
                                      .build());
    yamlSchemaValidator =
        Mockito.spy(new YamlSchemaValidator(yamlSchemaRootClasses, enumCodeSchemaHandler, requiredCodeSchemaHandler));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testValidate() throws IOException {
    final String type1 = getYamlResource("validator/testyamltype1.yaml");
    final String type2 = getYamlResource("validator/testyamltype2.yaml");
    final String type1Incorrect = getYamlResource("validator/testType1Incorrect.yaml");
    final String type2Incorrect = getYamlResource("validator/testYamlType2Incorrect.yaml");
    String schema = getYamlResource("validator/schema.json");
    ObjectMapper objectMapper = new ObjectMapper();

    yamlSchemaValidator.populateSchemaInStaticMap(objectMapper.readTree(schema), EntityType.CONNECTORS);

    final Set<String> type1Val = yamlSchemaValidator.validate(type1, EntityType.CONNECTORS);
    assertThat(type1Val).isEmpty();

    final Set<String> type2Val = yamlSchemaValidator.validate(type2, EntityType.CONNECTORS);
    assertThat(type2Val).isEmpty();

    final Set<String> type1IncorrectVal = yamlSchemaValidator.validate(type1Incorrect, EntityType.CONNECTORS);
    assertThat(type1IncorrectVal).isNotEmpty();

    final Set<String> type2IncorrectVal = yamlSchemaValidator.validate(type2Incorrect, EntityType.CONNECTORS);
    assertThat(type2IncorrectVal).isNotEmpty();
  }

  private String getYamlResource(String resource) throws IOException {
    return IOUtils.resourceToString(resource, StandardCharsets.UTF_8, YamlSchemaValidatorTest.class.getClassLoader());
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testProcessValidationMessages() {
    String path1 = "path1";
    String path2 = "path2";
    List<ValidationMessage> validationMessages = new ArrayList<>();
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.ENUM, path1, "[Http]"));
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.ENUM, path1, "[ShellScript]"));
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.ENUM, path1, "[Barrier]"));

    List<ValidationMessage> responseList1 = new ArrayList<>();
    responseList1.add(ValidationMessage.of("type", ValidatorTypeCode.ENUM, path1, "[Http, ShellScript,Barrier]"));
    doReturn(responseList1).when(enumCodeSchemaHandler).handle(any());
    Set<ValidationMessage> processValidationMessages =
        yamlSchemaValidator.processValidationMessages(validationMessages, null);
    assertEquals(processValidationMessages.size(), 1);
    assertEquals(processValidationMessages.stream()
                     .map(ValidationMessage::getCode)
                     .filter(o -> o.equals(ValidatorTypeCode.ENUM.getErrorCode()))
                     .count(),
        1);
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.ENUM, path2, "[Barrier]"));

    responseList1.add(ValidationMessage.of("type", ValidatorTypeCode.ENUM, path2, "[Http, ShellScript,Barrier]"));
    doReturn(responseList1).when(enumCodeSchemaHandler).handle(any());

    // new location added. So processed messages must have 2 validation messages.
    processValidationMessages = yamlSchemaValidator.processValidationMessages(validationMessages, null);
    assertEquals(processValidationMessages.size(), 2);
    assertEquals(processValidationMessages.stream()
                     .map(ValidationMessage::getCode)
                     .filter(o -> o.equals(ValidatorTypeCode.ENUM.getErrorCode()))
                     .count(),
        2);
    assertEquals(processValidationMessages.stream()
                     .map(ValidationMessage::getCode)
                     .filter(o -> o.equals(ValidatorTypeCode.TYPE.getErrorCode()))
                     .count(),
        0);
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.TYPE, path2, "[Barrier]"));
    // Same location added but with different ValidatorTypeCode. So it should come separate after processing.
    processValidationMessages = yamlSchemaValidator.processValidationMessages(validationMessages, null);
    assertEquals(processValidationMessages.size(), 3);
    assertEquals(processValidationMessages.stream()
                     .map(ValidationMessage::getCode)
                     .filter(o -> o.equals(ValidatorTypeCode.ENUM.getErrorCode()))
                     .count(),
        2);
    assertEquals(processValidationMessages.stream()
                     .map(ValidationMessage::getCode)
                     .filter(o -> o.equals(ValidatorTypeCode.TYPE.getErrorCode()))
                     .count(),
        1);

    // Adding new ValidatorTypeCode.
    validationMessages.add(ValidationMessage.of("type", ValidatorTypeCode.REQUIRED, path1, "template"));
    doReturn(Collections.singletonList(ValidationMessage.of("type", ValidatorTypeCode.REQUIRED, path1, "template")))
        .when(requiredCodeSchemaHandler)
        .handle(any(), any());
    processValidationMessages = yamlSchemaValidator.processValidationMessages(validationMessages, null);
    assertEquals(processValidationMessages.size(), 4);
    assertEquals(processValidationMessages.stream()
                     .map(ValidationMessage::getCode)
                     .filter(o -> o.equals(ValidatorTypeCode.ENUM.getErrorCode()))
                     .count(),
        2);
    assertEquals(processValidationMessages.stream()
                     .map(ValidationMessage::getCode)
                     .filter(o -> o.equals(ValidatorTypeCode.TYPE.getErrorCode()))
                     .count(),
        1);
    assertEquals(processValidationMessages.stream()
                     .map(ValidationMessage::getCode)
                     .filter(o -> o.equals(ValidatorTypeCode.REQUIRED.getErrorCode()))
                     .count(),
        1);
  }
}
