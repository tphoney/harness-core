/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.evaluators;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class YamlExpressionFunctorTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetYamlMap() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("inputset/pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(yamlContent);

    YamlExpressionFunctor functor = YamlExpressionFunctor.builder().build();
    List<String> fqnList = new LinkedList<>();
    Map<String, Map<String, Object>> fqnToValueMap = new HashMap<>();
    Map<String, Object> contextMap =
        functor.getYamlMap(yamlField.getNode().getField(YAMLFieldNameConstants.PIPELINE), fqnToValueMap, fqnList);
    assertThat(!contextMap.isEmpty()).isTrue();
    assertThat(contextMap.get("pipeline")).isInstanceOf(HashMap.class);
    Map<String, Object> pipelineMap = (Map<String, Object>) contextMap.get("pipeline");
    assertThat(pipelineMap.get("variables")).isInstanceOf(HashMap.class);
    Map<String, Object> pipelineVariablesMap = (Map<String, Object>) pipelineMap.get("variables");
    assertThat(pipelineVariablesMap.get("pipelineN1")).isEqualTo("stringValue1");
    Map<String, Object> stagesMap = (Map<String, Object>) pipelineMap.get("stages");
    Map<String, Object> stage1Map = (Map<String, Object>) stagesMap.get("stage1");
    Map<String, Object> stage1SpecMap = (Map<String, Object>) stage1Map.get("spec");
    Map<String, Object> stage1ServiceConfigMap = (Map<String, Object>) stage1SpecMap.get("serviceConfig");
    Map<String, Object> stage1ServiceMap = (Map<String, Object>) stage1ServiceConfigMap.get("service");
    assertThat(stage1ServiceMap.get("name")).isEqualTo("service1");
    assertThat(fqnList).isEmpty();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetMethod() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("inputset/pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlField yamlField = YamlUtils.readTree(yamlContent);
    YamlExpressionFunctor yamlExpressionFunctor =
        YamlExpressionFunctor.builder()
            .rootYamlField(yamlField.getNode().getField(YAMLFieldNameConstants.PIPELINE))
            .fqnPathToElement(
                "pipeline.stages.stage1.spec.serviceConfig.serviceDefinition.spec.artifacts.primary.spec.imagePath")
            .build();
    Object object = yamlExpressionFunctor.get("serviceConfig");
    assertThat(object).isNotNull();
    assertThat(object).isInstanceOf(HashMap.class);
    Map<String, Object> serviceMap = (Map<String, Object>) object;
    assertThat(serviceMap.keySet().size()).isEqualTo(2);
  }
}
