/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.azure.appservice.deployment;

import static io.harness.azure.model.AzureConstants.SLOT_SWAP;
import static io.harness.azure.model.AzureConstants.SLOT_SWAP_JOB_PROCESSOR_STR;
import static io.harness.azure.model.AzureConstants.START_DEPLOYMENT_SLOT;
import static io.harness.azure.model.AzureConstants.STOP_DEPLOYMENT_SLOT;
import static io.harness.rule.OwnerRule.ANIL;

import static software.wings.delegatetasks.azure.appservice.deployment.verifier.SlotStatusVerifier.SlotStatus.RUNNING;
import static software.wings.delegatetasks.azure.appservice.deployment.verifier.SlotStatusVerifier.SlotStatus.STOPPED;
import static software.wings.delegatetasks.azure.appservice.deployment.verifier.SlotStatusVerifier.SlotStatusVerifierType.START_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.deployment.verifier.SlotStatusVerifier.SlotStatusVerifierType.STOP_VERIFIER;
import static software.wings.delegatetasks.azure.appservice.deployment.verifier.SlotStatusVerifier.SlotStatusVerifierType.SWAP_VERIFIER;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.azure.client.AzureMonitorClient;
import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.delegatetasks.azure.AzureServiceCallBack;
import software.wings.delegatetasks.azure.appservice.deployment.verifier.SlotStatusVerifier;

import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.monitor.EventData;
import com.microsoft.azure.management.monitor.LocalizableString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class SlotSteadyStateCheckerTest extends CategoryTest {
  @Mock private LogCallback mockLogCallback;
  @InjectMocks private SlotSteadyStateChecker slotSteadyStateChecker;
  private final TimeLimiter timeLimiter = new FakeTimeLimiter();

  private static final String SOURCE_SLOT = "sourceSlot";
  private static final String TARGET_SLOT = "targetSlot";
  private static final String SLOT_ID = "slotId";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    on(slotSteadyStateChecker).set("timeLimiter", timeLimiter);
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testStopSlotSteadyState() {
    AzureWebClient azureWebClient = mock(AzureWebClient.class);
    AzureMonitorClient azureMonitorClient = mock(AzureMonitorClient.class);
    AzureServiceCallBack restCallBack = new AzureServiceCallBack(mockLogCallback, SLOT_SWAP);
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();

    SlotStatusVerifier statusVerifier = SlotStatusVerifier.getStatusVerifier(STOP_VERIFIER.name(), mockLogCallback,
        SOURCE_SLOT, azureWebClient, azureMonitorClient, azureWebClientContext, restCallBack);

    DeploymentSlot deploymentSlot = mock(DeploymentSlot.class);
    doReturn(Optional.of(deploymentSlot))
        .when(azureWebClient)
        .getDeploymentSlotByName(eq(azureWebClientContext), eq(SOURCE_SLOT));
    doReturn(RUNNING.name()).when(deploymentSlot).state();

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(new SlotStopStatusTask(deploymentSlot));
    executorService.shutdown();

    slotSteadyStateChecker.waitUntilCompleteWithTimeout(10, 1, mockLogCallback, STOP_DEPLOYMENT_SLOT, statusVerifier);
    verify(deploymentSlot, Mockito.atLeast(1)).state();
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testStartSlotSteadyState() {
    AzureWebClient azureWebClient = mock(AzureWebClient.class);
    AzureMonitorClient azureMonitorClient = mock(AzureMonitorClient.class);
    AzureServiceCallBack restCallBack = new AzureServiceCallBack(mockLogCallback, SLOT_SWAP);
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();

    SlotStatusVerifier statusVerifier = SlotStatusVerifier.getStatusVerifier(START_VERIFIER.name(), mockLogCallback,
        SOURCE_SLOT, azureWebClient, azureMonitorClient, azureWebClientContext, restCallBack);

    DeploymentSlot deploymentSlot = mock(DeploymentSlot.class);
    doReturn(Optional.of(deploymentSlot))
        .when(azureWebClient)
        .getDeploymentSlotByName(eq(azureWebClientContext), eq(SOURCE_SLOT));
    doReturn(RUNNING.name()).when(deploymentSlot).state();
    slotSteadyStateChecker.waitUntilCompleteWithTimeout(10, 10, mockLogCallback, START_DEPLOYMENT_SLOT, statusVerifier);
    verify(deploymentSlot, Mockito.atLeast(1)).state();

    // Failure
    doThrow(UncheckedTimeoutException.class).when(deploymentSlot).state();
    assertThatThrownBy(()
                           -> slotSteadyStateChecker.waitUntilCompleteWithTimeout(
                               10, 10, mockLogCallback, START_DEPLOYMENT_SLOT, statusVerifier))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Timed out waiting for executing operation");

    doThrow(Exception.class).when(deploymentSlot).state();
    assertThatThrownBy(()
                           -> slotSteadyStateChecker.waitUntilCompleteWithTimeout(
                               10, 10, mockLogCallback, START_DEPLOYMENT_SLOT, statusVerifier))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Error while waiting for executing operation");

    doReturn(Optional.empty()).when(azureWebClient).getDeploymentSlotByName(eq(azureWebClientContext), eq(SOURCE_SLOT));
    assertThatThrownBy(()
                           -> slotSteadyStateChecker.waitUntilCompleteWithTimeout(
                               10, 10, mockLogCallback, START_DEPLOYMENT_SLOT, statusVerifier))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unable to find deployment slot with name");
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testStartSlotSteadyStateCallbackFailed() {
    AzureWebClient azureWebClient = mock(AzureWebClient.class);
    AzureMonitorClient azureMonitorClient = mock(AzureMonitorClient.class);
    AzureServiceCallBack restCallBack = new AzureServiceCallBack(mockLogCallback, SLOT_SWAP);
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();

    SlotStatusVerifier statusVerifier = SlotStatusVerifier.getStatusVerifier(START_VERIFIER.name(), mockLogCallback,
        SOURCE_SLOT, azureWebClient, azureMonitorClient, azureWebClientContext, restCallBack);

    DeploymentSlot deploymentSlot = mock(DeploymentSlot.class);
    doReturn(Optional.of(deploymentSlot))
        .when(azureWebClient)
        .getDeploymentSlotByName(eq(azureWebClientContext), eq(SOURCE_SLOT));
    doReturn(STOPPED.name()).when(deploymentSlot).state();

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(new MarkFailureTask(restCallBack));
    executorService.shutdown();

    assertThatThrownBy(()
                           -> slotSteadyStateChecker.waitUntilCompleteWithTimeout(
                               10, 1, mockLogCallback, START_DEPLOYMENT_SLOT, statusVerifier))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Call back failed");
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSwapSlotSteadyState() {
    AzureWebClient azureWebClient = mock(AzureWebClient.class);
    AzureMonitorClient azureMonitorClient = mock(AzureMonitorClient.class);
    AzureServiceCallBack restCallBack = new AzureServiceCallBack(mockLogCallback, SLOT_SWAP);
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();

    DeploymentSlot deploymentSlot = mock(DeploymentSlot.class);
    doReturn(Optional.of(deploymentSlot))
        .when(azureWebClient)
        .getDeploymentSlotByName(eq(azureWebClientContext), eq(SOURCE_SLOT));
    doReturn(SLOT_ID).when(deploymentSlot).id();

    doReturn(createEventData())
        .when(azureMonitorClient)
        .listEventDataWithAllPropertiesByResourceId(eq(azureWebClientContext.getAzureConfig()),
            eq(azureWebClientContext.getSubscriptionId()), any(), any(), eq(SLOT_ID));

    doNothing().when(mockLogCallback).saveExecutionLog(any());

    SlotStatusVerifier statusVerifier = SlotStatusVerifier.getStatusVerifier(SWAP_VERIFIER.name(), mockLogCallback,
        SOURCE_SLOT, azureWebClient, azureMonitorClient, azureWebClientContext, restCallBack);

    slotSteadyStateChecker.waitUntilCompleteWithTimeout(10, 1, mockLogCallback, STOP_DEPLOYMENT_SLOT, statusVerifier);
    verify(azureMonitorClient, Mockito.atLeast(1))
        .listEventDataWithAllPropertiesByResourceId(eq(azureWebClientContext.getAzureConfig()),
            eq(azureWebClientContext.getSubscriptionId()), any(), any(), eq(SLOT_ID));
    assertThat(statusVerifier.getSteadyState().equalsIgnoreCase(RUNNING.name())).isTrue();

    reset(azureMonitorClient);
    doReturn(Collections.emptyList())
        .when(azureMonitorClient)
        .listEventDataWithAllPropertiesByResourceId(eq(azureWebClientContext.getAzureConfig()),
            eq(azureWebClientContext.getSubscriptionId()), any(), any(), eq(SLOT_ID));
    slotSteadyStateChecker.waitUntilCompleteWithTimeout(10, 1, mockLogCallback, STOP_DEPLOYMENT_SLOT, statusVerifier);
    verify(azureMonitorClient, Mockito.atLeast(1))
        .listEventDataWithAllPropertiesByResourceId(eq(azureWebClientContext.getAzureConfig()),
            eq(azureWebClientContext.getSubscriptionId()), any(), any(), eq(SLOT_ID));
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotSwapper() {
    AzureWebClient azureWebClient = mock(AzureWebClient.class);
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();
    AzureServiceCallBack restCallBack = new AzureServiceCallBack(mockLogCallback, SLOT_SWAP);
    restCallBack.failure(mock(Throwable.class));

    SwapSlotTask swapper = new SwapSlotTask(
        SOURCE_SLOT, TARGET_SLOT, azureWebClient, azureWebClientContext, restCallBack, mockLogCallback);

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(swapper);
    executorService.shutdown();

    try {
      Thread.sleep(5000);
    } catch (InterruptedException exception) {
      exception.printStackTrace();
    }
    verify(azureWebClient)
        .swapDeploymentSlotsAsync(eq(azureWebClientContext), eq(SOURCE_SLOT), eq(TARGET_SLOT), eq(restCallBack));
  }

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testSlotStatusVerifierFail() {
    AzureWebClient azureWebClient = mock(AzureWebClient.class);
    AzureMonitorClient azureMonitorClient = mock(AzureMonitorClient.class);
    AzureServiceCallBack restCallBack = new AzureServiceCallBack(mockLogCallback, SLOT_SWAP);
    AzureWebClientContext azureWebClientContext = getAzureWebClientContext();

    assertThatThrownBy(()
                           -> SlotStatusVerifier.getStatusVerifier("verifierType", mockLogCallback, SOURCE_SLOT,
                               azureWebClient, azureMonitorClient, azureWebClientContext, restCallBack))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("No slot status verifier defined for");
  }

  private List<EventData> createEventData() {
    List<EventData> eventDataList = new ArrayList<>();
    EventData slotSwap = mock(EventData.class);
    LocalizableString slotSwapLocalizableString = mock(LocalizableString.class);
    LocalizableString slotSwapStatusLocalizableString = mock(LocalizableString.class);
    doReturn(SLOT_SWAP_JOB_PROCESSOR_STR).when(slotSwap).caller();
    doReturn(slotSwapLocalizableString).when(slotSwap).operationName();
    doReturn(slotSwapStatusLocalizableString).when(slotSwap).status();
    doReturn("Succeeded").when(slotSwapStatusLocalizableString).localizedValue();
    doReturn("Microsoft.Web/sites/slots/SlotSwap/action").when(slotSwapLocalizableString).localizedValue();
    eventDataList.add(slotSwap);

    EventData endSlotWarmUp = mock(EventData.class);
    LocalizableString endSlotWarmUpLocalizableString = mock(LocalizableString.class);
    LocalizableString endSlotWarmUpStatusLocalizableString = mock(LocalizableString.class);
    doReturn(SLOT_SWAP_JOB_PROCESSOR_STR).when(endSlotWarmUp).caller();
    doReturn(endSlotWarmUpLocalizableString).when(endSlotWarmUp).operationName();
    doReturn(endSlotWarmUpStatusLocalizableString).when(endSlotWarmUp).status();
    doReturn("Succeeded").when(endSlotWarmUpStatusLocalizableString).localizedValue();
    doReturn("Microsoft.Web/sites/slots/EndSlotWarmup/action").when(endSlotWarmUpLocalizableString).localizedValue();
    eventDataList.add(endSlotWarmUp);

    EventData startSlotWarmUp = mock(EventData.class);
    LocalizableString startSlotWarmUpLocalizableString = mock(LocalizableString.class);
    LocalizableString startSlotWarmUpStatusLocalizableString = mock(LocalizableString.class);
    doReturn(SLOT_SWAP_JOB_PROCESSOR_STR).when(startSlotWarmUp).caller();
    doReturn(startSlotWarmUpLocalizableString).when(startSlotWarmUp).operationName();
    doReturn(startSlotWarmUpStatusLocalizableString).when(startSlotWarmUp).status();
    doReturn("Succeeded").when(startSlotWarmUpStatusLocalizableString).localizedValue();
    doReturn("Microsoft.Web/sites/slots/StartSlotWarmup/action")
        .when(startSlotWarmUpLocalizableString)
        .localizedValue();
    eventDataList.add(startSlotWarmUp);

    EventData applyConfig = mock(EventData.class);
    LocalizableString applyConfigLocalizableString = mock(LocalizableString.class);
    LocalizableString applyConfigStatusLocalizableString = mock(LocalizableString.class);
    doReturn(SLOT_SWAP_JOB_PROCESSOR_STR).when(applyConfig).caller();
    doReturn(applyConfigLocalizableString).when(applyConfig).operationName();
    doReturn(applyConfigStatusLocalizableString).when(applyConfig).status();
    doReturn("Succeeded").when(applyConfigStatusLocalizableString).localizedValue();
    doReturn("Microsoft.Web/sites/slots/ApplySlotConfig/action").when(applyConfigLocalizableString).localizedValue();
    eventDataList.add(applyConfig);

    EventData notStarted = mock(EventData.class);
    LocalizableString notStartedLocalizableString = mock(LocalizableString.class);
    LocalizableString notStartedStatusLocalizableString = mock(LocalizableString.class);
    doReturn(SLOT_SWAP_JOB_PROCESSOR_STR).when(notStarted).caller();
    doReturn(notStartedLocalizableString).when(notStarted).operationName();
    doReturn(notStartedStatusLocalizableString).when(notStarted).status();
    doReturn("Succeeded").when(notStartedStatusLocalizableString).localizedValue();
    doReturn("Microsoft.Web/sites/slots/RandomAction/action").when(notStartedLocalizableString).localizedValue();
    eventDataList.add(notStarted);
    return eventDataList;
  }

  private static class MarkFailureTask implements Runnable {
    private final AzureServiceCallBack restCallBack;

    MarkFailureTask(AzureServiceCallBack restCallBack) {
      this.restCallBack = restCallBack;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(5000);
        Throwable throwable = mock(Throwable.class);
        doReturn("Call back failed").when(throwable).getMessage();
        restCallBack.failure(throwable);
      } catch (InterruptedException exception) {
        exception.printStackTrace();
      }
    }
  }

  private static class SlotStopStatusTask implements Runnable {
    private final DeploymentSlot deploymentSlot;
    SlotStopStatusTask(DeploymentSlot deploymentSlot) {
      this.deploymentSlot = deploymentSlot;
    }

    @Override
    public void run() {
      try {
        doReturn(RUNNING.name()).when(deploymentSlot).state();
        Thread.sleep(3000);
        doReturn(STOPPED.name()).when(deploymentSlot).state();
      } catch (InterruptedException exception) {
        exception.printStackTrace();
      }
    }
  }

  private AzureWebClientContext getAzureWebClientContext() {
    return AzureWebClientContext.builder()
        .azureConfig(AzureConfig.builder().build())
        .resourceGroupName("rg")
        .appName("app")
        .subscriptionId("subId")
        .build();
  }
}
