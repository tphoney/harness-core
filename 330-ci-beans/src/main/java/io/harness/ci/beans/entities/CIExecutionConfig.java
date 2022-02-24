package io.harness.ci.beans.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "CIExecutionConfigKeys")
@Entity(value = "ciexecutionconfig", noClassnameStored = true)
@StoreIn("harnessci")
@Document("ciexecutionconfig")
@HarnessEntity(exportable = true)
public class CIExecutionConfig implements PersistentEntity, UuidAware, CreatedAtAware {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotEmpty String accountIdentifier;
  @NotEmpty String addOnTag;
  @NotEmpty String liteEngineTag;
  @NotEmpty String gitCloneTag;
  @NotEmpty String securityTag;
  @NotEmpty String buildAndPushDockerRegistryTag;
  @NotEmpty String buildAndPushECRTag;
  @NotEmpty String buildAndPushGCRTag;
  @NotEmpty String gcsUploadTag;
  @NotEmpty String s3UploadTag;
  @NotEmpty String artifactoryUploadTag;
  @NotEmpty String cacheGCSTag;
  @NotEmpty String cacheS3Tag;
  @SchemaIgnore private long createdAt;
}
