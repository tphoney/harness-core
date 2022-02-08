package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateToken;
import io.harness.delegate.beans.DelegateTokenCacheKey;

import software.wings.beans.DelegateStatus;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DEL)
public class DelegateTokenCacheHelper {
  @Inject private Cache<DelegateTokenCacheKey, DelegateToken> delegateTokenCache;
  @Inject private DelegateService delegateService;

  public DelegateToken getDelegateToken(DelegateTokenCacheKey delegateTokenCacheKey) {
    return delegateTokenCache != null ? delegateTokenCache.get(delegateTokenCacheKey) : null;
  }

  // TODO: find a better way to invalidate a particular cache when a delegate token is revoked.
  public void invalidateCacheUsingAccountId(String accountId) {
    DelegateStatus delegateStatus = delegateService.getDelegateStatus(accountId);
    delegateStatus.getDelegates()
        .stream()
        .filter(Objects::nonNull)
        .forEach(delegateInner
            -> invalidateCacheUsingKey(new DelegateTokenCacheKey(accountId, delegateInner.getHostName())));
  }

  // TODO: Question, is it able to insert for the very first time
  public void putIfTokenIsAbsent(DelegateTokenCacheKey delegateTokenCacheKey, DelegateToken delegateToken) {
    if (delegateTokenCache != null) {
      delegateTokenCache.putIfAbsent(delegateTokenCacheKey, delegateToken);
    }
  }

  public void invalidateCacheUsingKey(DelegateTokenCacheKey delegateTokenCacheKey) {
    log.info("Invalidating cache for accountId {} and delegateHostName {}", delegateTokenCacheKey.getAccountId(),
        delegateTokenCacheKey.getDelegateHostName());
    if (delegateTokenCache != null) {
      delegateTokenCache.remove(delegateTokenCacheKey);
    }
  }
}
