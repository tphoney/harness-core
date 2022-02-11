/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;

import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.utils.Utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@JsonTypeName("RANCHER_KUBERNETES")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "RancherKubernetesInfrastructureMappingKeys")
@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class RancherKubernetesInfrastructureMapping extends ContainerInfrastructureMapping {
  @Attributes(title = "Namespace") private String namespace;
  @Trimmed private String releaseName;

  public RancherKubernetesInfrastructureMapping() {
    super(InfrastructureMappingType.RANCHER_KUBERNETES.name());
  }

  @lombok.Builder
  public RancherKubernetesInfrastructureMapping(String accountId, String infraMappingType, String cloudProviderType,
      String cloudProviderId, String namespace, String appId, String envId, String deploymentType,
      String serviceTemplateId) {
    this();
    this.accountId = accountId;
    super.infraMappingType = infraMappingType;
    this.computeProviderType = cloudProviderType;
    this.computeProviderSettingId = cloudProviderId;
    this.namespace = namespace;
    this.appId = appId;
    this.envId = envId;
    this.deploymentType = deploymentType;
    this.serviceTemplateId = serviceTemplateId;
  }

  @Override
  public void applyProvisionerVariables(
      Map<String, Object> map, NodeFilteringType nodeFilteringType, boolean featureFlagEnabled) {
    throw new UnsupportedOperationException();
  }

  @SchemaIgnore
  @Override
  @Attributes(title = "Connection Type")
  public String getHostConnectionAttrs() {
    return null;
  }

  @SchemaIgnore
  @Override
  public String getDefaultName() {
    return Utils.normalize(format("%s_RANCHER_Kubernetes_%s",
        Optional.ofNullable(getComputeProviderName()).orElse(getComputeProviderType().toLowerCase()).replace(':', '_'),
        Optional.ofNullable(getNamespace()).orElse("default")));
  }

  public static final class Builder {
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String accountId;
    private String clusterName;
    private String namespace;
    private String releaseName;
    private String uuid;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private String computeProviderSettingId;
    private String envId;
    private String serviceTemplateId;
    private String serviceId;
    private String computeProviderType;
    private String infraMappingType;
    private String deploymentType;
    private String computeProviderName;
    private String name;
    private boolean sample;
    // auto populate name
    private boolean autoPopulate = true;

    private Builder() {}

    public static Builder aRancherKubernetesInfrastructureMapping() {
      return new Builder();
    }

    public Builder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public Builder withReleaseName(String releaseName) {
      this.releaseName = releaseName;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public Builder withComputeProviderSettingId(String computeProviderSettingId) {
      this.computeProviderSettingId = computeProviderSettingId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    public Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public Builder withComputeProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    public Builder withComputeProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public Builder withSample(boolean sample) {
      this.sample = sample;
      return this;
    }

    public RancherKubernetesInfrastructureMapping build() {
      RancherKubernetesInfrastructureMapping rancherKubernetesInfrastructureMapping =
          new RancherKubernetesInfrastructureMapping();
      rancherKubernetesInfrastructureMapping.setClusterName(clusterName);
      rancherKubernetesInfrastructureMapping.setNamespace(namespace);
      rancherKubernetesInfrastructureMapping.setReleaseName(releaseName);
      rancherKubernetesInfrastructureMapping.setUuid(uuid);
      rancherKubernetesInfrastructureMapping.setAppId(appId);
      rancherKubernetesInfrastructureMapping.setCreatedBy(createdBy);
      rancherKubernetesInfrastructureMapping.setCreatedAt(createdAt);
      rancherKubernetesInfrastructureMapping.setLastUpdatedBy(lastUpdatedBy);
      rancherKubernetesInfrastructureMapping.setLastUpdatedAt(lastUpdatedAt);
      rancherKubernetesInfrastructureMapping.setEntityYamlPath(entityYamlPath);
      rancherKubernetesInfrastructureMapping.setComputeProviderSettingId(computeProviderSettingId);
      rancherKubernetesInfrastructureMapping.setEnvId(envId);
      rancherKubernetesInfrastructureMapping.setServiceTemplateId(serviceTemplateId);
      rancherKubernetesInfrastructureMapping.setServiceId(serviceId);
      rancherKubernetesInfrastructureMapping.setComputeProviderType(computeProviderType);
      rancherKubernetesInfrastructureMapping.setInfraMappingType(infraMappingType);
      rancherKubernetesInfrastructureMapping.setDeploymentType(deploymentType);
      rancherKubernetesInfrastructureMapping.setComputeProviderName(computeProviderName);
      rancherKubernetesInfrastructureMapping.setName(name);
      rancherKubernetesInfrastructureMapping.setAutoPopulate(autoPopulate);
      rancherKubernetesInfrastructureMapping.setAccountId(accountId);
      rancherKubernetesInfrastructureMapping.setSample(sample);
      return rancherKubernetesInfrastructureMapping;
    }
  }
}
