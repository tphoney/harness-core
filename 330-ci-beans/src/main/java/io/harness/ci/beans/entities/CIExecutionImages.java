package io.harness.ci.beans.entities;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@Singleton
public class CIExecutionImages {
  @NotEmpty String ciContainerTag;
  @NotEmpty String gitCloneTag;
  @NotEmpty String buildAndPushDockerRegistryTag;
  @NotEmpty String buildAndPushECRTag;
  @NotEmpty String buildAndPushGCRTag;
  @NotEmpty String gcsUploadTag;
  @NotEmpty String s3UploadTag;
  @NotEmpty String artifactoryUploadTag;
  @NotEmpty String cacheGCSTag;
  @NotEmpty String cacheS3Tag;
}
