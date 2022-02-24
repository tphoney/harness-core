package io.harness.execution;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.ci.beans.entities.CIExecutionConfig;
import io.harness.ci.beans.entities.CIExecutionImages;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.ci.config.StepImageConfig;
import io.harness.repositories.CIExecutionConfigRepository;

import com.google.inject.Inject;
import de.skuzzle.semantic.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CIExecutionConfigService {
  @Inject CIExecutionConfigRepository configRepository;
  @Inject CIExecutionServiceConfig ciExecutionServiceConfig;

  public CIExecutionServiceConfig getCiExecutionServiceConfig() {
    return ciExecutionServiceConfig;
  }

  public Boolean updateCIContainerTag(String accountId, CIExecutionImages ciExecutionImages) {
    CIExecutionConfig executionConfig;
    Optional<CIExecutionConfig> existingConfig = configRepository.findFirstByAccountIdentifier(accountId);
    if (existingConfig.isPresent()) {
      executionConfig = existingConfig.get();
    } else {
      executionConfig = CIExecutionConfig.builder().accountIdentifier(accountId).build();
    }
    executionConfig.setGitCloneTag(ciExecutionImages.getGitCloneTag());
    executionConfig.setAddOnTag(ciExecutionImages.getAddonTag());
    executionConfig.setLiteEngineTag(ciExecutionImages.getLiteEngineTag());
    executionConfig.setArtifactoryUploadTag(ciExecutionImages.getArtifactoryUploadTag());
    executionConfig.setBuildAndPushDockerRegistryTag(ciExecutionImages.getBuildAndPushDockerRegistryTag());
    executionConfig.setCacheGCSTag(ciExecutionImages.getCacheGCSTag());
    executionConfig.setCacheS3Tag(ciExecutionImages.getCacheS3Tag());
    executionConfig.setBuildAndPushECRTag(ciExecutionImages.getBuildAndPushGCRTag());
    executionConfig.setBuildAndPushGCRTag(ciExecutionImages.getBuildAndPushGCRTag());
    executionConfig.setGcsUploadTag(ciExecutionImages.getGcsUploadTag());
    executionConfig.setS3UploadTag(ciExecutionImages.getS3UploadTag());
    executionConfig.setSecurityTag(ciExecutionImages.getSecurityTag());
    configRepository.save(executionConfig);
    return true;
  }

  public String getLiteEngineImage(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    String image;
    if (configOptional.isPresent()) {
      image = configOptional.get().getAddOnTag();
    } else {
      image = ciExecutionServiceConfig.getAddonImage();
    }
    return image;
  }

  public String getAddonImage(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    String tag;
    if (configOptional.isPresent()) {
      tag = configOptional.get().getAddOnTag();
    } else {
      tag = ciExecutionServiceConfig.getLiteEngineImage();
    }
    return tag;
  }

  public List<DeprecatedImageInfo> getDeprecatedTags(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    List<DeprecatedImageInfo> deprecatedTags = new ArrayList();
    if (configOptional.isPresent()) {
      CIExecutionConfig ciExecutionConfig = configOptional.get();
      if (!checkForCIImage(ciExecutionServiceConfig.getAddonImage(), ciExecutionConfig.getAddOnTag())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("AddonImage").version(ciExecutionConfig.getAddOnTag()).build());
      }
      if (!checkForCIImage(ciExecutionServiceConfig.getLiteEngineImage(), ciExecutionConfig.getLiteEngineTag())) {
        deprecatedTags.add(
                DeprecatedImageInfo.builder().tag("LiteEngineTag").version(ciExecutionConfig.getLiteEngineTag()).build());
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
      if (!ciExecutionServiceConfig.getStepConfig().getSecurityConfig().equals(ciExecutionConfig.getSecurityTag())) {
        deprecatedTags.add(
                DeprecatedImageInfo.builder().tag("SecurityTag").version(ciExecutionConfig.getGcsUploadTag()).build());
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

  private boolean checkForCIImage(String customImage, String defaultImage) {
    String defaultImageTag = defaultImage.split(":")[1];
    String customImageTag = customImage.split(":")[1];
    Version defaultVersion = Version.parseVersion(defaultImageTag);
    Version customVersion = Version.parseVersion(customImageTag);
    // we are supporting 2 back versions
    return defaultVersion.isLowerThanOrEqualTo(customVersion.nextMinor().nextMinor());
  }

  public StepImageConfig getPluginVersion(CIStepInfoType stepInfoType, String accountId) {
    Optional<CIExecutionConfig> existingConfig = configRepository.findFirstByAccountIdentifier(accountId);
    List<String> entrypoint;
    String image;
    switch (stepInfoType) {
      case DOCKER:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getBuildAndPushDockerRegistryConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getBuildAndPushDockerRegistryTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getBuildAndPushDockerRegistryConfig().getImage();
        }
        break;
      case GCR:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getBuildAndPushGCRConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getBuildAndPushGCRTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getBuildAndPushGCRConfig().getImage();
        }
        break;
      case ECR:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getBuildAndPushECRConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getBuildAndPushECRTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getBuildAndPushECRConfig().getImage();
        }
        break;
      case RESTORE_CACHE_S3:
      case SAVE_CACHE_S3:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getCacheS3Config().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getCacheS3Tag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getCacheS3Config().getImage();
        }
        break;
      case UPLOAD_S3:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getS3UploadConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getS3UploadTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getS3UploadConfig().getImage();
        }
        break;
      case UPLOAD_GCS:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getCacheGCSConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getGcsUploadTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getGcsUploadConfig().getImage();
        }
        break;
      case SAVE_CACHE_GCS:
      case RESTORE_CACHE_GCS:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getCacheGCSConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getCacheGCSTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getCacheGCSConfig().getImage();
        }
        break;
      case SECURITY:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getSecurityConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getSecurityTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getSecurityConfig().getImage();
        }
        break;
      case UPLOAD_ARTIFACTORY:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getArtifactoryUploadConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getArtifactoryUploadTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getArtifactoryUploadConfig().getImage();
        }
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + stepInfoType);
    }
    return StepImageConfig.builder().entrypoint(entrypoint).image(image).build();
  }
}
