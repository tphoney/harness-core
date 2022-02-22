/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.retry.RetryExecutionParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.plan.execution.beans.ExecArgs;
import io.harness.pms.plan.execution.beans.TriggerFlowPlanDetails;
import io.harness.pms.plan.execution.beans.dto.RunStageRequestDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class PipelineExecutor {
  ExecutionHelper executionHelper;
  ValidateAndMergeHelper validateAndMergeHelper;
  PlanExecutionMetadataService planExecutionMetadataService;
  RetryExecutionHelper retryExecutionHelper;
  PMSPipelineTemplateHelper pipelineTemplateHelper;

  public PlanExecutionResponseDto runPipelineWithInputSetPipelineYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, String runtimeInputYaml, boolean useV2) {
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, null, moduleType,
        runtimeInputYaml, Collections.emptyList(), Collections.emptyMap(), useV2);
  }

  public PlanExecutionResponseDto runPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, List<String> inputSetReferences,
      String pipelineBranch, String pipelineRepoID) {
    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId,
        orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch, pipelineRepoID, null);
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, null, moduleType,
        mergedRuntimeInputYaml, Collections.emptyList(), Collections.emptyMap(), false);
  }

  public PlanExecutionResponseDto runStagesWithRuntimeInputYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, RunStageRequestDTO runStageRequestDTO, boolean useV2) {
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, null, moduleType,
        runStageRequestDTO.getRuntimeInputYaml(), runStageRequestDTO.getStageIdentifiers(),
        runStageRequestDTO.getExpressionValues(), useV2);
  }

  public PlanExecutionResponseDto rerunStagesWithRuntimeInputYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, String originalExecutionId, RunStageRequestDTO runStageRequestDTO, boolean useV2) {
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, originalExecutionId,
        moduleType, runStageRequestDTO.getRuntimeInputYaml(), runStageRequestDTO.getStageIdentifiers(),
        runStageRequestDTO.getExpressionValues(), useV2);
  }

  public PlanExecutionResponseDto rerunPipelineWithInputSetPipelineYaml(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, String originalExecutionId,
      String runtimeInputYaml, boolean useV2) {
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, originalExecutionId,
        moduleType, runtimeInputYaml, Collections.emptyList(), Collections.emptyMap(), useV2);
  }

  public PlanExecutionResponseDto rerunPipelineWithInputSetReferencesList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String moduleType, String originalExecutionId,
      List<String> inputSetReferences, String pipelineBranch, String pipelineRepoID) {
    String mergedRuntimeInputYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId,
        orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch, pipelineRepoID, null);
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, originalExecutionId,
        moduleType, mergedRuntimeInputYaml, Collections.emptyList(), Collections.emptyMap(), false);
  }

  public PlanExecutionResponseDto startPlanExecution(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String originalExecutionId, String moduleType, String runtimeInputYaml,
      List<String> stagesToRun, Map<String, String> expressionValues, boolean useV2) {
    return startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, originalExecutionId,
        moduleType, runtimeInputYaml, stagesToRun, expressionValues, useV2, null);
  }

  public PlanExecutionResponseDto startPlanExecution(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String originalExecutionId, String moduleType, String runtimeInputYaml,
      List<String> stagesToRun, Map<String, String> expressionValues, boolean useV2,
      TriggerFlowPlanDetails triggerFlowPlanDetails) {
    PipelineEntity pipelineEntity =
        executionHelper.fetchPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    if (EmptyPredicate.isNotEmpty(stagesToRun) && !pipelineEntity.shouldAllowStageExecutions()) {
      throw new InvalidRequestException(
          String.format("Stage executions are not allowed for pipeline [%s]", pipelineIdentifier));
    }
    ExecutionTriggerInfo triggerInfo = executionHelper.buildTriggerInfo(triggerFlowPlanDetails, originalExecutionId);

    // RetryExecutionParameters
    RetryExecutionParameters retryExecutionParameters = buildRetryExecutionParameters(false, null, null, null);

    ExecArgs execArgs = executionHelper.buildExecutionArgs(pipelineEntity, moduleType, runtimeInputYaml, stagesToRun,
        expressionValues, triggerInfo, originalExecutionId, retryExecutionParameters, triggerFlowPlanDetails);
    PlanExecution planExecution;
    if (useV2) {
      planExecution = executionHelper.startExecutionV2(accountId, orgIdentifier, projectIdentifier,
          execArgs.getMetadata(), execArgs.getPlanExecutionMetadata(), false, null, null);
    } else {
      planExecution = executionHelper.startExecution(accountId, orgIdentifier, projectIdentifier,
          execArgs.getMetadata(), execArgs.getPlanExecutionMetadata(), false, null, null);
    }
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity))
        .build();
  }

  public PlanExecutionResponseDto retryPipelineWithInputSetPipelineYaml(@NotNull String accountId,
      @NotNull String orgIdentifier, @NotNull String projectIdentifier, @NotNull String pipelineIdentifier,
      String moduleType, String inputSetPipelineYaml, String previousExecutionId, List<String> retryStagesIdentifier,
      boolean runAllStages, boolean useV2) {
    PipelineEntity pipelineEntity =
        executionHelper.fetchPipelineEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);

    if (!runAllStages && retryStagesIdentifier.size() > 1) {
      // run only failed stage
      retryStagesIdentifier = retryExecutionHelper.fetchOnlyFailedStages(previousExecutionId, retryStagesIdentifier);
    }

    ExecutionTriggerInfo triggerInfo = executionHelper.buildTriggerInfoForManualFlow(null);
    Optional<PlanExecutionMetadata> optionalPlanExecutionMetadata =
        planExecutionMetadataService.findByPlanExecutionId(previousExecutionId);

    if (!optionalPlanExecutionMetadata.isPresent()) {
      throw new InvalidRequestException(String.format("No plan exist for %s planExecutionId", previousExecutionId));
    }
    String previousProcessedYaml = optionalPlanExecutionMetadata.get().getProcessedYaml();
    List<String> identifierOfSkipStages = new ArrayList<>();

    // RetryExecutionParameters
    RetryExecutionParameters retryExecutionParameters =
        buildRetryExecutionParameters(true, previousProcessedYaml, retryStagesIdentifier, identifierOfSkipStages);

    ExecArgs execArgs = executionHelper.buildExecutionArgs(pipelineEntity, moduleType, inputSetPipelineYaml, null, null,
        triggerInfo, previousExecutionId, retryExecutionParameters, null);
    PlanExecution planExecution;
    if (useV2) {
      planExecution =
          executionHelper.startExecutionV2(accountId, orgIdentifier, projectIdentifier, execArgs.getMetadata(),
              execArgs.getPlanExecutionMetadata(), true, identifierOfSkipStages, previousExecutionId);
    } else {
      planExecution =
          executionHelper.startExecution(accountId, orgIdentifier, projectIdentifier, execArgs.getMetadata(),
              execArgs.getPlanExecutionMetadata(), true, identifierOfSkipStages, previousExecutionId);
    }
    return PlanExecutionResponseDto.builder()
        .planExecution(planExecution)
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(pipelineEntity))
        .build();
  }

  public RetryExecutionParameters buildRetryExecutionParameters(
      boolean isRetry, String processedYaml, List<String> stagesIdentifier, List<String> identifierOfSkipStages) {
    if (!isRetry) {
      return RetryExecutionParameters.builder().isRetry(false).build();
    }

    return RetryExecutionParameters.builder()
        .isRetry(true)
        .previousProcessedYaml(processedYaml)
        .retryStagesIdentifier(stagesIdentifier)
        .identifierOfSkipStages(identifierOfSkipStages)
        .build();
  }
}
