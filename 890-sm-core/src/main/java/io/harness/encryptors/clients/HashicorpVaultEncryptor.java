/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.helpers.NGVaultTaskHelper.getVaultAwmIamAuthLoginResult;
import static io.harness.helpers.ext.vault.VaultRestClientFactory.getFullPath;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import io.harness.annotations.dev.OwnedBy;
import io.harness.concurrent.HTimeLimiter;
import io.harness.encryptors.VaultEncryptor;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.exception.runtime.HashiCorpVaultRuntimeException;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.helpers.ext.vault.VaultRestClientFactory;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.VaultConfig;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class HashicorpVaultEncryptor implements VaultEncryptor {
  private final TimeLimiter timeLimiter;
  private final int NUM_OF_RETRIES = 3;

  @Inject
  public HashicorpVaultEncryptor(TimeLimiter timeLimiter) {
    this.timeLimiter = timeLimiter;
  }

  @Override
  public EncryptedRecord createSecret(
      String accountId, String name, String plaintext, EncryptionConfig encryptionConfig) {
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofSeconds(15),
            () -> upsertSecretInternal(name, plaintext, accountId, null, vaultConfig, false));
      } catch (Exception e) {
        failedAttempts++;
        log.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          if (e instanceof HashiCorpVaultRuntimeException) {
            throw new HashiCorpVaultRuntimeException(e.getMessage());
          } else {
            String message = "After " + NUM_OF_RETRIES + " tries, encryption for vault secret " + name + " failed.";
            throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
          }
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public EncryptedRecord updateSecret(String accountId, String name, String plaintext, EncryptedRecord existingRecord,
      EncryptionConfig encryptionConfig) {
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofSeconds(5),
            () -> upsertSecretInternal(name, plaintext, accountId, existingRecord, vaultConfig, false));
      } catch (Exception e) {
        failedAttempts++;
        log.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          if (e instanceof HashiCorpVaultRuntimeException) {
            throw new HashiCorpVaultRuntimeException(e.getMessage());
          } else {
            String message = "After " + NUM_OF_RETRIES + " tries, encryption for vault secret " + name + " failed.";
            throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
          }
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public EncryptedRecord renameSecret(
      String accountId, String name, EncryptedRecord existingRecord, EncryptionConfig encryptionConfig) {
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofSeconds(15),
            () -> renameSecretInternal(name, accountId, existingRecord, vaultConfig));
      } catch (Exception e) {
        failedAttempts++;
        log.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String message = "After " + NUM_OF_RETRIES + " tries, encryption for vault secret " + name + " failed.";
          throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public boolean deleteSecret(String accountId, EncryptedRecord existingRecord, EncryptionConfig encryptionConfig) {
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    try {
      String fullPath = getFullPath(vaultConfig.getBasePath(), existingRecord.getEncryptionKey());
      String vaultToken = getToken(vaultConfig);
      return VaultRestClientFactory.create(vaultConfig)
          .deleteSecret(
              String.valueOf(vaultToken), vaultConfig.getNamespace(), vaultConfig.getSecretEngineName(), fullPath);
    } catch (IOException e) {
      String message = "Deletion of Vault secret at " + existingRecord.getEncryptionKey() + " failed";
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }
  }

  private EncryptedRecord upsertSecretInternal(String keyUrl, String value, String accountId,
      EncryptedRecord existingRecord, VaultConfig vaultConfig, boolean deleteRequired) throws IOException {
    log.info("Saving secret {} into Vault {}", keyUrl, vaultConfig.getBasePath());

    // With existing encrypted value. Need to delete it first and rewrite with new value.
    String fullPath = getFullPath(vaultConfig.getBasePath(), keyUrl);

    String vaultToken = getToken(vaultConfig);
    boolean isSuccessful = VaultRestClientFactory.create(vaultConfig)
                               .writeSecret(String.valueOf(vaultToken), vaultConfig.getNamespace(),
                                   vaultConfig.getSecretEngineName(), fullPath, value);

    if (isSuccessful) {
      log.info("Done saving vault secret at {} in {}", keyUrl, vaultConfig.getBasePath());
      if (existingRecord != null) {
        String oldFullPath = getFullPath(vaultConfig.getBasePath(), existingRecord.getEncryptionKey());
        if (!oldFullPath.equals(fullPath)) {
          if (deleteRequired) {
            deleteSecret(accountId, existingRecord, vaultConfig);
          }
        }
      }
      return EncryptedRecordData.builder().encryptionKey(keyUrl).encryptedValue(keyUrl.toCharArray()).build();
    } else {
      String errorMsg = "Encryption request for " + keyUrl + " was not successful.";
      log.error(errorMsg);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, errorMsg, USER);
    }
  }

  private EncryptedRecord renameSecretInternal(
      String keyUrl, String accountId, EncryptedRecord existingRecord, VaultConfig vaultConfig) throws IOException {
    char[] value = fetchSecretInternal(existingRecord, vaultConfig);
    return upsertSecretInternal(keyUrl, new String(value), accountId, existingRecord, vaultConfig, true);
  }

  @Override
  public boolean validateReference(String accountId, String path, EncryptionConfig encryptionConfig) {
    return isNotEmpty(fetchSecretValue(accountId, EncryptedRecordData.builder().path(path).build(), encryptionConfig));
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    if (isEmpty(encryptedRecord.getEncryptionKey()) && isEmpty(encryptedRecord.getPath())) {
      return null;
    }
    VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
    int failedAttempts = 0;
    while (true) {
      try {
        return HTimeLimiter.callInterruptible21(
            timeLimiter, Duration.ofSeconds(15), () -> fetchSecretInternal(encryptedRecord, vaultConfig));
      } catch (Exception e) {
        failedAttempts++;
        log.warn("decryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String message = "Decryption failed after " + NUM_OF_RETRIES + " retries for secret "
              + encryptedRecord.getEncryptionKey() + " or path " + encryptedRecord.getPath();
          throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
        }
        sleep(ofMillis(1000));
      }
    }
  }

  private char[] fetchSecretInternal(EncryptedRecord data, VaultConfig vaultConfig) throws IOException {
    String fullPath =
        isEmpty(data.getPath()) ? getFullPath(vaultConfig.getBasePath(), data.getEncryptionKey()) : data.getPath();
    long startTime = System.currentTimeMillis();
    log.info("Reading secret {} from vault {}", fullPath, vaultConfig.getVaultUrl());
    String vaultToken = getToken(vaultConfig);
    String value = VaultRestClientFactory.create(vaultConfig)
                       .readSecret(String.valueOf(vaultToken), vaultConfig.getNamespace(),
                           vaultConfig.getSecretEngineName(), fullPath);

    if (isNotEmpty(value)) {
      log.info("Done reading secret {} from vault {} in {} ms.", fullPath, vaultConfig.getVaultUrl(),
          System.currentTimeMillis() - startTime);
      return value.toCharArray();
    } else {
      String errorMsg = "Secret key path '" + fullPath + "' is invalid.";
      log.error(errorMsg);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, errorMsg, USER);
    }
  }

  private String getToken(VaultConfig vaultConfig) {
    if (vaultConfig.isUseVaultAgent()) {
      try {
        byte[] content = Files.readAllBytes(Paths.get(URI.create("file://" + vaultConfig.getSinkPath())));
        String authToken = new String(content);
        vaultConfig.setAuthToken(authToken);
      } catch (IOException e) {
        throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR,
            "Using Vault Agent Cannot read Token From Sink Path:" + vaultConfig.getSinkPath(), e, USER);
      }
    } else if (vaultConfig.isUseAwsIam()) {
      VaultAppRoleLoginResult vaultAwmIamAuthLoginResult = getVaultAwmIamAuthLoginResult(vaultConfig);
      vaultConfig.setAuthToken(vaultAwmIamAuthLoginResult.getClientToken());
    }
    return vaultConfig.getAuthToken();
  }

  @Override
  public boolean validateSecretManagerConfiguration(String accountId, EncryptionConfig encryptionConfig) {
    try {
      createSecret(accountId, VaultConfig.VAULT_VAILDATION_URL, Boolean.TRUE.toString(), encryptionConfig);
    } catch (Exception exception) {
      log.error("Validation for Secret Manager/KMS failed: " + encryptionConfig.getName());
      throw exception;
    }
    return true;
  }
}
