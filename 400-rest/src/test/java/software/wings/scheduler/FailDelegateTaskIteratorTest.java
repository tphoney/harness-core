package software.wings.scheduler;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.JENNY;

import static software.wings.utils.WingsTestConstants.APP_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.iterator.FailDelegateTaskIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.TaskType;
import software.wings.service.impl.DelegateTaskServiceClassicImpl;
import software.wings.service.intfc.AssignDelegateService;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.LinkedList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistenceIteratorFactory.class)
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class FailDelegateTaskIteratorTest extends WingsBaseTest {
  @Mock PersistenceIteratorFactory persistenceIteratorFactory;
  @InjectMocks @Inject private FailDelegateTaskIterator failDelegateTaskIterator;
  @Mock private AssignDelegateService assignDelegateService;
  @InjectMocks @Inject private DelegateTaskServiceClassicImpl delegateTaskServiceClassic;
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    ArgumentCaptor<MongoPersistenceIteratorBuilder> captor =
        ArgumentCaptor.forClass(MongoPersistenceIteratorBuilder.class);
    failDelegateTaskIterator.registerIterators(1);
    verify(persistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(FailDelegateTaskIterator.class), captor.capture());
    MongoPersistenceIteratorBuilder mongoPersistenceIteratorBuilder = captor.getValue();
    assertThat(mongoPersistenceIteratorBuilder).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testHandle_failLongQueued() {}

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testHandle_failLongStarted() {}

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testHandle_withAllDelegatesValidationCompleted() {
    Account account = new Account();
    account.setUuid(generateUuid());
    persistence.save(account);
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(account.getUuid())
            .waitId(generateUuid())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .eligibleToExecuteDelegateIds(new LinkedList<>(Arrays.asList("del1", "del2", "del3")))
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.HTTP.name())
                      .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .validationCompleteDelegateIds(ImmutableSet.of("del1", "del2", "del3"))
            .build();
    persistence.save(delegateTask);
    failDelegateTaskIterator.failValidationCompletedQueuedTask(account);
    assertThat(persistence.createQuery(DelegateTask.class).get()).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testHandle_withDelegatesPendingValidation() {
    Account account = new Account();
    account.setUuid(generateUuid());
    persistence.save(account);
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(account.getUuid())
            .waitId(generateUuid())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .eligibleToExecuteDelegateIds(new LinkedList<>(Arrays.asList("del1", "del2", "del3")))
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.HTTP.name())
                      .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .validationCompleteDelegateIds(ImmutableSet.of("del1", "del2"))
            .build();
    persistence.save(delegateTask);
    failDelegateTaskIterator.failValidationCompletedQueuedTask(account);
    assertThat(persistence.createQuery(DelegateTask.class).get()).isNotNull();
  }

  @Test
  @Owner(developers = JENNY)
  @Category(UnitTests.class)
  public void testHandle_withAllDelegatesValidationCompleted_ButFoundConnectedWhitelistedOnes() {
    Account account = new Account();
    account.setUuid(generateUuid());
    persistence.save(account);
    DelegateTask delegateTask =
        DelegateTask.builder()
            .accountId(account.getUuid())
            .waitId(generateUuid())
            .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
            .eligibleToExecuteDelegateIds(new LinkedList<>(Arrays.asList("del1", "del2", "del3")))
            .data(TaskData.builder()
                      .async(true)
                      .taskType(TaskType.HTTP.name())
                      .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .validationCompleteDelegateIds(ImmutableSet.of("del1", "del2"))
            .build();
    persistence.save(delegateTask);
    when(assignDelegateService.connectedWhitelistedDelegates(delegateTask)).thenReturn(Arrays.asList("del1"));
    failDelegateTaskIterator.failValidationCompletedQueuedTask(account);
    assertThat(persistence.createQuery(DelegateTask.class).get()).isNotNull();
  }
}
