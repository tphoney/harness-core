/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.view;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.service.CEViewService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;

import software.wings.beans.Account;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;

import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class ViewCostUpdateService {
  @Autowired private BatchMainConfig config;
  @Autowired private CEViewService ceViewService;
  @Autowired private BigQueryService bigQueryService;
  @Autowired private AccountShardService accountShardService;
  @Autowired private CloudBillingHelper cloudBillingHelper;

  public void updateTotalCost() {
    List<Account> ceEnabledAccounts = accountShardService.getCeEnabledAccounts();
    List<String> accountIds = ceEnabledAccounts.stream().map(Account::getUuid).collect(Collectors.toList());
    accountIds.forEach(accountId -> {
      AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR);
      List<CEView> views = ceViewService.getViewByState(accountId, ViewState.COMPLETED);
      views.forEach(view -> {
        log.info("Updating view {}", view.getUuid());
        try {
          ceViewService.updateTotalCost(view, bigQueryService.get(),
              cloudBillingHelper.getCloudProviderTableName(
                  config.getBillingDataPipelineConfig().getGcpProjectId(), accountId, unified));
        } catch (Exception ex) {
          log.error("Exception while updating cost", ex);
        }
      });
      ignore.close();
    });
    log.info("Updated views for all accounts");
  }
}
