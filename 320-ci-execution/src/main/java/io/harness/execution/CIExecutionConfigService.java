package io.harness.execution;

import io.harness.ci.beans.entities.CIExecutionConfig;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.repositories.CIExecutionConfigRepository;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Optional;

public class CIExecutionConfigService {
  @Inject CIExecutionConfigRepository configRepository;
  @Inject CIExecutionServiceConfig ciExecutionServiceConfig;
  public static String ADDON_IMAGE = "harness/ci-addon";
  public static String LITE_ENGINE_IMAGE = "harness/ci-lite-engine";

  public Boolean updateCIContainerTag(String accountId, String tag) {
    CIExecutionConfig executionConfig;
    Optional<CIExecutionConfig> existingConfig = configRepository.findFirstByAccountIdentifier(accountId);
    if (existingConfig.isPresent()) {
      executionConfig = existingConfig.get();
      executionConfig.setTag(tag);
    } else {
      executionConfig = CIExecutionConfig.builder().accountIdentifier(accountId).tag(tag).build();
    }
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
      tag = configOptional.get().getTag();
    } else {
      tag = ciExecutionServiceConfig.getCiImageTag();
    }
    return tag;
  }

  public Boolean isUsingDeprecatedTag(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    String allowedTags = ciExecutionServiceConfig.getSupportedCIImageTags();
    String[] allowedTagsList = allowedTags.split(",");
    if (configOptional.isPresent()) {
      String tag = configOptional.get().getTag();
      return !Arrays.asList(allowedTagsList).contains(tag);
    } else {
      return false;
    }
  }
}
