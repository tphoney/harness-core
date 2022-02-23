/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.accesscontrol.serializer.AccessControlClientRegistrars;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.stage.DeploymentStageNode;
import io.harness.cdng.helm.HelmDeployStepNode;
import io.harness.cdng.helm.HelmRollbackStepNode;
import io.harness.cdng.k8s.K8sApplyStepNode;
import io.harness.cdng.k8s.K8sBGSwapServicesStepNode;
import io.harness.cdng.k8s.K8sBlueGreenStepNode;
import io.harness.cdng.k8s.K8sCanaryDeleteStepNode;
import io.harness.cdng.k8s.K8sCanaryStepNode;
import io.harness.cdng.k8s.K8sDeleteStepNode;
import io.harness.cdng.k8s.K8sRollingRollbackStepNode;
import io.harness.cdng.k8s.K8sRollingStepNode;
import io.harness.cdng.k8s.K8sScaleStepNode;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.provision.terraform.TerraformApplyStepNode;
import io.harness.cdng.provision.terraform.TerraformDestroyStepNode;
import io.harness.cdng.provision.terraform.TerraformPlanStepNode;
import io.harness.cdng.provision.terraform.TerraformRollbackStepNode;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.serializer.kryo.NGKryoRegistrar;
import io.harness.serializer.morphia.NGMorphiaRegistrar;
import io.harness.yaml.schema.beans.SchemaNamespaceConstants;
import io.harness.yaml.schema.beans.YamlGroup;
import io.harness.yaml.schema.beans.YamlSchemaMetadata;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.serializer.kryo.PollingKryoRegistrar;
import java.util.Collections;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class CDNGRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(AccessControlClientRegistrars.kryoRegistrars)
          .addAll(ManagerRegistrars.kryoRegistrars)
          .addAll(SMCoreRegistrars.kryoRegistrars)
          .addAll(DelegateServiceDriverRegistrars.kryoRegistrars)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .addAll(GitOpsRegistrars.kryoRegistrars)
          .addAll(YamlBeansModuleRegistrars.kryoRegistrars)
          .addAll(OrchestrationStepsModuleRegistrars.kryoRegistrars)
          .addAll(CDNGBeanRegistrars.kryoRegistrars)
          .add(NGKryoRegistrar.class)
          .add(PollingKryoRegistrar.class)
          .addAll(DelegateTaskRegistrars.kryoRegistrars)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ManagerRegistrars.morphiaRegistrars)
          .addAll(SMCoreRegistrars.morphiaRegistrars)
          .addAll(DelegateServiceDriverRegistrars.morphiaRegistrars)
          .addAll(ConnectorNextGenRegistrars.morphiaRegistrars)
          .addAll(GitOpsRegistrars.morphiaRegistrars)
          .add(NGMorphiaRegistrar.class)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .addAll(YamlBeansModuleRegistrars.morphiaRegistrars)
          .addAll(OrchestrationStepsModuleRegistrars.morphiaRegistrars)
          .addAll(CDNGBeanRegistrars.morphiaRegistrars)
          .addAll(InstanceRegistrars.morphiaRegistrars)
          .addAll(DelegateTaskRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder()
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.DEPLOYMENT_STAGE)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(DeploymentStageNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(ImmutableList.of(ModuleType.CD, ModuleType.PMS))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STAGE.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.DEPLOYMENT_STEPS)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CDStepInfo.class)
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_CANARY_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sCanaryStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_APPLY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sApplyStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_BLUE_GREEN_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sBlueGreenStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_ROLLING_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sRollingStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_ROLLING_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sRollingRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_SCALE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sScaleStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_DELETE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sDeleteStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_BG_SWAP_SERVICES_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sBGSwapServicesStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.K8S_CANARY_DELETE_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(K8sCanaryDeleteStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TERRAFORM_APPLY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(TerraformApplyStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TERRAFORM_PLAN_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(TerraformPlanStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TERRAFORM_DESTROY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(TerraformDestroyStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.TERRAFORM_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(TerraformRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.HELM_DEPLOY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(HelmDeployStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.HELM_ROLLBACK_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(HelmRollbackStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .namespace(SchemaNamespaceConstants.CD)
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .build();
}
