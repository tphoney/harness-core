/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.oas;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.testing.TestExecution;

import com.google.common.collect.HashMultimap;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public abstract class OASModule extends AbstractModule {
  public static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  public static final String ACCOUNT_ID = "accountId";
  public static final String EXCLUSION_FILE = "/oas/exclusion-file";
  public static final String ENDPOINT_EXCLUSION = "endpoint-exclusion";
  public static final String DTO_EXCLUSION = "dto-exclusion";

  public abstract Collection<Class<?>> getResourceClasses();

  public void testOASAdoption(Collection<Class<?>> classes) {
    List<String> endpointsWithoutAccountParam = new ArrayList<>();
    List<String> dtoWithoutDescriptionToField = new ArrayList<>();

    if (classes == null) {
      return;
    }
    for (Class<?> clazz : classes) {
      if (clazz.isAnnotationPresent(Tag.class)) {
        for (final Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isPublic(method.getModifiers()) && method.isAnnotationPresent(Operation.class)
              && !isHiddenApi(method)) {
            checkParamAnnotation(method);
            dtoWithoutDescriptionToField.addAll(checkDtoFieldsDescription(method));
            endpointsWithoutAccountParam.addAll(checkAccountIdentifierParam(method));
          }
        }
      }
    }

    finalAssertion(dtoWithoutDescriptionToField, DTO_EXCLUSION,
        "All the fields in DTO should have description, but found below : ");
    finalAssertion(endpointsWithoutAccountParam, ENDPOINT_EXCLUSION,
        "There should not be endpoints without account identifier as path OR query param, but found below : ");
  }
  private void finalAssertion(List<String> listFromCheck, String exclusionType, String message) {
    if (!listFromCheck.isEmpty()) {
      List<String> exclusionDtoList = new ArrayList<>();
      Map<String, Collection<String>> map = exclude(exclusionType).asMap();
      for (Map.Entry<String, Collection<String>> entry : map.entrySet()) {
        if (entry.getKey().equals(exclusionType)) {
          exclusionDtoList.addAll(entry.getValue());
        }
      }
      listFromCheck.removeAll(exclusionDtoList);
      assertThat(listFromCheck.isEmpty()).as(getDetails(listFromCheck, message)).isTrue();
    }
  }

  private String getDetails(List<String> endpointsWithoutAccountParam, String message) {
    StringBuilder details = new StringBuilder(message);
    endpointsWithoutAccountParam.forEach(entry -> details.append("\n ").append(entry));
    return details.toString();
  }

  private HashMultimap<String, String> exclude(String key) {
    HashMultimap<String, String> excludedDTOsMap = HashMultimap.create();
    List<String> excludedDTOs = new ArrayList<>();
    try (InputStream in = getClass().getResourceAsStream(EXCLUSION_FILE)) {
      if (in == null) {
        log.info("No " + key + " configured for OAS validations.");
        return excludedDTOsMap;
      }
      excludedDTOs = IOUtils.readLines(in, "UTF-8");

      for (String excludedDTO : excludedDTOs) {
        String[] parts = excludedDTO.split(":");
        String exclusionType = parts[0].trim();
        String exclusionClassOrMethod = parts[1].trim();
        if (exclusionType.equals(key)) {
          excludedDTOsMap.put(exclusionType, exclusionClassOrMethod);
        }
      }
    } catch (Exception e) {
      log.error("Failed to load " + key + " file {} with error: {}", EXCLUSION_FILE, e.getMessage());
    }
    return excludedDTOsMap;
  }

  private boolean isHiddenApi(Method method) {
    Annotation[] methodAnnotations = method.getDeclaredAnnotations();
    for (Annotation annotation : methodAnnotations) {
      if (annotation.annotationType() == Operation.class) {
        Operation operation = (Operation) annotation;
        return operation.hidden();
      }
    }
    return false;
  }

  private void checkParamAnnotation(Method method) {
    Annotation[][] parametersAnnotationsList = method.getParameterAnnotations();
    for (Annotation[] annotations : parametersAnnotationsList) {
      for (Annotation annotation : annotations) {
        if (annotation.annotationType() == Parameter.class) {
          Parameter parameter = (Parameter) annotation;
          assertThat(parameter.description()).isNotEmpty();
        } else if (annotation.annotationType() == RequestBody.class) {
          RequestBody requestBody = (RequestBody) annotation;
          assertThat(requestBody.description()).isNotEmpty();
        }
      }
    }
  }

  private List<String> recursiveDtoFieldDescriptionCheck(Class<?> clazz) {
    List<String> dtoWithoutDescriptionToField = new ArrayList<>();
    Field[] listOfFields = clazz.getDeclaredFields();
    for (Field field : listOfFields) {
      if (field.getType() == field.getDeclaringClass()) {
        return dtoWithoutDescriptionToField;
      }

      if (field.getType() == List.class || field.getType() == Set.class) {
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Class<?> classInList = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        if (classInList.isAnnotationPresent(Schema.class)) {
          dtoWithoutDescriptionToField.addAll(recursiveDtoFieldDescriptionCheck(classInList));
        }
      }

      if (field.getType().isAnnotationPresent(Schema.class)) {
        dtoWithoutDescriptionToField.addAll(recursiveDtoFieldDescriptionCheck(field.getType()));
      } else {
        if (!field.isAnnotationPresent(Schema.class)) {
          if (!dtoWithoutDescriptionToField.contains(clazz.getName())) {
            dtoWithoutDescriptionToField.add(clazz.getName());
          }
        } else {
          Annotation[] annotations = field.getAnnotations();
          for (Annotation annotation : annotations) {
            if (annotation.annotationType() == Schema.class) {
              Schema schema = (Schema) annotation;
              if (schema.description().isEmpty()) {
                dtoWithoutDescriptionToField.add(clazz.getName());
              }
            }
          }
        }
      }
    }
    return dtoWithoutDescriptionToField;
  }

  private List<String> checkDtoFieldsDescription(Method method) {
    List<String> dtoWithoutDescriptionToField = new ArrayList<>();
    java.lang.reflect.Parameter[] parameters = method.getParameters();
    Class<?>[] listOfClass = method.getParameterTypes();

    for (java.lang.reflect.Parameter parameter : parameters) {
      if (parameter.getType() == List.class || parameter.getType() == Set.class) {
        ParameterizedType parameterizedType = (ParameterizedType) parameter.getParameterizedType();
        parameterizedType.getActualTypeArguments();
        Class<?> classInList = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        if (classInList.isAnnotationPresent(Schema.class)) {
          dtoWithoutDescriptionToField.addAll(recursiveDtoFieldDescriptionCheck(classInList));
        }
      }
    }

    for (Class<?> clazz : listOfClass) {
      if (clazz.isAnnotationPresent(Schema.class)) {
        dtoWithoutDescriptionToField.addAll(recursiveDtoFieldDescriptionCheck(clazz));
      }
    }
    return dtoWithoutDescriptionToField;
  }

  private List<String> checkAccountIdentifierParam(Method method) {
    List<String> endpointsWithoutAccountParam = new ArrayList<>();
    boolean hasAccountIdentifierQueryParam = false;
    boolean hasAccountIdentifierPathParam = false;

    Annotation[][] parametersAnnotationsList = method.getParameterAnnotations();
    for (Annotation[] annotations : parametersAnnotationsList) {
      if (hasAccountIdentifierQueryParam || hasAccountIdentifierPathParam) {
        break;
      }
      for (Annotation parameterAnnotation : annotations) {
        if (parameterAnnotation.annotationType() == QueryParam.class) {
          QueryParam queryParameter = (QueryParam) parameterAnnotation;
          hasAccountIdentifierQueryParam =
              ACCOUNT_IDENTIFIER.equals(queryParameter.value()) || ACCOUNT_ID.equals(queryParameter.value());
        }
        if (parameterAnnotation.annotationType() == PathParam.class) {
          PathParam pathParameter = (PathParam) parameterAnnotation;
          hasAccountIdentifierPathParam =
              ACCOUNT_IDENTIFIER.equals(pathParameter.value()) || ACCOUNT_ID.equals(pathParameter.value());
        }
      }
    }
    if (!hasAccountIdentifierQueryParam && !hasAccountIdentifierPathParam) {
      endpointsWithoutAccountParam.add(method.getDeclaringClass().getName() + "." + method.getName());
    }
    return endpointsWithoutAccountParam;
  }

  @Override
  protected void configure() {
    MapBinder<String, TestExecution> testExecutionMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
    testExecutionMapBinder.addBinding("OAS test").toInstance(() -> testOASAdoption(getResourceClasses()));
  }
}
