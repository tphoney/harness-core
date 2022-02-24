/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.resources;

import static io.harness.rule.OwnerRule.DEEPAK_CHHIKARA;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.rule.Owner;
import io.harness.rule.ResourceTestRule;
import io.harness.utils.InvalidResourceData;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.yaml.snakeyaml.Yaml;

public class ServiceLevelObjectiveResourceTest extends CvNextGenTestBase {
  @Inject private Injector injector;
  @Inject private MonitoredServiceService monitoredServiceService;
  @Inject private MetricPackService metricPackService;
  private MonitoredServiceDTO monitoredServiceDTO;
  private BuilderFactory builderFactory;
  private static ServiceLevelObjectiveResource serviceLevelObjectiveResource = new ServiceLevelObjectiveResource();

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(serviceLevelObjectiveResource).build();
  @Before
  public void setup() {
    injector.injectMembers(serviceLevelObjectiveResource);
    builderFactory = BuilderFactory.getDefault();
    metricPackService.createDefaultMetricPackAndThresholds(builderFactory.getContext().getAccountId(),
        builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier());
    monitoredServiceDTO = builderFactory.monitoredServiceDTOBuilder().build();
    monitoredServiceService.create(builderFactory.getContext().getAccountId(), monitoredServiceDTO);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testCreate_withThresholdSli() throws IOException {
    String sloYaml = getYAML("slo/slo-with-threshold-sli.yaml");
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testCreate_withRatioSli() throws IOException {
    String sloYaml = getYAML("slo/slo-with-ratio-sli.yaml");
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testCreate_withInvalidSli() throws IOException {
    String sloYaml = getYAML("slo/slo-with-ratio-sli.yaml");
    List<InvalidResourceData> invalidResourceDataList = new ArrayList<>();
    invalidResourceDataList.add(
        InvalidResourceData.builder().property("name").replacementValue("").expectedResponseCode(400).build());
    invalidResourceDataList.add(
        InvalidResourceData.builder().property("identifier").replacementValue("").expectedResponseCode(400).build());
    invalidResourceDataList.add(InvalidResourceData.builder()
                                    .path("target")
                                    .property("type")
                                    .replacementValue(100)
                                    .expectedResponseCode(500)
                                    .build());
    invalidResourceDataList.add(InvalidResourceData.builder()
                                    .path("target")
                                    .property("sloTargetPercentage")
                                    .replacementValue(100)
                                    .expectedResponseCode(400)
                                    .build());
    invalidResourceDataList.add(InvalidResourceData.builder()
                                    .path("serviceLevelIndicators")
                                    .property("sliMissingDataType")
                                    .replacementValue("")
                                    .expectedResponseCode(500)
                                    .build());
    invalidResourceDataList.add(InvalidResourceData.builder()
                                    .path("serviceLevelIndicators\\spec\\spec")
                                    .property("thresholdType")
                                    .replacementValue("$")
                                    .expectedResponseCode(500)
                                    .build());
    for (InvalidResourceData invalidResourceData : invalidResourceDataList) {
      String sloJson = replace(sloYaml, invalidResourceData);
      Response response = RESOURCES.client()
                              .target("http://localhost:9998/slo/")
                              .queryParam("accountId", builderFactory.getContext().getAccountId())
                              .request(MediaType.APPLICATION_JSON_TYPE)
                              .post(Entity.json(sloJson));
      assertThat(response.getStatus()).isEqualTo(invalidResourceData.getExpectedResponseCode());
    }
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withRollingSLOTarget() throws IOException {
    String sloYaml = getYAML("slo/slo-with-rolling-target.yaml");
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(200);
    String jsonResponse = response.readEntity(String.class);
    // TODO: we need to find a library to assert json responses in a better way.
    assertThat(jsonResponse)
        .contains(
            "{\"type\":\"Threshold\",\"spec\":{\"metric1\":\"metric2\",\"thresholdValue\":4.0,\"thresholdType\":\"<\"}}");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_sloTargetValidation() throws IOException {
    String sloYaml = getYAML("slo/slo-invalid-target.yaml");
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(400);
    String jsonResponse = response.readEntity(String.class);
    // TODO: we need to find a library to assert json responses in a better way.
    assertThat(jsonResponse).contains("{\"field\":\"sloTarget\",\"message\":\"slo target should be less than 100\"}");
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_invalidMonitoredServiceIdentifier() throws IOException {
    String sloYaml = getYAML("slo/slo-with-rolling-target.yaml", "invalidIdentifier");
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(500);
    String jsonResponse = response.readEntity(String.class);
    assertThat(jsonResponse).contains("Monitored Source Entity with identifier invalidIdentifier is not present");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_duplicateIdentifier() throws IOException {
    String sloYaml = getYAML("slo/slo-with-rolling-target.yaml");
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(200);
    response = RESOURCES.client()
                   .target("http://localhost:9998/slo/")
                   .queryParam("accountId", builderFactory.getContext().getAccountId())
                   .request(MediaType.APPLICATION_JSON_TYPE)
                   .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(500);
    String jsonResponse = response.readEntity(String.class);
    assertThat(jsonResponse)
        .containsPattern(Pattern.compile("serviceLevelObjective with identifier .* is already present"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withCalenderSLOTarget() throws IOException {
    String sloYaml = getYAML("slo/slo-with-calender-target.yaml");
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withCalenderSLOTargetInvalid() throws IOException {
    String sloYaml = getYAML("slo/slo-with-calender-target-invalid.yaml");
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo/")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .post(Entity.json(convertToJson(sloYaml)));
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(String.class)).contains("\"field\":\"dayOfWeek\",\"message\":\"may not be null\"");
  }

  private static String convertToJson(String yamlString) {
    Yaml yaml = new Yaml();
    Map<String, Object> map = yaml.load(yamlString);

    JSONObject jsonObject = new JSONObject(map);
    return jsonObject.toString();
  }

  private String getYAML(String filePath) throws IOException {
    return getYAML(filePath, monitoredServiceDTO.getIdentifier());
  }

  private String getYAML(String filePath, String monitoredServiceIdentifier) throws IOException {
    String sloYaml = getResource(filePath);
    sloYaml = sloYaml.replace("$projectIdentifier", builderFactory.getContext().getProjectIdentifier());
    sloYaml = sloYaml.replace("$orgIdentifier", builderFactory.getContext().getOrgIdentifier());
    sloYaml = sloYaml.replace("$monitoredServiceRef", monitoredServiceIdentifier);
    sloYaml = sloYaml.replace(
        "$healthSourceRef", monitoredServiceDTO.getSources().getHealthSources().iterator().next().getIdentifier());
    return sloYaml;
  }

  private String replace(String text, InvalidResourceData invalidResourceData) {
    Yaml yaml = new Yaml();
    Map<String, Object> map = yaml.load(text);
    JSONObject jsonObject = new JSONObject(map);
    JSONObject iteratorObject = jsonObject;
    if (Objects.nonNull(invalidResourceData.getPath())) {
      String[] path = invalidResourceData.getPath().split("\\\\");
      for (String value : path) {
        JSONObject dataObject = iteratorObject.optJSONObject(value);
        if (Objects.nonNull(dataObject)) {
          iteratorObject = iteratorObject.getJSONObject(value);
        } else {
          iteratorObject = iteratorObject.getJSONArray(value).getJSONObject(0);
        }
      }
    }
    iteratorObject.remove(invalidResourceData.getProperty());
    iteratorObject.put(invalidResourceData.getProperty(), invalidResourceData.getReplacementValue());
    return jsonObject.toString();
  }
}
