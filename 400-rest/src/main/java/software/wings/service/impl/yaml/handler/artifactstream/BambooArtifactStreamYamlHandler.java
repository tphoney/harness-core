/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.artifactstream;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.FeatureName.ARTIFACT_STREAM_METADATA_ONLY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream.Yaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author rktummala on 10/09/17
 */
@OwnedBy(CDC)
@Singleton
public class BambooArtifactStreamYamlHandler extends ArtifactStreamYamlHandler<Yaml, BambooArtifactStream> {
  @Inject FeatureFlagService featureFlagService;

  @Override
  public Yaml toYaml(BambooArtifactStream bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setArtifactPaths(bean.getArtifactPaths());
    yaml.setPlanName(bean.getJobname());

    boolean metadataOnly;
    if (featureFlagService.isEnabled(ARTIFACT_STREAM_METADATA_ONLY, bean.getAccountId())) {
      metadataOnly = true;
    } else {
      metadataOnly = bean.isMetadataOnly();
    }
    yaml.setMetadataOnly(metadataOnly);
    return yaml;
  }

  @Override
  protected void toBean(BambooArtifactStream bean, ChangeContext<Yaml> changeContext, String appId) {
    super.toBean(bean, changeContext, appId);
    Yaml yaml = changeContext.getYaml();
    bean.setArtifactPaths(yaml.getArtifactPaths());
    bean.setJobname(yaml.getPlanName());

    boolean metadataOnly;
    if (featureFlagService.isEnabled(ARTIFACT_STREAM_METADATA_ONLY, bean.getAccountId())) {
      metadataOnly = true;
    } else {
      metadataOnly = yaml.isMetadataOnly();
    }
    bean.setMetadataOnly(metadataOnly);
  }

  @Override
  protected BambooArtifactStream getNewArtifactStreamObject() {
    return new BambooArtifactStream();
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
