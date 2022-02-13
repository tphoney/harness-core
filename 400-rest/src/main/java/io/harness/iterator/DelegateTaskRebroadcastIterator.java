package io.harness.iterator;

import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.metrics.impl.DelegateMetricsServiceImpl.DELEGATE_TASK_REBROADCAST;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.delegate.task.TaskLogContext;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.metrics.intfc.DelegateMetricsService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.workers.background.AccountLevelEntityProcessController;

import software.wings.beans.Account;
import software.wings.beans.TaskType;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateTaskRebroadcastIterator implements MongoPersistenceIterator.Handler<Account> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<Account> persistenceProvider;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private DelegateTaskService delegateTaskService;
  @Inject private HPersistence persistence;
  @Inject private AccountService accountService;
  @Inject private ConfigurationController configurationController;
  @Inject private DelegateMetricsService delegateMetricsService;
  @Inject private DelegateTaskBroadcastHelper broadcastHelper;
  @Inject private DelegateSelectionLogsService delegateSelectionLogsService;
  @Inject private Clock clock;


  private static final long DELEGATE_TASK_REBROADCAST_TIMEOUT = 10;
  private static long BROADCAST_INTERVAL = TimeUnit.MINUTES.toMillis(1);
  private static int MAX_BROADCAST_ROUND = 3;

  public void registerIterators(int threadPoolSize) {
    PersistenceIteratorFactory.PumpExecutorOptions options =
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .interval(Duration.ofSeconds(DELEGATE_TASK_REBROADCAST_TIMEOUT))
            .poolSize(threadPoolSize)
            .name("DelegateTaskRebroadcast")
            .build();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(options, FailDelegateTaskIterator.class,
        MongoPersistenceIterator.<Account, MorphiaFilterExpander<Account>>builder()
            .clazz(Account.class)
            .fieldName(Account.AccountKeys.delegateTaskRebroadcastIteration)
            .targetInterval(Duration.ofSeconds(DELEGATE_TASK_REBROADCAST_TIMEOUT))
            .acceptableNoAlertDelay(Duration.ofSeconds(45))
            .acceptableExecutionTime(Duration.ofSeconds(30))
            .entityProcessController(new AccountLevelEntityProcessController(accountService))
            .handler(this)
            .schedulingType(MongoPersistenceIterator.SchedulingType.REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Account entity) {
    rebroadcastUnassignedTasks(entity);
  }

  @VisibleForTesting
  protected void rebroadcastUnassignedTasks(Account account) {
    // Re-broadcast queued tasks not picked up by any Delegate and not in process of validation
    long now = clock.millis();

    Query<DelegateTask> unassignedTasksQuery = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                                   .filter(DelegateTask.DelegateTaskKeys.accountId, account.getUuid())
                                                   .filter(DelegateTask.DelegateTaskKeys.status, QUEUED)
                                                   .field(DelegateTask.DelegateTaskKeys.nextBroadcast)
                                                   .lessThan(now)
                                                   .field(DelegateTask.DelegateTaskKeys.expiry)
                                                   .greaterThan(now)
                                                   .field(DelegateTask.DelegateTaskKeys.broadcastRound)
                                                   .lessThan(MAX_BROADCAST_ROUND)
                                                   .field(DelegateTask.DelegateTaskKeys.delegateId)
                                                   .doesNotExist();

    try (HIterator<DelegateTask> iterator = new HIterator<>(unassignedTasksQuery.fetch())) {
      int count = 0;
      while (iterator.hasNext()) {
        DelegateTask delegateTask = iterator.next();
        Query<DelegateTask> query =
            persistence.createQuery(DelegateTask.class, excludeAuthority)
                .filter(DelegateTask.DelegateTaskKeys.uuid, delegateTask.getUuid())
                .filter(DelegateTask.DelegateTaskKeys.broadcastCount, delegateTask.getBroadcastCount());

        LinkedList<String> eligibleDelegatesList = delegateTask.getEligibleToExecuteDelegateIds();

        if (isEmpty(eligibleDelegatesList)) {
          log.info("No eligible delegates for task {}", delegateTask.getUuid());
          continue;
        }

        // add connected eligible delegates to broadcast list. Also rotate the eligibleDelegatesList list
        List<String> broadcastToDelegates = Lists.newArrayList();
        int broadcastLimit = Math.min(eligibleDelegatesList.size(), 10);

        Iterator<String> delegateIdIterator = eligibleDelegatesList.iterator();

        while (delegateIdIterator.hasNext() && broadcastLimit > broadcastToDelegates.size()) {
          String delegateId = eligibleDelegatesList.removeFirst();
          broadcastToDelegates.add(delegateId);
          eligibleDelegatesList.addLast(delegateId);
        }
        long nextInterval = TimeUnit.SECONDS.toMillis(5);
        int broadcastRoundCount = delegateTask.getBroadcastRound();
        Set<String> alreadyTriedDelegates =
            Optional.ofNullable(delegateTask.getAlreadyTriedDelegates()).orElse(Sets.newHashSet());

        // if all delegates got one round of rebroadcast, then increase broadcast interval & broadcastRound
        if (alreadyTriedDelegates.containsAll(delegateTask.getEligibleToExecuteDelegateIds())) {
          alreadyTriedDelegates.clear();
          broadcastRoundCount++;
          nextInterval = (long) broadcastRoundCount * BROADCAST_INTERVAL;
        }
        alreadyTriedDelegates.addAll(broadcastToDelegates);

        UpdateOperations<DelegateTask> updateOperations =
            persistence.createUpdateOperations(DelegateTask.class)
                .set(DelegateTask.DelegateTaskKeys.lastBroadcastAt, now)
                .set(DelegateTask.DelegateTaskKeys.broadcastCount, delegateTask.getBroadcastCount() + 1)
                .set(DelegateTask.DelegateTaskKeys.eligibleToExecuteDelegateIds, eligibleDelegatesList)
                .set(DelegateTask.DelegateTaskKeys.nextBroadcast, now + nextInterval)
                .set(DelegateTask.DelegateTaskKeys.alreadyTriedDelegates, alreadyTriedDelegates)
                .set(DelegateTask.DelegateTaskKeys.broadcastRound, broadcastRoundCount);
        delegateTask = persistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);
        // update failed, means this was broadcast by some other manager
        if (delegateTask == null) {
          log.info("Cannot find delegate task, update failed on broadcast");
          continue;
        }
        delegateTask.setBroadcastToDelegateIds(broadcastToDelegates);

        BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(delegateTask);
        if (isNotEmpty(broadcastToDelegates)) {
          delegateSelectionLogsService.logBroadcastToDelegate(
              batch, Sets.newHashSet(broadcastToDelegates), delegateTask.getAccountId());
        }

        delegateSelectionLogsService.save(batch);

        try (AutoLogContext ignore1 = new TaskLogContext(delegateTask.getUuid(), delegateTask.getData().getTaskType(),
                 TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR);
             AutoLogContext ignore2 = new AccountLogContext(delegateTask.getAccountId(), OVERRIDE_ERROR)) {
          log.info("Rebroadcast queued task id {} on broadcast attempt: {} on round {} to {} ", delegateTask.getUuid(),
              delegateTask.getBroadcastCount(), delegateTask.getBroadcastRound(),
              delegateTask.getBroadcastToDelegateIds());
          delegateMetricsService.recordDelegateTaskMetrics(delegateTask, DELEGATE_TASK_REBROADCAST);
          broadcastHelper.rebroadcastDelegateTask(delegateTask);
          count++;
        }
      }
      log.info("{} tasks were rebroadcast", count);
    }
  }
}
