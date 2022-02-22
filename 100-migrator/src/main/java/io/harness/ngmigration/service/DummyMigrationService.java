/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import static software.wings.ngmigration.NGMigrationEntityType.DUMMY_HEAD;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngmigration.beans.DummyNode;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.ngmigration.client.NGClient;
import io.harness.ngmigration.client.PmsClient;

import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;

@OwnedBy(HarnessTeam.CDC)
public class DummyMigrationService implements NgMigrationService {
  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    throw new NotImplementedException("Dummy Method not implemented");
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    CgEntityId cgEntityId = CgEntityId.builder().type(DUMMY_HEAD).id(DUMMY_HEAD.name()).build();
    return DiscoveryNode.builder()
        .entityNode(CgEntityNode.builder()
                        .type(DUMMY_HEAD)
                        .id(DUMMY_HEAD.name())
                        .entityId(cgEntityId)
                        .entity(DummyNode.builder().name("HEAD").build())
                        .build())
        .children(new HashSet<>())
        .build();
  }

  @Override
  public NGMigrationStatus canMigrate(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    return NGMigrationStatus.builder().status(true).build();
  }

  @Override
  public void migrate(String auth, NGClient ngClient, PmsClient pmsClient, MigrationInputDTO inputDTO,
      NGYamlFile yamlFile) throws IOException {
    /*
      We do not migrate dummy node. This is purely for help
     */
  }

  @Override
  public List<NGYamlFile> getYamls(MigrationInputDTO inputDTO, Map<CgEntityId, CgEntityNode> entities,
      Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId, Map<CgEntityId, NgEntityDetail> migratedEntities) {
    return new ArrayList<>();
  }
}
