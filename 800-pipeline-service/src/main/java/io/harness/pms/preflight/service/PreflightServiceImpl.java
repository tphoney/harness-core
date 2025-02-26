/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.preflight.service;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.manage.ManagedExecutorService;
import io.harness.ng.core.EntityDetail;
import io.harness.pms.inputset.InputSetErrorDTOPMS;
import io.harness.pms.inputset.InputSetErrorResponseDTOPMS;
import io.harness.pms.merger.helpers.InputSetMergeHelper;
import io.harness.pms.ngpipeline.inputset.helpers.InputSetErrorsHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineSetupUsageHelper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.preflight.PreFlightCause;
import io.harness.pms.preflight.PreFlightDTO;
import io.harness.pms.preflight.PreFlightEntityErrorInfo;
import io.harness.pms.preflight.PreFlightStatus;
import io.harness.pms.preflight.connector.ConnectorCheckResponse;
import io.harness.pms.preflight.connector.ConnectorPreflightHandler;
import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.pms.preflight.entity.PreFlightEntity.PreFlightEntityKeys;
import io.harness.pms.preflight.handler.AsyncPreFlightHandler;
import io.harness.pms.preflight.inputset.PipelineInputResponse;
import io.harness.pms.preflight.mapper.PreFlightMapper;
import io.harness.pms.rbac.validator.PipelineRbacService;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.preflight.PreFlightRepository;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Date;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PreflightServiceImpl implements PreflightService {
  private static final ExecutorService executorService = new ManagedExecutorService(Executors.newFixedThreadPool(1));

  @Inject PreFlightRepository preFlightRepository;
  @Inject ConnectorPreflightHandler connectorPreflightHandler;
  @Inject PMSPipelineService pmsPipelineService;
  @Inject PipelineSetupUsageHelper pipelineSetupUsageHelper;
  @Inject PipelineRbacService pipelineRbacServiceImpl;

  @Override
  public String startPreflightCheck(@NotNull String accountId, @NotNull String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String pipelineIdentifier, String inputSetPipelineYaml) {
    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("The given pipeline id [%s] does not exist", pipelineIdentifier));
    }
    String pipelineYaml;
    if (EmptyPredicate.isEmpty(inputSetPipelineYaml)) {
      pipelineYaml = pipelineEntity.get().getYaml();
    } else {
      pipelineYaml =
          InputSetMergeHelper.mergeInputSetIntoPipeline(pipelineEntity.get().getYaml(), inputSetPipelineYaml, false);
    }
    List<EntityDetail> entityDetails = pipelineSetupUsageHelper.getReferencesOfPipeline(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml, null);
    pipelineRbacServiceImpl.validateStaticallyReferredEntities(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml, entityDetails);

    Map<String, InputSetErrorResponseDTOPMS> errorResponseMap = EmptyPredicate.isEmpty(inputSetPipelineYaml)
        ? null
        : InputSetErrorsHelper.getUuidToErrorResponseMap(pipelineEntity.get().getYaml(), inputSetPipelineYaml);
    PreFlightEntity preFlightEntitySaved;
    if (errorResponseMap == null) {
      preFlightEntitySaved = saveInitialPreflightEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
          pipelineYaml, entityDetails, Collections.emptyList());
    } else {
      List<PipelineInputResponse> pipelineInputResponses = getPipelineInputResponses(errorResponseMap);
      preFlightEntitySaved = saveInitialPreflightEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
          pipelineYaml, entityDetails, pipelineInputResponses);
    }

    executorService.submit(AsyncPreFlightHandler.builder()
                               .entity(preFlightEntitySaved)
                               .entityDetails(entityDetails)
                               .preflightService(this)
                               .build());
    return preFlightEntitySaved.getUuid();
  }

  @Override
  public PreFlightEntity saveInitialPreflightEntity(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String pipelineYaml, List<EntityDetail> entityDetails,
      List<PipelineInputResponse> pipelineInputResponses) {
    PreFlightEntity preFlightEntity =
        PreFlightMapper.toEmptyEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineYaml);

    preFlightEntity.setPipelineInputResponse(pipelineInputResponses);

    List<EntityDetail> connectorUsages = entityDetails.stream()
                                             .filter(entityDetail -> entityDetail.getType() == EntityType.CONNECTORS)
                                             .collect(Collectors.toList());
    List<ConnectorCheckResponse> connectorTemplates =
        connectorPreflightHandler.getConnectorCheckResponseTemplate(connectorUsages);
    preFlightEntity.setConnectorCheckResponse(connectorTemplates);
    return preFlightRepository.save(preFlightEntity);
  }

  public void updateStatus(String id, PreFlightStatus status, PreFlightEntityErrorInfo errorInfo) {
    Criteria criteria = Criteria.where(PreFlightEntityKeys.uuid).is(id);
    Update update = new Update();
    update.set(PreFlightEntityKeys.preFlightStatus, status);
    if (errorInfo != null) {
      update.set(PreFlightEntityKeys.errorInfo, errorInfo);
    }
    if (status == PreFlightStatus.FAILURE || status == PreFlightStatus.SUCCESS) {
      update.set(
          PreFlightEntityKeys.validUntil, Date.from(OffsetDateTime.now().plus(Duration.ofMinutes(5)).toInstant()));
    }
    preFlightRepository.update(criteria, update);
  }

  @Override
  public List<ConnectorCheckResponse> updateConnectorCheckResponses(String accountId, String orgId, String projectId,
      String preflightEntityId, Map<String, Object> fqnToObjectMapMergedYaml, List<EntityDetail> connectorUsages) {
    List<ConnectorCheckResponse> connectorCheckResponses =
        connectorPreflightHandler.getConnectorCheckResponsesForReferredConnectors(
            accountId, orgId, projectId, fqnToObjectMapMergedYaml, connectorUsages);
    if (!EmptyPredicate.isEmpty(connectorCheckResponses)) {
      Criteria criteria = Criteria.where(PreFlightEntityKeys.uuid).is(preflightEntityId);
      Update update = new Update();
      update.set(PreFlightEntityKeys.connectorCheckResponse, connectorCheckResponses);
      preFlightRepository.update(criteria, update);
    }
    return connectorCheckResponses;
  }

  @Override
  public PreFlightDTO getPreflightCheckResponse(String preflightCheckId) {
    Optional<PreFlightEntity> optionalPreFlightEntity = preFlightRepository.findById(preflightCheckId);
    if (!optionalPreFlightEntity.isPresent()) {
      throw new InvalidRequestException("Could not find pre flight check data corresponding to id:" + preflightCheckId);
    }
    PreFlightEntity preFlightEntity = optionalPreFlightEntity.get();
    return PreFlightMapper.toPreFlightDTO(preFlightEntity);
  }

  @VisibleForTesting
  List<PipelineInputResponse> getPipelineInputResponses(Map<String, InputSetErrorResponseDTOPMS> errorResponseMap) {
    List<PipelineInputResponse> res = new ArrayList<>();
    errorResponseMap.keySet().forEach(key -> {
      List<InputSetErrorDTOPMS> errors = errorResponseMap.get(key).getErrors();
      List<PreFlightCause> preFlightCauses =
          errors.stream()
              .map(error -> PreFlightCause.builder().cause(error.getMessage()).build())
              .collect(Collectors.toList());
      PreFlightEntityErrorInfo errorInfo = PreFlightEntityErrorInfo.builder()
                                               .summary("Runtime value provided for " + key + " is wrong")
                                               .causes(preFlightCauses)
                                               .build();
      res.add(PipelineInputResponse.builder()
                  .success(false)
                  .errorInfo(errorInfo)
                  .fqn(key)
                  .stageName(YamlUtils.getStageIdentifierFromFqn(key))
                  .build());
    });
    return res;
  }
}
