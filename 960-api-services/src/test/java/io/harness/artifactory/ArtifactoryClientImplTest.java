/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifactory;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.TMACARI;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jfrog.artifactory.client.model.impl.PackageTypeImpl.docker;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.helpers.ext.jenkins.BuildDetails;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jfrog.artifactory.client.model.impl.PackageTypeImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(CDP)
public class ArtifactoryClientImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks ArtifactoryClientImpl artifactoryClient;

  /**
   * The Wire mock rule.
   */
  @Rule
  public WireMockRule wireMockRule =
      new WireMockRule(WireMockConfiguration.wireMockConfig()
                           .usingFilesUnderClasspath("960-api-services/src/test/resources")
                           .disableRequestJournal()
                           .port(0));

  String url;

  private ArtifactoryConfigRequest artifactoryConfig;
  private ArtifactoryConfigRequest artifactoryConfigAnonymous;

  @Before
  public void setUp() throws IllegalAccessException {
    url = String.format("http://localhost:%d/artifactory/", wireMockRule.port());
    artifactoryConfig = ArtifactoryConfigRequest.builder()
                            .artifactoryUrl(url)
                            .username("admin")
                            .password("dummy123!".toCharArray())
                            .build();
    artifactoryConfigAnonymous = ArtifactoryConfigRequest.builder().artifactoryUrl(url).build();
  }

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetRepositories() {
    Map<String, String> repositories = artifactoryClient.getRepositories(
        artifactoryConfig, Arrays.stream(PackageTypeImpl.values()).filter(type -> docker != type).collect(toList()));
    assertThat(repositories.size()).isEqualTo(4);
    assertThat(repositories.get("harness-maven")).isEqualTo("harness-maven");
    assertThat(repositories.get("harness-rpm")).isEqualTo("harness-rpm");
    assertThat(repositories.get("harness-maven-snapshots")).isEqualTo("harness-maven-snapshots");
    assertThat(repositories.get("harness-ivy")).isEqualTo("harness-ivy");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetRpmFilePaths() {
    List<BuildDetails> builds = artifactoryClient.getBuildDetails(artifactoryConfig, "harness-rpm", "todolist*/", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(BuildDetails::getNumber).contains("todolist-1.0-2.x86_64.rpm");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoWithAnyWildcardMatch() {
    List<BuildDetails> builds = artifactoryClient.getBuildDetails(
        artifactoryConfig, "harness-maven", "io/harness/todolist/todolist/*/*.war", 50);
    assertThat(builds).isNotNull();
    assertThat(builds)
        .extracting(BuildDetails::getNumber)
        .contains("1.0.0-SNAPSHOT/todolist-1.0.0-20170930.195402-1.war");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoForAtLeastOneWildcardPattern() {
    List<BuildDetails> builds = artifactoryClient.getBuildDetails(
        artifactoryConfig, "harness-maven", "io/harness/todolist/todolist/[0-9]+/*.war", 50);
    assertThat(builds).isNotNull();
    assertThat(builds)
        .extracting(BuildDetails::getNumber)
        .contains("1.0.0-SNAPSHOT/todolist-1.0.0-20170930.195402-1.war");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoForArtifactPathsWithoutAnyWildcardCharacter() {
    List<BuildDetails> builds = artifactoryClient.getBuildDetails(
        artifactoryConfig, "harness-maven", "/io/harness/todolist/todolist/1.0/todolist-1.0.war", 50);
    assertThat(builds).isNotNull();
    assertThat(builds)
        .extracting(BuildDetails::getNumber)
        .contains("io/harness/todolist/todolist/1.0/todolist-1.0.war");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoForArtifactPathsWithoutAnyWildcardCharacter1() {
    List<BuildDetails> builds = artifactoryClient.getBuildDetails(
        artifactoryConfig, "harness-maven", "io/harness/todolist/todolist/1.0/*.war", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(BuildDetails::getNumber).contains("todolist-1.0.war");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void shouldDownloadArtifact() {
    Map<String, String> metadata = ImmutableMap.of(
        "artifactPath", "harness-rpm/todolist-1.0-2.x86_64.rpm", "artifactFileName", "todolist-1.0-2.x86_64.rpm");
    InputStream artifactInputStream = artifactoryClient.downloadArtifacts(
        artifactoryConfig, "harness-rpm", metadata, "artifactPath", "artifactFileName");
    assertThat(artifactInputStream).isNotNull();
  }
}
