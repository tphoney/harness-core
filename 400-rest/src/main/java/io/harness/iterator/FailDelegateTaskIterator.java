package io.harness.iterator;

import static io.harness.beans.DelegateTask.Status.*;
import static io.harness.beans.DelegateTask.Status.ABORTED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.TaskFailureReason.EXPIRED;
import static io.harness.metrics.impl.DelegateMetricsServiceImpl.DELEGATE_TASK_EXPIRED;
import static io.harness.persistence.HQuery.excludeAuthority;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.metrics.intfc.DelegateMetricsService;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.workers.background.AccountLevelEntityProcessController;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;

import software.wings.beans.Account;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AssignDelegateService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class FailDelegateTaskIterator implements MongoPersistenceIterator.Handler<Account> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<Account> persistenceProvider;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private DelegateTaskService delegateTaskService;
  @Inject private HPersistence persistence;
  @Inject private AccountService accountService;
  @Inject private ConfigurationController configurationController;
  @Inject private DelegateMetricsService delegateMetricsService;

  private static final long VALIDATION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

  private static final long DELEGATE_TASK_FAIL_TIMEOUT = 10;
  private static final FindOptions expiryLimit = new FindOptions().limit(100);
  private static final SecureRandom random = new SecureRandom();

  public void registerIterators(int threadPoolSize) {
    PersistenceIteratorFactory.PumpExecutorOptions options =
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .interval(Duration.ofSeconds(DELEGATE_TASK_FAIL_TIMEOUT))
            .poolSize(threadPoolSize)
            .name("DelegateTaskFail")
            .build();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(options, FailDelegateTaskIterator.class,
        MongoPersistenceIterator.<Account, MorphiaFilterExpander<Account>>builder()
            .clazz(Account.class)
            .fieldName(Account.AccountKeys.delegateTaskFailIteration)
            .targetInterval(Duration.ofSeconds(DELEGATE_TASK_FAIL_TIMEOUT))
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
    if (configurationController.isPrimary()) {
      markTimedOutTasksAsFailed();
      markLongQueuedTasksAsFailed();
      failValidationCompletedQueuedTask(entity);
    }
  }

  private void markTimedOutTasksAsFailed() {
    List<Key<DelegateTask>> longRunningTimedOutTaskKeys = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                                              .filter(DelegateTask.DelegateTaskKeys.status, STARTED)
                                                              .field(DelegateTask.DelegateTaskKeys.expiry)
                                                              .lessThan(currentTimeMillis())
                                                              .asKeyList(expiryLimit);

    if (!longRunningTimedOutTaskKeys.isEmpty()) {
      List<String> keyList = longRunningTimedOutTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      log.info("Marking following timed out tasks as failed [{}]", keyList);
      endTasks(keyList);
    }
  }

  private AtomicInteger clustering = new AtomicInteger(1);

  private void markLongQueuedTasksAsFailed() {
    // Find tasks which have been queued for too long
    Query<DelegateTask> query = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                    .field(DelegateTask.DelegateTaskKeys.status)
                                    .in(asList(QUEUED, PARKED, ABORTED))
                                    .field(DelegateTask.DelegateTaskKeys.expiry)
                                    .lessThan(currentTimeMillis());

    // We usually pick from the top, but if we have full bucket we maybe slowing down
    // lets randomize a bit to increase the distribution
    int clusteringValue = clustering.get();
    if (clusteringValue > 1) {
      query.field(DelegateTask.DelegateTaskKeys.createdAt).mod(clusteringValue, random.nextInt(clusteringValue));
    }

    List<Key<DelegateTask>> longQueuedTaskKeys = query.asKeyList(expiryLimit);
    clustering.set(longQueuedTaskKeys.size() == expiryLimit.getLimit() ? Math.min(16, clusteringValue * 2)
                                                                       : Math.max(1, clusteringValue / 2));

    if (!longQueuedTaskKeys.isEmpty()) {
      List<String> keyList = longQueuedTaskKeys.stream().map(key -> key.getId().toString()).collect(toList());
      log.info("Marking following long queued tasks as failed [{}]", keyList);
      endTasks(keyList);
    }
  }

  @VisibleForTesting
  public void endTasks(List<String> taskIds) {
    Map<String, DelegateTask> delegateTasks = new HashMap<>();
    Map<String, String> taskWaitIds = new HashMap<>();
    List<DelegateTask> tasksToExpire = new ArrayList<>();
    List<String> taskIdsToExpire = new ArrayList<>();
    try {
      List<DelegateTask> tasks = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                     .field(DelegateTask.DelegateTaskKeys.uuid)
                                     .in(taskIds)
                                     .asList();

      for (DelegateTask task : tasks) {
        if (shouldExpireTask(task)) {
          tasksToExpire.add(task);
          taskIdsToExpire.add(task.getUuid());
          delegateMetricsService.recordDelegateTaskMetrics(task, DELEGATE_TASK_EXPIRED);
        }
      }

      delegateTasks.putAll(tasksToExpire.stream().collect(toMap(DelegateTask::getUuid, identity())));
      taskWaitIds.putAll(tasksToExpire.stream()
                             .filter(task -> isNotEmpty(task.getWaitId()))
                             .collect(toMap(DelegateTask::getUuid, DelegateTask::getWaitId)));
    } catch (Exception e1) {
      log.error("Failed to deserialize {} tasks. Trying individually...", taskIds.size(), e1);
      for (String taskId : taskIds) {
        try {
          DelegateTask task = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                  .filter(DelegateTask.DelegateTaskKeys.uuid, taskId)
                                  .get();
          if (shouldExpireTask(task)) {
            taskIdsToExpire.add(taskId);
            delegateTasks.put(taskId, task);
            delegateMetricsService.recordDelegateTaskMetrics(task, DELEGATE_TASK_EXPIRED);
            if (isNotEmpty(task.getWaitId())) {
              taskWaitIds.put(taskId, task.getWaitId());
            }
          }
        } catch (Exception e2) {
          log.error("Could not deserialize task {}. Trying again with only waitId field.", taskId, e2);
          taskIdsToExpire.add(taskId);
          try {
            String waitId = persistence.createQuery(DelegateTask.class, excludeAuthority)
                                .filter(DelegateTask.DelegateTaskKeys.uuid, taskId)
                                .project(DelegateTask.DelegateTaskKeys.waitId, true)
                                .get()
                                .getWaitId();
            if (isNotEmpty(waitId)) {
              taskWaitIds.put(taskId, waitId);
            }
          } catch (Exception e3) {
            log.error(
                "Could not deserialize task {} with waitId only, giving up. Task will be deleted but notify not called.",
                taskId, e3);
          }
        }
      }
    }

    boolean deleted = persistence.deleteOnServer(persistence.createQuery(DelegateTask.class, excludeAuthority)
                                                     .field(DelegateTask.DelegateTaskKeys.uuid)
                                                     .in(taskIdsToExpire));

    if (deleted) {
      taskIdsToExpire.forEach(taskId -> {
        if (taskWaitIds.containsKey(taskId)) {
          String errorMessage = delegateTasks.containsKey(taskId)
              ? assignDelegateService.getActiveDelegateAssignmentErrorMessage(EXPIRED, delegateTasks.get(taskId))
              : "Unable to determine proper error as delegate task could not be deserialized.";
          log.info("Marking task as failed - {}: {}", taskId, errorMessage);

          if (delegateTasks.get(taskId) != null) {
            delegateTaskService.handleResponse(delegateTasks.get(taskId), null,
                DelegateTaskResponse.builder()
                    .accountId(delegateTasks.get(taskId).getAccountId())
                    .responseCode(DelegateTaskResponse.ResponseCode.FAILED)
                    .response(ErrorNotifyResponseData.builder().errorMessage(errorMessage).build())
                    .build());
          }
        }
      });
    }
  }

  private boolean shouldExpireTask(DelegateTask task) {
    return !task.isForceExecute();
  }


  private void failValidationCompletedQueuedTask(Account account) {
    Query<DelegateTask> validationStartedTaskQuery = persistence.createQuery(DelegateTask.class, excludeAuthority)
            .filter(DelegateTask.DelegateTaskKeys.accountId, account.getUuid())
            .filter(DelegateTask.DelegateTaskKeys.status, QUEUED)
            .field(DelegateTask.DelegateTaskKeys.validationStartedAt)
            .lessThan(clock.millis() - VALIDATION_TIMEOUT);
    try (HIterator<DelegateTask> iterator = new HIterator<>(validationStartedTaskQuery.fetch())) {
      while (iterator.hasNext()) {
        DelegateTask delegateTask = iterator.next();
        
      }


    }
  }
}
