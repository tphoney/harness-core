package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import static software.wings.app.ManagerCacheRegistrar.DELEGATE_TOKEN_CACHE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateTokenCacheKey;

import software.wings.beans.DelegateStatus;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Objects;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DEL)
public class DelegateTokenCacheHelper {
  @Inject @Named(DELEGATE_TOKEN_CACHE) private Cache<DelegateTokenCacheKey, DelegateToken> delegateTokenCache;
  @Inject private DelegateService delegateService;
  @Inject private AccountService accountService;

  public DelegateToken getDelegateToken(DelegateTokenCacheKey delegateTokenCacheKey) {
    if (delegateTokenCache == null) {
      log.warn("Delegate token cache not yet initialized.");
      return null;
    }
    return delegateTokenCache.get(delegateTokenCacheKey);
  }

  // TODO: find a better way to invalidate a particular cache when a delegate token is revoked.
  public void invalidateAllCacheUsingAccountId(String accountId) {
    log.warn("All delegate token cache for account {} will be invalidated.", accountId);
    try {
      DelegateStatus delegateStatus = delegateService.getDelegateStatus(accountId);
      delegateStatus.getDelegates()
          .stream()
          .filter(Objects::nonNull)
          .forEach(delegateInner
              -> invalidateCacheUsingKey(DelegateTokenCacheKey.builder()
                                             .accountId(accountId)
                                             .delegateHostName(delegateInner.getHostName())
                                             .build()));
    } catch (Exception e) {
      log.error("Error occurred during invalidating all delegate tokens cache for account {}", accountId, e);
    }
  }

  public void putToken(DelegateTokenCacheKey delegateTokenCacheKey, DelegateToken delegateToken) {
    if (delegateTokenCache != null) {
      delegateTokenCache.put(delegateTokenCacheKey, delegateToken);
    }
  }

  public void invalidateCacheUsingKey(DelegateTokenCacheKey delegateTokenCacheKey) {
    if (delegateTokenCache != null) {
      log.info("Invalidating cache for accountId {} and delegateHostName {}", delegateTokenCacheKey.getAccountId(),
          delegateTokenCacheKey.getDelegateHostName());
      delegateTokenCache.remove(delegateTokenCacheKey);
    }
  }
}
