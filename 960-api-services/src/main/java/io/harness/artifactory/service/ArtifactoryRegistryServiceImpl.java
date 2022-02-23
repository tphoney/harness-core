/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifactory.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifactory.ArtifactoryClientImpl;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorDescending;
import io.harness.exception.ArtifactoryRegistryException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.expression.RegexFunctor;

import software.wings.utils.RepositoryFormat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class ArtifactoryRegistryServiceImpl implements ArtifactoryRegistryService {
  @Inject ArtifactoryClientImpl artifactoryClient;

  @Override
  public List<BuildDetailsInternal> getBuilds(ArtifactoryConfigRequest artifactoryConfig, String repositoryName,
      String imageName, String repositoryFormat, int maxNumberOfBuilds) {
    if (RepositoryFormat.docker.name().equals(repositoryFormat)) {
      return artifactoryClient.getArtifactsDetails(
          artifactoryConfig, repositoryName, imageName, repositoryFormat, maxNumberOfBuilds);
    }
    throw NestedExceptionUtils.hintWithExplanationException("Please check your artifact YAML configuration.",
        String.format("RepositoryFormat [%s] is an invalid value.", repositoryFormat),
        new ArtifactoryRegistryException(
            "Invalid value for RepositoryFormat field. Currently only 'docker' repository format is supported."));
  }

  @Override
  public BuildDetailsInternal getLastSuccessfulBuildFromRegex(ArtifactoryConfigRequest artifactoryConfig,
      String repositoryName, String imageName, String repositoryFormat, String tagRegex) {
    try {
      Pattern.compile(tagRegex);
    } catch (PatternSyntaxException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check tagRegex field in Artifactory artifact configuration.",
          String.format("TagRegex field contains an invalid regex value '%s'.", tagRegex),
          new ArtifactoryRegistryException(e.getMessage()));
    }

    List<BuildDetailsInternal> builds =
        getBuilds(artifactoryConfig, repositoryName, imageName, repositoryFormat, MAX_NO_OF_TAGS_PER_IMAGE);
    builds = builds.stream()
                 .filter(build -> new RegexFunctor().match(tagRegex, build.getNumber()))
                 .sorted(new BuildDetailsInternalComparatorDescending())
                 .collect(Collectors.toList());

    if (builds.isEmpty()) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check tagRegex field in Artifactory artifact configuration.",
          String.format(
              "Could not find any tags that match regex [%s] for Artifactory repository [%s] for %s artifact image [%s] in registry [%s].",
              tagRegex, repositoryName, repositoryFormat, imageName, artifactoryConfig.getArtifactoryUrl()),
          new ArtifactoryRegistryException(
              String.format("Could not find an artifact image tag that matches tagRegex '%s'", tagRegex)));
    }
    return builds.get(0);
  }

  @Override
  public BuildDetailsInternal verifyBuildNumber(ArtifactoryConfigRequest artifactoryConfig, String repositoryName,
      String imageName, String repoFormat, String tag) {
    return getBuildNumber(artifactoryConfig, repositoryName, imageName, repoFormat, tag);
  }

  private BuildDetailsInternal getBuildNumber(ArtifactoryConfigRequest artifactoryConfig, String repository,
      String imageName, String repositoryFormat, String tag) {
    List<BuildDetailsInternal> builds =
        getBuilds(artifactoryConfig, repository, imageName, repositoryFormat, MAX_NO_OF_TAGS_PER_IMAGE);
    builds = builds.stream().filter(build -> build.getNumber().equals(tag)).collect(Collectors.toList());

    if (builds.size() == 0) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check your Artifactory repository for image/tag existence.",
          String.format(
              "Did not find any images for tag [%s] in Artifactory repository [%s] for %s artifact image [%s] in registry [%s].",
              tag, repository, repositoryFormat, imageName, artifactoryConfig.getArtifactoryUrl()),
          new ArtifactoryRegistryException(String.format("Image tag ('%s') not found.", tag)));
    } else if (builds.size() == 1) {
      return builds.get(0);
    }

    throw NestedExceptionUtils.hintWithExplanationException(
        "Please check your Artifactory repository for images with same tag.",
        String.format(
            "Found multiple artifact images for tag [%s] in Artifactory repository [%s] for %s artifact image [%s] in registry [%s].",
            tag, repository, repositoryFormat, imageName, artifactoryConfig.getArtifactoryUrl()),
        new ArtifactoryRegistryException(
            String.format("Found multiple image tags ('%s'), but expected only one.", tag)));
  }

  @Override
  public boolean validateCredentials(ArtifactoryConfigRequest artifactoryConfig) {
    return artifactoryClient.validateArtifactServer(artifactoryConfig);
  }

  @Override
  public boolean verifyArtifactManifestUrl(
      BuildDetailsInternal buildDetailsInternal, ArtifactoryConfigRequest artifactoryConfig) {
    String artifactManifestUrl = buildDetailsInternal.getMetadata().get(ArtifactMetadataKeys.ARTIFACT_MANIFEST_URL);
    if (isNotEmpty(artifactManifestUrl)) {
      return artifactoryClient.verifyArtifactManifestUrl(artifactoryConfig, artifactManifestUrl);
    }

    throw NestedExceptionUtils.hintWithExplanationException(
        "Please verify your Artifactory artifact repository URL field or repository port.",
        String.format("Artifact manifest url was not found when retrieving metadata for artifact [%s]",
            buildDetailsInternal.getMetadata().get(ArtifactMetadataKeys.IMAGE)),
        new ArtifactoryRegistryException("Could not retrieve artifact manifest."));
  }
}
