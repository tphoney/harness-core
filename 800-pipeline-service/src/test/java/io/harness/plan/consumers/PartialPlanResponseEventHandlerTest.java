package io.harness.plan.consumers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.async.plan.PartialPlanCreatorResponseData;
import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class PartialPlanResponseEventHandlerTest extends CategoryTest {
  @InjectMocks PartialPlanResponseEventHandler eventHandler;
  @Mock WaitNotifyEngine waitNotifyEngine;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    String notifyId = "s";
    PartialPlanResponse partialPlanResponse = PartialPlanResponse.newBuilder().setNotifyId(notifyId).build();
    eventHandler.handleEvent(partialPlanResponse, null, 0);
    verify(waitNotifyEngine, times(1))
        .doneWith(notifyId, PartialPlanCreatorResponseData.builder().partialPlanResponse(partialPlanResponse).build());
  }
}