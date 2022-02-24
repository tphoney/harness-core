/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeEventMetadata;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;

import com.google.inject.Inject;

public abstract class ChangeEventMetaDataTransformer<E extends Activity, M extends ChangeEventMetadata> {
  @Inject NextGenService nextGenService;

  public abstract E getEntity(ChangeEventDTO changeEventDTO);

  public final ChangeEventDTO getDTO(E activity) {
    ServiceResponseDTO serviceResponseDTO = nextGenService.getService(activity.getAccountId(),
        activity.getOrgIdentifier(), activity.getProjectIdentifier(), activity.getServiceIdentifier());
    EnvironmentResponseDTO environmentResponseDTO = nextGenService.getEnvironment(activity.getAccountId(),
        activity.getOrgIdentifier(), activity.getProjectIdentifier(), activity.getEnvironmentIdentifier());
    String serviceName = serviceResponseDTO != null ? serviceResponseDTO.getName() : null;
    String environmentName = environmentResponseDTO != null ? environmentResponseDTO.getName() : null;
    return ChangeEventDTO.builder()
        .id(activity.getUuid())
        .accountId(activity.getAccountId())
        .orgIdentifier(activity.getOrgIdentifier())
        .projectIdentifier(activity.getProjectIdentifier())
        .serviceIdentifier(activity.getServiceIdentifier())
        .serviceName(serviceName)
        .changeSourceIdentifier(activity.getChangeSourceIdentifier())
        .monitoredServiceIdentifier(activity.getMonitoredServiceIdentifier())
        .envIdentifier(activity.getEnvironmentIdentifier())
        .environmentName(environmentName)
        .name(activity.getActivityName())
        .eventTime(activity.getEventTime().toEpochMilli())
        .type(ChangeSourceType.ofActivityType(activity.getType()))
        .metadata(getMetadata(activity))
        .build();
  }

  protected abstract M getMetadata(E activity);
}
