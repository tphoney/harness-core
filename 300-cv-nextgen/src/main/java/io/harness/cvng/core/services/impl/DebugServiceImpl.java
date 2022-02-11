/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.SLODebugResponse;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.DebugService;
import io.harness.cvng.servicelevelobjective.entities.SLOHealthIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.services.api.SLOHealthIndicatorService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelIndicatorService;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelObjectiveService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.List;

public class DebugServiceImpl implements DebugService {
  @Inject ServiceLevelObjectiveService serviceLevelObjectiveService;
  @Inject ServiceLevelIndicatorService serviceLevelIndicatorService;
  @Inject SLOHealthIndicatorService sloHealthIndicatorService;

  @Override
  public SLODebugResponse get(ProjectParams projectParams, String identifier) {
    ServiceLevelObjective serviceLevelObjective = serviceLevelObjectiveService.getEntity(projectParams, identifier);

    Preconditions.checkNotNull(serviceLevelObjective, "Value of Identifier is not present in database");

    List<ServiceLevelIndicator> serviceLevelIndicatorList =
        serviceLevelIndicatorService.getEntities(projectParams, serviceLevelObjective.getServiceLevelIndicators());

    SLOHealthIndicator sloHealthIndicator =
        sloHealthIndicatorService.getBySLOIdentifier(projectParams, serviceLevelObjective.getIdentifier());

    return SLODebugResponse.builder()
        .projectParams(projectParams)
        .serviceLevelObjective(serviceLevelObjective)
        .serviceLevelIndicatorList(serviceLevelIndicatorList)
        .sloHealthIndicator(sloHealthIndicator)
        .build();
  }
}
