/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.vm;

import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.vm.CIVmExecuteStepTaskParams;
import io.harness.delegate.beans.ci.vm.VmTaskExecutionResponse;
import io.harness.delegate.beans.ci.vm.steps.VmRunStep;
import io.harness.delegate.task.citasks.vm.helper.HttpHelper;
import io.harness.delegate.task.citasks.vm.helper.StepExecutionHelper;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIVMExecuteStepTaskHandlerTest extends CategoryTest {
  @Mock private HttpHelper httpHelper;
  @Mock private StepExecutionHelper stepExecutionHelper;
  @InjectMocks private io.harness.delegate.task.citasks.vm.CIVMExecuteStepTaskHandler CIVMExecuteStepTaskHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternal() throws IOException {
    CIVmExecuteStepTaskParams params = CIVmExecuteStepTaskParams.builder()
                                           .stageRuntimeId("stage")
                                           .stepRuntimeId("step")
                                           .stepInfo(VmRunStep.builder().build())
                                           .build();
    when(stepExecutionHelper.callRunnerForStepExecution(any()))
        .thenReturn(VmTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build());
    VmTaskExecutionResponse response = CIVMExecuteStepTaskHandler.executeTaskInternal(params, "");
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalFailure() {
    CIVmExecuteStepTaskParams params = CIVmExecuteStepTaskParams.builder()
                                           .stageRuntimeId("stage")
                                           .stepRuntimeId("test")
                                           .stepInfo(VmRunStep.builder().build())
                                           .build();
    when(stepExecutionHelper.callRunnerForStepExecution(any()))
        .thenReturn(VmTaskExecutionResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                        .errorMessage("exit code 1")
                        .build());

    VmTaskExecutionResponse response = CIVMExecuteStepTaskHandler.executeTaskInternal(params, "");
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }
}
