/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.dto.secrets.SecretRequestWrapper;
import io.harness.serializer.morphia.FeedbackMorphiaRegistrars;
import io.harness.serializer.morphia.InvitesMorphiaRegistrar;
import io.harness.serializer.morphia.MockRoleAssignmentMorphiaRegistrar;
import io.harness.serializer.morphia.NGMorphiaRegistrars;
import io.harness.serializer.morphia.NgUserGroupMorphiaRegistrar;
import io.harness.serializer.morphia.NgUserMorphiaRegistrar;
import io.harness.serializer.morphia.NgUserProfileMorphiaRegistrars;
import io.harness.serializer.morphia.ServiceAccountMorphiaRegistrars;
import io.harness.serializer.morphia.WebhookMorphiaRegistrars;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.serializer.registrars.NGCommonsRegistrars;
import org.mongodb.morphia.converters.TypeConverter;

@OwnedBy(HarnessTeam.PL)
public class NextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(AccessControlClientRegistrars.kryoRegistrars)
          .addAll(SecretManagerClientRegistrars.kryoRegistrars)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .addAll(GitOpsRegistrars.kryoRegistrars)
          .addAll(CDNGRegistrars.kryoRegistrars)
          .addAll(OutboxEventRegistrars.kryoRegistrars)
          .addAll(NGFileServiceRegistrars.kryoRegistrars)
          .addAll(NGAuditCommonsRegistrars.kryoRegistrars)
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .addAll(LicenseManagerRegistrars.kryoRegistrars)
          .add(PipelineServiceUtilKryoRegistrar.class)
          .addAll(DelegateTaskRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(SecretManagerClientRegistrars.morphiaRegistrars)
          .addAll(ConnectorNextGenRegistrars.morphiaRegistrars)
          .addAll(GitOpsRegistrars.morphiaRegistrars)
          .addAll(CDNGRegistrars.morphiaRegistrars)
          .add(NgUserGroupMorphiaRegistrar.class)
          .add(NgUserMorphiaRegistrar.class)
          .add(NgUserProfileMorphiaRegistrars.class)
          .add(WebhookMorphiaRegistrars.class)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .addAll(OutboxEventRegistrars.morphiaRegistrars)
          .addAll(NGFileServiceRegistrars.morphiaRegistrars)
          .addAll(GitSyncRegistrars.morphiaRegistrars)
          .addAll(NGAuditCommonsRegistrars.morphiaRegistrars)
          .add(MockRoleAssignmentMorphiaRegistrar.class)
          .add(InvitesMorphiaRegistrar.class)
          .addAll(PrimaryVersionManagerRegistrars.morphiaRegistrars)
          .addAll(LicenseManagerRegistrars.morphiaRegistrars)
          .addAll(SignupRegistrars.morphiaRegistrars)
          .add(ServiceAccountMorphiaRegistrars.class)
          .add(NGMorphiaRegistrars.class)
          .add(FeedbackMorphiaRegistrars.class)
          .addAll(InstanceRegistrars.morphiaRegistrars)
          .addAll(DelegateTaskRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder()
          .addAll(ConnectorNextGenRegistrars.yamlSchemaRegistrars)
          .addAll(GitOpsRegistrars.yamlSchemaRegistrars)
          .addAll(CDNGRegistrars.yamlSchemaRegistrars)
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.SECRETS)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(true)
                   .availableAtAccountLevel(true)
                   .clazz(SecretRequestWrapper.class)
                   .build())
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder().build();
}
