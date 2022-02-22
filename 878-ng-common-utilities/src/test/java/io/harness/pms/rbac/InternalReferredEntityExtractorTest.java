/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.rbac;

import static io.harness.rule.OwnerRule.SAHIL;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitysetupusage.dto.EntityReferencesDTO;
import io.harness.remote.client.NGRestUtils;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NGRestUtils.class})
@OwnedBy(HarnessTeam.PIPELINE)
public class InternalReferredEntityExtractorTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";
  @Mock EntitySetupUsageClient entitySetupUsageClient;
  @InjectMocks InternalReferredEntityExtractor internalReferredEntityExtractor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyInteractions() {
    Mockito.verifyNoMoreInteractions(entitySetupUsageClient);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testExtractInternalEntities() {
    String dummy = "dummy";
    List<EntityDetail> entityDetailList = new ArrayList<>();

    PowerMockito.mockStatic(NGRestUtils.class);
    Mockito.when(NGRestUtils.getResponseWithRetry(Mockito.any(), Mockito.any()))
        .thenReturn(EntityReferencesDTO.builder().entitySetupUsageBatchList(new ArrayList<>()).build());

    for (int i = 0; i < 20; i++) {
      String name = dummy + i;
      entityDetailList.add(getEntityDetail(name, EntityType.CONNECTORS));
    }
    internalReferredEntityExtractor.extractInternalEntities(ACCOUNT_ID, entityDetailList);

    Mockito.verify(entitySetupUsageClient)
        .listAllReferredUsagesBatch(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
  }
  private EntityDetail getEntityDetail(String name, EntityType entityType) {
    return EntityDetail.builder()
        .entityRef(IdentifierRef.builder().identifier(name).accountIdentifier(ACCOUNT_ID).build())
        .type(entityType)
        .build();
  }
}
