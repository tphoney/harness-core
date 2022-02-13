package io.harness.workers.background.iterator;

import static io.harness.beans.FeatureName.DELEGATE_TASK_REBROADCAST_KILL_SWITCH;

import io.harness.ff.FeatureFlagService;
import io.harness.workers.background.AccountLevelEntityProcessController;

import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;

public class AccountLevelEntityProcessTaskRebroadcastController extends AccountLevelEntityProcessController {
  @Inject private FeatureFlagService featureFlagService;

  public AccountLevelEntityProcessTaskRebroadcastController(AccountService accountService) {
    super(accountService);
  }

  @Override
  public boolean shouldProcessEntity(Account account) {
    if (featureFlagService.isEnabled(DELEGATE_TASK_REBROADCAST_KILL_SWITCH, account.getUuid())) {
      return false;
    }
    return super.shouldProcessEntity(account);
  }
}
