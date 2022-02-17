/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.registrars;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.states.CISpecStep;
import io.harness.states.CleanupStep;
import io.harness.states.DockerStep;
import io.harness.states.ECRStep;
import io.harness.states.GCRStep;
import io.harness.states.InitializeTaskStep;
import io.harness.states.IntegrationStageStepPMS;
import io.harness.states.PluginStep;
import io.harness.states.RestoreCacheGCSStep;
import io.harness.states.RestoreCacheS3Step;
import io.harness.states.RunStep;
import io.harness.states.RunTestsStep;
import io.harness.states.SaveCacheGCSStep;
import io.harness.states.SaveCacheS3Step;
import io.harness.states.SecurityStep;
import io.harness.states.UploadToArtifactoryStep;
import io.harness.states.UploadToGCSStep;
import io.harness.states.UploadToS3Step;
import io.harness.states.codebase.CodeBaseStep;
import io.harness.states.codebase.CodeBaseTaskStep;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class ExecutionRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();

    engineSteps.put(InitializeTaskStep.STEP_TYPE, InitializeTaskStep.class);
    engineSteps.put(CleanupStep.STEP_TYPE, CleanupStep.class);
    engineSteps.put(RunStep.STEP_TYPE, RunStep.class);
    engineSteps.put(PluginStep.STEP_TYPE, PluginStep.class);
    engineSteps.put(SecurityStep.STEP_TYPE, SecurityStep.class);
    engineSteps.put(ECRStep.STEP_TYPE, ECRStep.class);
    engineSteps.put(GCRStep.STEP_TYPE, GCRStep.class);
    engineSteps.put(DockerStep.STEP_TYPE, DockerStep.class);
    engineSteps.put(UploadToS3Step.STEP_TYPE, UploadToS3Step.class);
    engineSteps.put(SaveCacheS3Step.STEP_TYPE, SaveCacheS3Step.class);
    engineSteps.put(RestoreCacheS3Step.STEP_TYPE, RestoreCacheS3Step.class);
    engineSteps.put(UploadToGCSStep.STEP_TYPE, UploadToGCSStep.class);
    engineSteps.put(SaveCacheGCSStep.STEP_TYPE, SaveCacheGCSStep.class);
    engineSteps.put(RestoreCacheGCSStep.STEP_TYPE, RestoreCacheGCSStep.class);
    engineSteps.put(UploadToArtifactoryStep.STEP_TYPE, UploadToArtifactoryStep.class);
    engineSteps.put(RunTestsStep.STEP_TYPE, RunTestsStep.class);
    engineSteps.put(IntegrationStageStepPMS.STEP_TYPE, IntegrationStageStepPMS.class);
    engineSteps.put(CodeBaseStep.STEP_TYPE, CodeBaseStep.class);
    engineSteps.put(CodeBaseTaskStep.STEP_TYPE, CodeBaseTaskStep.class);
    engineSteps.put(CISpecStep.STEP_TYPE, CISpecStep.class);
    engineSteps.putAll(OrchestrationStepsModuleSdkStepRegistrar.getEngineSteps());
    return engineSteps;
  }
}
