package io.harness.execution;

import io.harness.ci.beans.entities.CIExecutionConfig;
import io.harness.ci.beans.entities.CIExecutionImages;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.repositories.CIExecutionConfigRepository;

import com.google.inject.Inject;
import de.skuzzle.semantic.Version;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CIExecutionConfigService {
  @Inject CIExecutionConfigRepository configRepository;
  @Inject CIExecutionServiceConfig ciExecutionServiceConfig;
  public static String ADDON_IMAGE = "harness/ci-addon";
  public static String LITE_ENGINE_IMAGE = "harness/ci-lite-engine";

  public Boolean updateCIContainerTag(String accountId, CIExecutionImages ciExecutionImages) {
    CIExecutionConfig executionConfig;
    Optional<CIExecutionConfig> existingConfig = configRepository.findFirstByAccountIdentifier(accountId);
    if (existingConfig.isPresent()) {
      executionConfig = existingConfig.get();
    } else {
      executionConfig = CIExecutionConfig.builder().accountIdentifier(accountId).build();
    }
    executionConfig.setCiContainerTag(ciExecutionImages.getCiContainerTag());
    executionConfig.setArtifactoryUploadTag(ciExecutionImages.getArtifactoryUploadTag());
    executionConfig.setBuildAndPushDockerRegistryTag(ciExecutionImages.getBuildAndPushDockerRegistryTag());
    executionConfig.setCacheGCSTag(ciExecutionImages.getCacheGCSTag());
    executionConfig.setCacheS3Tag(ciExecutionImages.getCacheS3Tag());
    executionConfig.setBuildAndPushECRTag(ciExecutionImages.getBuildAndPushGCRTag());
    executionConfig.setBuildAndPushGCRTag(ciExecutionImages.getBuildAndPushGCRTag());
    executionConfig.setGcsUploadTag(ciExecutionImages.getGcsUploadTag());
    executionConfig.setS3UploadTag(ciExecutionImages.getS3UploadTag());
    configRepository.save(executionConfig);
    return true;
  }

  public String getLiteEngineImage(String accountId) {
    String tag = getContainerTag(accountId);
    return LITE_ENGINE_IMAGE + ":" + tag;
  }

  public String getAddonImage(String accountId) {
    String tag = getContainerTag(accountId);
    return ADDON_IMAGE + ":" + tag;
  }

  private String getContainerTag(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    String tag;
    if (configOptional.isPresent()) {
      tag = configOptional.get().getCiContainerTag();
    } else {
      tag = ciExecutionServiceConfig.getCiImageTag();
    }
    return tag;
  }

  public List<String> getDeprecatedTags(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    List<String> deprecatedTags = Collections.emptyList();
    if (configOptional.isPresent()) {
      if (checkForCIImage(ciExecutionServiceConfig.getCiImageTag(), configOptional.get().getCiContainerTag())) {
        deprecatedTags.add("CIImageTag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getCacheS3Config().equals(configOptional.get().getCacheS3Tag())) {
        deprecatedTags.add("CacheS3Tag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getArtifactoryUploadConfig().equals(
              configOptional.get().getArtifactoryUploadTag())) {
        deprecatedTags.add("ArtifactoryUploadTag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getCacheGCSConfig().equals(configOptional.get().getCacheGCSTag())) {
        deprecatedTags.add("CacheGCSTag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getS3UploadConfig().equals(configOptional.get().getS3UploadTag())) {
        deprecatedTags.add("S3UploadTag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getCacheS3Config().equals(configOptional.get().getCacheS3Tag())) {
        deprecatedTags.add("CacheS3Tag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getGcsUploadConfig().equals(
              configOptional.get().getGcsUploadTag())) {
        deprecatedTags.add("GCSUploadTag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getBuildAndPushDockerRegistryConfig().equals(
              configOptional.get().getBuildAndPushDockerRegistryTag())) {
        deprecatedTags.add("BuildAndPushDockerTag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getGitCloneConfig().equals(configOptional.get().getGitCloneTag())) {
        deprecatedTags.add("GitCloneTag");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getBuildAndPushECRConfig().equals(
              configOptional.get().getBuildAndPushECRTag())) {
        deprecatedTags.add("BuildAndPushECRConfig");
      }
      if (!ciExecutionServiceConfig.getStepConfig().getBuildAndPushGCRConfig().equals(
              configOptional.get().getBuildAndPushGCRTag())) {
        deprecatedTags.add("BuildAndPushGCRConfig");
      }
    }
    return deprecatedTags;
  }

  private boolean checkForCIImage(String customImageTag, String defaultImageTag) {
    Version latestTag = Version.parseVersion(ciExecutionServiceConfig.getCiImageTag());
    Version customTag = Version.parseVersion(customImageTag);
    // we are supporting 2 back versions
    return latestTag.isLowerThanOrEqualTo(customTag.nextMinor().nextMinor());
  }
}
