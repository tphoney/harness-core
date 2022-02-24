/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.ANJAN;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.monitoredService.ChangeSourceDTO;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.KubernetesChangeSourceSpec;
import io.harness.cvng.core.beans.params.MonitoredServiceParams;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.entities.changeSource.ChangeSource.ChangeSourceKeys;
import io.harness.cvng.core.entities.changeSource.KubernetesChangeSource;
import io.harness.cvng.core.services.api.ChangeEventService;
import io.harness.cvng.core.services.api.FeatureFlagService;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.utils.FeatureFlagNames;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class ChangeSourceServiceImplTest extends CvNextGenTestBase {
  @Inject ChangeSourceService changeSourceService;
  @Inject HPersistence hPersistence;
  @Inject VerificationManagerService verificationManagerService;
  @Mock ChangeEventService changeEventService;
  BuilderFactory builderFactory;

  MonitoredServiceParams monitoredServiceParams;

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(changeSourceService, "changeEventService", changeEventService, true);

    builderFactory = BuilderFactory.getDefault();
    monitoredServiceParams = builderFactory.getContext().getMonitoredServiceParams();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate() {
    ChangeSourceDTO changeSourceDTO = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> changeSourceDTOToBeCreated = new HashSet<>(Arrays.asList(changeSourceDTO));

    changeSourceService.create(monitoredServiceParams, changeSourceDTOToBeCreated);

    Set<ChangeSourceDTO> changeSourceDTOSetFromDb = changeSourceService.get(
        monitoredServiceParams.getServiceEnvironmentParams(), Arrays.asList(changeSourceDTO.getIdentifier()));

    assertThat(changeSourceDTOSetFromDb.size()).isEqualTo(1);
    ChangeSource changeSource =
        changeSourceService.get(monitoredServiceParams.getServiceEnvironmentParams(), changeSourceDTO.getIdentifier());
    assertThat(changeSource.isEligibleForDemo()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_forDemoWithFFOff() {
    ChangeSourceDTO changeSourceDTO =
        builderFactory.getHarnessCDChangeSourceDTOBuilder().identifier("cdng_dev").build();
    Set<ChangeSourceDTO> changeSourceDTOToBeCreated = new HashSet<>(Arrays.asList(changeSourceDTO));
    changeSourceService.create(monitoredServiceParams, changeSourceDTOToBeCreated);
    ChangeSource changeSource =
        changeSourceService.get(monitoredServiceParams.getServiceEnvironmentParams(), changeSourceDTO.getIdentifier());
    assertThat(changeSource).isNotNull();
    assertThat(changeSource.isConfiguredForDemo()).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_forDemoWithFFOn() throws IllegalAccessException {
    ChangeSourceDTO changeSourceDTO =
        builderFactory.getHarnessCDChangeSourceDTOBuilder().identifier("cdng_dev").build();
    Set<ChangeSourceDTO> changeSourceDTOToBeCreated = new HashSet<>(Arrays.asList(changeSourceDTO));
    FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
    when(featureFlagService.isFeatureFlagEnabled(
             eq(builderFactory.getContext().getAccountId()), eq(FeatureFlagNames.CVNG_MONITORED_SERVICE_DEMO)))
        .thenReturn(true);
    FieldUtils.writeField(changeSourceService, "featureFlagService", featureFlagService, true);
    changeSourceService.create(monitoredServiceParams, changeSourceDTOToBeCreated);
    ChangeSource changeSource =
        changeSourceService.get(monitoredServiceParams.getServiceEnvironmentParams(), changeSourceDTO.getIdentifier());
    assertThat(changeSource).isNotNull();
    assertThat(changeSource.isConfiguredForDemo()).isTrue();
    assertThat(changeSource.isEligibleForDemo()).isTrue();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreate_withNonUniqueIdentifier() {
    ChangeSourceDTO changeSourceDto1 = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    ChangeSourceDTO changeSourceDto2 =
        builderFactory.getHarnessCDChangeSourceDTOBuilder().identifier(changeSourceDto1.getIdentifier()).build();
    Set<ChangeSourceDTO> changeSourceDTOToBeCreated = new HashSet<>(Arrays.asList(changeSourceDto1, changeSourceDto2));
    assertThatThrownBy(() -> changeSourceService.create(monitoredServiceParams, changeSourceDTOToBeCreated))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpdate() {
    ChangeSourceDTO changeSourceDto = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    ChangeSourceDTO kubeChangeSourceDto = builderFactory.getKubernetesChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> updateDtos = new HashSet<>(Arrays.asList(changeSourceDto, kubeChangeSourceDto));
    String updatedName = "UPDATED_NAME";

    changeSourceService.create(monitoredServiceParams, updateDtos);
    ChangeSource initialChangeSource = getChangeSourceFromDb(changeSourceDto.getIdentifier());
    changeSourceDto.setName(updatedName);
    updateDtos.remove(kubeChangeSourceDto);
    changeSourceService.update(monitoredServiceParams, updateDtos);

    ChangeSource updatedChangeSource = getChangeSourceFromDb(changeSourceDto.getIdentifier());

    assertThat(updatedChangeSource.getUuid()).isEqualTo(initialChangeSource.getUuid());
    assertThat(updatedChangeSource.getCreatedAt()).isEqualTo(initialChangeSource.getCreatedAt());
    assertThat(updatedChangeSource.getName()).isEqualTo(updatedName);
    assertThat(getChangeSourceFromDb(kubeChangeSourceDto.getIdentifier())).isNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testDelete() {
    ChangeSourceDTO changeSourceDto = builderFactory.getHarnessCDChangeSourceDTOBuilder().build();
    Set<ChangeSourceDTO> dtos = new HashSet<>(Arrays.asList(changeSourceDto));
    changeSourceService.create(monitoredServiceParams, dtos);

    changeSourceService.delete(
        monitoredServiceParams.getServiceEnvironmentParams(), Arrays.asList(changeSourceDto.getIdentifier()));
    ChangeSource changeSourceFromDb = getChangeSourceFromDb(changeSourceDto.getIdentifier());

    assertThat(changeSourceFromDb).isNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testgetChangeSummary() {
    ChangeSummaryDTO changeSummaryDTO = ChangeSummaryDTO.builder().build();
    when(changeEventService.getChangeSummary(eq(builderFactory.getContext().getServiceEnvironmentParams()),
             eq(new ArrayList<>()), eq(Instant.ofEpochSecond(100)), eq(Instant.ofEpochSecond(100))))
        .thenReturn(changeSummaryDTO);
    ChangeSummaryDTO result =
        changeSourceService.getChangeSummary(builderFactory.getContext().getServiceEnvironmentParams(),
            new ArrayList<>(), Instant.ofEpochSecond(100), Instant.ofEpochSecond(100));
    assertThat(result).isEqualTo(changeSummaryDTO);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testEnqueueDataCollectionTask() throws IllegalAccessException {
    verificationManagerService = spy(verificationManagerService);
    FieldUtils.writeField(changeSourceService, "verificationManagerService", verificationManagerService, true);
    String kubeConnectorIdentifier = randomAlphanumeric(20);
    String datacollectionTaskId = generateUuid();
    String identifier = randomAlphanumeric(20);
    String name = randomAlphanumeric(20);

    KubernetesChangeSource kubeChangeSource = KubernetesChangeSource.builder()
                                                  .connectorIdentifier(kubeConnectorIdentifier)
                                                  .dataCollectionRequired(true)
                                                  .identifier(identifier)
                                                  .name(name)
                                                  .type(ChangeSourceType.KUBERNETES)
                                                  .orgIdentifier(builderFactory.getContext().getOrgIdentifier())
                                                  .projectIdentifier(builderFactory.getContext().getProjectIdentifier())
                                                  .serviceIdentifier(builderFactory.getContext().getServiceIdentifier())
                                                  .accountId(builderFactory.getContext().getAccountId())
                                                  .envIdentifier(builderFactory.getContext().getEnvIdentifier())
                                                  .serviceIdentifier(builderFactory.getContext().getServiceIdentifier())
                                                  .build();

    HashSet<ChangeSourceDTO> changeSourceDTOS = new HashSet<>();
    changeSourceDTOS.add(ChangeSourceDTO.builder()
                             .enabled(true)
                             .identifier(identifier)
                             .name(name)
                             .type(ChangeSourceType.KUBERNETES)
                             .spec(KubernetesChangeSourceSpec.builder().connectorRef(kubeConnectorIdentifier).build())
                             .build());

    changeSourceService.create(monitoredServiceParams, changeSourceDTOS);
    ChangeSource changeSource =
        changeSourceService.get(monitoredServiceParams.getServiceEnvironmentParams(), identifier);
    kubeChangeSource.setUuid(changeSource.getUuid());

    when(verificationManagerService.createDataCollectionTask(eq(builderFactory.getContext().getAccountId()),
             eq(builderFactory.getContext().getOrgIdentifier()), eq(builderFactory.getContext().getProjectIdentifier()),
             any()))
        .thenReturn(datacollectionTaskId);
    changeSourceService.enqueueDataCollectionTask(kubeChangeSource);

    changeSource = changeSourceService.get(monitoredServiceParams.getServiceEnvironmentParams(), identifier);
    assertThat(changeSource.getDataCollectionTaskId()).isEqualTo(datacollectionTaskId);
  }

  private ChangeSource getChangeSourceFromDb(String identifier) {
    return hPersistence.createQuery(ChangeSource.class).filter(ChangeSourceKeys.identifier, identifier).get();
  }
}
