package io.harness.execution;

import io.harness.ci.beans.entities.CIExecutionConfig;
import io.harness.ci.beans.entities.CIExecutionImages;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.repositories.CIExecutionConfigRepository;

import com.google.inject.Inject;
import de.skuzzle.semantic.Version;
import java.util.ArrayList;
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

  public List<DeprecatedImageInfo> getDeprecatedTags(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    List<DeprecatedImageInfo> deprecatedTags = new ArrayList();
    if (configOptional.isPresent()) {
      CIExecutionConfig ciExecutionConfig = configOptional.get();
      if (checkForCIImage(ciExecutionServiceConfig.getCiImageTag(), ciExecutionConfig.getCiContainerTag())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("CIImageTag").version(ciExecutionConfig.getCiContainerTag()).build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getCacheS3Config().equals(ciExecutionConfig.getCacheS3Tag())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("CacheS3Tag").version(ciExecutionConfig.getCacheS3Tag()).build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getArtifactoryUploadConfig().equals(
              ciExecutionConfig.getArtifactoryUploadTag())) {
        deprecatedTags.add(DeprecatedImageInfo.builder()
                               .tag("ArtifactoryUploadTag")
                               .version(ciExecutionConfig.getArtifactoryUploadTag())
                               .build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getCacheGCSConfig().equals(ciExecutionConfig.getCacheGCSTag())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("CacheGCSTag").version(ciExecutionConfig.getCacheGCSTag()).build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getS3UploadConfig().equals(ciExecutionConfig.getS3UploadTag())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("S3UploadTag").version(ciExecutionConfig.getS3UploadTag()).build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getCacheS3Config().equals(ciExecutionConfig.getCacheS3Tag())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("CacheS3Tag").version(ciExecutionConfig.getCacheS3Tag()).build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getGcsUploadConfig().equals(ciExecutionConfig.getGcsUploadTag())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("GCSUploadTag").version(ciExecutionConfig.getGcsUploadTag()).build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getBuildAndPushDockerRegistryConfig().equals(
              ciExecutionConfig.getBuildAndPushDockerRegistryTag())) {
        deprecatedTags.add(DeprecatedImageInfo.builder()
                               .tag("BuildAndPushDockerTag")
                               .version(ciExecutionConfig.getBuildAndPushDockerRegistryTag())
                               .build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getGitCloneConfig().equals(ciExecutionConfig.getGitCloneTag())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("GitCloneTag").version(ciExecutionConfig.getGitCloneTag()).build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getBuildAndPushECRConfig().equals(
              ciExecutionConfig.getBuildAndPushECRTag())) {
        deprecatedTags.add(DeprecatedImageInfo.builder()
                               .tag("BuildAndPushECRConfig")
                               .version(ciExecutionConfig.getBuildAndPushECRTag())
                               .build());
      }
      if (!ciExecutionServiceConfig.getStepConfig().getBuildAndPushGCRConfig().equals(
              ciExecutionConfig.getBuildAndPushGCRTag())) {
        deprecatedTags.add(DeprecatedImageInfo.builder()
                               .tag("BuildAndPushGCRConfig")
                               .version(ciExecutionConfig.getBuildAndPushGCRTag())
                               .build());
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
