/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.RancherKubernetesInfrastructure;
import software.wings.infra.RancherKubernetesInfrastructure.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import java.util.List;

public class RancherKubernetesInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<Yaml, RancherKubernetesInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public Yaml toYaml(RancherKubernetesInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return Yaml.builder()
        .namespace(bean.getNamespace())
        .releaseName(bean.getReleaseName())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.RANCHER_KUBERNETES)
        .clusterSelectionCriteria(bean.getClusterSelectionCriteria())
        .build();
  }

  @Override
  public RancherKubernetesInfrastructure upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    RancherKubernetesInfrastructure bean = RancherKubernetesInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(RancherKubernetesInfrastructure bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setNamespace(yaml.getNamespace());
    bean.setReleaseName(yaml.getReleaseName());
    bean.setClusterSelectionCriteria(yaml.getClusterSelectionCriteria());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
