/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.impl;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateToken.DelegateTokenKeys;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.delegate.beans.DelegateTokenDetails.DelegateTokenDetailsBuilder;
import io.harness.delegate.beans.DelegateTokenStatus;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateTokenService;
import io.harness.utils.Misc;

import software.wings.beans.Account;
import software.wings.beans.Event;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.account.AccountCrudObserver;

import com.google.inject.Inject;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateTokenServiceImpl implements DelegateTokenService, AccountCrudObserver {
  @Inject private HPersistence persistence;
  @Inject private AuditServiceHelper auditServiceHelper;

  private static final String DEFAULT_TOKEN_NAME = "default";

  @Override
  public DelegateTokenDetails createDelegateToken(String accountId, String name) {
    DelegateToken delegateToken = DelegateToken.builder()
                                      .accountId(accountId)
                                      .createdAt(System.currentTimeMillis())
                                      .name(name)
                                      .status(DelegateTokenStatus.ACTIVE)
                                      .value(Misc.generateSecretKey())
                                      .build();

    persistence.save(delegateToken);

    auditServiceHelper.reportForAuditingUsingAccountId(
        delegateToken.getAccountId(), null, delegateToken, Event.Type.CREATE);

    return getDelegateTokenDetails(delegateToken, true);
  }

  @Override
  public DelegateTokenDetails upsertDefaultToken(String accountId, String tokenValue) {
    Query<DelegateToken> query = persistence.createQuery(DelegateToken.class)
                                     .filter(DelegateTokenKeys.accountId, accountId)
                                     .filter(DelegateTokenKeys.name, DEFAULT_TOKEN_NAME);

    UpdateOperations<DelegateToken> updateOperations =
        persistence.createUpdateOperations(DelegateToken.class)
            .setOnInsert(DelegateTokenKeys.uuid, UUIDGenerator.generateUuid())
            .setOnInsert(DelegateTokenKeys.accountId, accountId)
            .set(DelegateTokenKeys.name, DEFAULT_TOKEN_NAME)
            .set(DelegateTokenKeys.status, DelegateTokenStatus.ACTIVE)
            .set(DelegateTokenKeys.value, tokenValue);

    DelegateToken delegateToken = persistence.upsert(query, updateOperations, HPersistence.upsertReturnNewOptions);

    return getDelegateTokenDetails(delegateToken, false);
  }

  @Override
  public void revokeDelegateToken(String accountId, String tokenName) {
    Query<DelegateToken> filterQuery = persistence.createQuery(DelegateToken.class)
                                           .field(DelegateTokenKeys.accountId)
                                           .equal(accountId)
                                           .field(DelegateTokenKeys.name)
                                           .equal(tokenName);

    DelegateToken originalDelegateToken = filterQuery.get();
    UpdateOperations<DelegateToken> updateOperations =
        persistence.createUpdateOperations(DelegateToken.class)
            .set(DelegateTokenKeys.status, DelegateTokenStatus.REVOKED)
            .set(DelegateTokenKeys.validUntil,
                Date.from(OffsetDateTime.now().plusDays(DelegateToken.TTL.toDays()).toInstant()));
    DelegateToken updatedDelegateToken =
        persistence.findAndModify(filterQuery, updateOperations, new FindAndModifyOptions());
    auditServiceHelper.reportForAuditingUsingAccountId(
        accountId, originalDelegateToken, updatedDelegateToken, Event.Type.UPDATE);
  }

  @Override
  public void deleteDelegateToken(String accountId, String tokenName) {
    Query<DelegateToken> deleteQuery = persistence.createQuery(DelegateToken.class)
                                           .field(DelegateTokenKeys.accountId)
                                           .equal(accountId)
                                           .field(DelegateTokenKeys.name)
                                           .equal(tokenName);

    DelegateToken delegateToken = deleteQuery.get();

    if (!delegateToken.getName().equals(DEFAULT_TOKEN_NAME)) {
      persistence.delete(delegateToken);
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, delegateToken);
    }
  }

  @Override
  public String getTokenValue(String accountId, String tokenName) {
    DelegateToken delegateToken = persistence.createQuery(DelegateToken.class)
                                      .field(DelegateTokenKeys.accountId)
                                      .equal(accountId)
                                      .field(DelegateTokenKeys.name)
                                      .equal(tokenName)
                                      .get();

    return delegateToken != null ? delegateToken.getValue() : null;
  }

  @Override
  public List<DelegateTokenDetails> getDelegateTokens(String accountId, DelegateTokenStatus status, String tokenName) {
    List<DelegateToken> queryResults;

    Query<DelegateToken> query = persistence.createQuery(DelegateToken.class)
                                     .field(DelegateTokenKeys.accountId)
                                     .equal(accountId)
                                     .field(DelegateTokenKeys.isNg)
                                     .notEqual(true);

    if (null != status) {
      query = query.field(DelegateTokenKeys.status).equal(status);
    }

    if (!StringUtils.isEmpty(tokenName)) {
      query = query.field(DelegateTokenKeys.name).startsWith(tokenName);
    }

    queryResults = query.asList();

    List<DelegateTokenDetails> delegateTokenDetailsList = new ArrayList<>();

    // Removing token values
    queryResults.forEach(token -> delegateTokenDetailsList.add(getDelegateTokenDetails(token, false)));

    return delegateTokenDetailsList;
  }

  private DelegateTokenDetails getDelegateTokenDetails(DelegateToken delegateToken, boolean includeTokenValue) {
    DelegateTokenDetailsBuilder delegateTokenDetailsBuilder = DelegateTokenDetails.builder();

    delegateTokenDetailsBuilder.uuid(delegateToken.getUuid())
        .accountId(delegateToken.getAccountId())
        .name(delegateToken.getName())
        .createdAt(delegateToken.getCreatedAt())
        .createdBy(delegateToken.getCreatedBy())
        .status(delegateToken.getStatus());

    if (includeTokenValue) {
      delegateTokenDetailsBuilder.value(delegateToken.getValue());
    }

    return delegateTokenDetailsBuilder.build();
  }

  @Override
  public void onAccountCreated(Account account) {
    upsertDefaultToken(account.getUuid(), account.getAccountKey());
  }

  @Override
  public void onAccountUpdated(Account account) {
    // do nothing
  }
}
