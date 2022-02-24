/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessModule._890_SM_CORE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.CYBERARK_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.SecretChangeLog;
import io.harness.exception.SecretManagementDelegateException;
import io.harness.helpers.ext.cyberark.CyberArkReadResponse;
import io.harness.helpers.ext.cyberark.CyberArkRestClient;
import io.harness.helpers.ext.cyberark.CyberArkRestClientFactory;
import io.harness.helpers.ext.vault.SSHVaultAuthResponse;
import io.harness.helpers.ext.vault.SSHVaultAuthResult;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.SignedSSHVaultRequest;
import io.harness.helpers.ext.vault.SignedSSHVaultResponse;
import io.harness.helpers.ext.vault.SysMount;
import io.harness.helpers.ext.vault.SysMountsResponse;
import io.harness.helpers.ext.vault.VaultAppRoleLoginRequest;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResponse;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.helpers.ext.vault.VaultRestClientFactory;
import io.harness.helpers.ext.vault.VaultSecretMetadata;
import io.harness.helpers.ext.vault.VaultSecretMetadata.VersionMetadata;
import io.harness.helpers.ext.vault.VaultSysAuthRestClient;
import io.harness.security.encryption.EncryptedRecord;

import software.wings.beans.BaseVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.VaultConfig;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;

@OwnedBy(PL)
@Singleton
@Slf4j
@TargetModule(_890_SM_CORE)
public class SecretManagementDelegateServiceImpl implements SecretManagementDelegateService {
  @Override
  public void signPublicKey(HostConnectionAttributes hostConnectionAttributes, SSHVaultConfig sshVaultConfig) {
    try {
      if (sshVaultConfig == null) {
        sshVaultConfig = hostConnectionAttributes.getSshVaultConfig();
      }
      if (sshVaultConfig == null) {
        throw new SecretManagementDelegateException(
            VAULT_OPERATION_ERROR, "SSH Vault config while fetching signed public key is null", USER);
      }
      String vaultToken = getToken(sshVaultConfig);
      if (isEmpty(vaultToken)) {
        VaultAppRoleLoginResult loginResult = appRoleLogin(sshVaultConfig);
        if (loginResult != null) {
          vaultToken = loginResult.getClientToken();
          sshVaultConfig.setAuthToken(vaultToken);
        }
      }

      VaultSysAuthRestClient restClient =
          VaultRestClientFactory
              .getVaultRetrofit(sshVaultConfig.getVaultUrl(), sshVaultConfig.isCertValidationRequired())
              .create(VaultSysAuthRestClient.class);
      log.info("[VaultSSH]: HostConnectionAttributes are : {} Vault SSH Config is : {}", hostConnectionAttributes,
          sshVaultConfig);
      SignedSSHVaultRequest signedSSHVaultRequest = SignedSSHVaultRequest.builder()
                                                        .publicKey(hostConnectionAttributes.getPublicKey())
                                                        .validPrincipals(hostConnectionAttributes.getUserName())
                                                        .build();
      log.info("[VaultSSH]: Signing request: {}", signedSSHVaultRequest);
      Response<SignedSSHVaultResponse> response =
          restClient
              .fetchSignedPublicKey(sshVaultConfig.getSecretEngineName(), hostConnectionAttributes.getRole(),
                  vaultToken, signedSSHVaultRequest)
              .execute();
      log.info("[VaultSSH]: Signing response: {}", response);
      if (response.isSuccessful() && response.body().getSignedSSHVaultResult() != null) {
        log.info(
            "[VaultSSH]: Signed public key is : {}", response.body().getSignedSSHVaultResult().getSignedPublicKey());
        hostConnectionAttributes.setSignedPublicKey(
            response.body().getSignedSSHVaultResult().getSignedPublicKey().trim());
      } else {
        log.info("[VaultSSH]: Error signing public key for request:{}", signedSSHVaultRequest);
        logAndThrowVaultError(sshVaultConfig, response, "sign public key with SSH secret engine");
      }
    } catch (IOException ioe) {
      String message = String.format(
          "Failed to sign public key with SSH secret engine %s , due to unexpected network error. Please try again.",
          sshVaultConfig.getSecretEngineName());
      log.error(message, ioe);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, USER);
    }
  }

  @Override
  public SSHVaultAuthResult validateSSHVault(SSHVaultConfig vaultConfig) {
    try {
      VaultSysAuthRestClient restClient =
          VaultRestClientFactory.getVaultRetrofit(vaultConfig.getVaultUrl(), vaultConfig.isCertValidationRequired())
              .create(VaultSysAuthRestClient.class);
      Response<SSHVaultAuthResponse> response =
          restClient.fetchAuthPublicKey(vaultConfig.getSecretEngineName(), vaultConfig.getAuthToken()).execute();
      SSHVaultAuthResult result = null;
      if (response.isSuccessful()) {
        result = response.body().getSshVaultAuthResult();
      } else {
        logAndThrowVaultError(vaultConfig, response, "perform Config CA check for SSH secret engine");
      }
      return result;
    } catch (IOException e) {
      String message = "Failed to perform Config CA check for SSH secret engine " + vaultConfig.getName() + " at "
          + vaultConfig.getVaultUrl();
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }
  }

  @Override
  public List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedRecord encryptedData, VaultConfig vaultConfig) {
    List<SecretChangeLog> secretChangeLogs = new ArrayList<>();

    EmbeddedUser vaultUser = EmbeddedUser.builder().name("VaultUser").build();
    String encryptedDataId = encryptedData.getUuid();

    try {
      VaultSecretMetadata secretMetadata = VaultRestClientFactory.create(vaultConfig)
                                               .readSecretMetadata(getToken(vaultConfig), vaultConfig.getNamespace(),
                                                   vaultConfig.getSecretEngineName(), encryptedData.getPath());
      if (secretMetadata != null && isNotEmpty(secretMetadata.getVersions())) {
        for (Entry<Integer, VersionMetadata> entry : secretMetadata.getVersions().entrySet()) {
          int version = entry.getKey();
          VersionMetadata versionMetadata = entry.getValue();
          final String changeDescription;
          final String changeTime;
          if (versionMetadata.isDestroyed()) {
            changeDescription = "Deleted at version " + version + " in Vault";
            changeTime = versionMetadata.getDeletionTime();
          } else {
            changeDescription = version == 1 ? "Created in Vault" : "Updated to version " + version + " in Vault";
            changeTime = versionMetadata.getCreatedTime();
          }
          SecretChangeLog changeLog = SecretChangeLog.builder()
                                          .accountId(vaultConfig.getAccountId())
                                          .encryptedDataId(encryptedDataId)
                                          .description(changeDescription)
                                          .external(true)
                                          .user(vaultUser)
                                          .build();
          long changeTimeInMillis = Instant.parse(changeTime).toEpochMilli();
          changeLog.setCreatedBy(vaultUser);
          changeLog.setLastUpdatedBy(vaultUser);
          changeLog.setCreatedAt(changeTimeInMillis);
          changeLog.setLastUpdatedAt(changeTimeInMillis);

          // need to set the change time to the corresponding field in SecretChangeLog.
          // changeLog.setCreatedAt();
          secretChangeLogs.add(changeLog);
        }
      }
    } catch (Exception e) {
      String message = "Retrieval of vault secret version history failed";
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }

    return secretChangeLogs;
  }

  @Override
  public boolean renewVaultToken(BaseVaultConfig baseVaultConfig) {
    int failedAttempts = 0;
    while (true) {
      try {
        log.info("renewing token for vault {}", baseVaultConfig);
        VaultSysAuthRestClient restClient =
            VaultRestClientFactory
                .getVaultRetrofit(baseVaultConfig.getVaultUrl(), baseVaultConfig.isCertValidationRequired())
                .create(VaultSysAuthRestClient.class);
        Response<Object> response =
            restClient.renewToken(baseVaultConfig.getAuthToken(), baseVaultConfig.getNamespace()).execute();
        boolean isSuccessful = response.isSuccessful();
        if (isSuccessful) {
          return true;
        } else {
          logAndThrowVaultError(baseVaultConfig, response, "renew");
        }
      } catch (Exception e) {
        failedAttempts++;
        log.warn("renewal failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String message = "renewal failed after " + NUM_OF_RETRIES + " retries";
          throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public List<SecretEngineSummary> listSecretEngines(BaseVaultConfig vaultConfig) {
    List<SecretEngineSummary> secretEngineSummaries = new ArrayList<>();
    try {
      String vaultToken = getToken(vaultConfig);
      if (isEmpty(vaultToken)) {
        VaultAppRoleLoginResult loginResult = appRoleLogin(vaultConfig);
        if (loginResult != null) {
          vaultToken = loginResult.getClientToken();
          vaultConfig.setAuthToken(vaultToken);
        }
      }

      VaultSysAuthRestClient restClient =
          VaultRestClientFactory.getVaultRetrofit(vaultConfig.getVaultUrl(), vaultConfig.isCertValidationRequired())
              .create(VaultSysAuthRestClient.class);
      Response<SysMountsResponse> response = restClient.getAllMounts(vaultToken, vaultConfig.getNamespace()).execute();
      if (response.isSuccessful()) {
        Map<String, SysMount> sysMountMap = response.body().getData();
        log.info("Found Vault sys mount points: {}", sysMountMap.keySet());

        for (Entry<String, SysMount> entry : sysMountMap.entrySet()) {
          String secretEngineName = StringUtils.removeEnd(entry.getKey(), "/");
          SysMount sysMount = entry.getValue();
          Integer version = sysMount.getOptions() == null ? null : sysMount.getOptions().getVersion();
          SecretEngineSummary secretEngineSummary = SecretEngineSummary.builder()
                                                        .name(secretEngineName)
                                                        .description(sysMount.getDescription())
                                                        .type(sysMount.getType())
                                                        .version(version)
                                                        .build();
          secretEngineSummaries.add(secretEngineSummary);
        }
      } else {
        // Throw error when sys mount fails.
        logAndThrowVaultError(vaultConfig, response, "listSecretEngines");
      }
    } catch (IOException e) {
      String message =
          String.format("Failed to list secret engines for %s due to unexpected network error. Please try again.",
              vaultConfig.getVaultUrl());
      log.error(message, e);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, USER);
    }

    return secretEngineSummaries;
  }

  @Override
  public VaultAppRoleLoginResult appRoleLogin(BaseVaultConfig vaultConfig) {
    try {
      VaultSysAuthRestClient restClient =
          VaultRestClientFactory.getVaultRetrofit(vaultConfig.getVaultUrl(), vaultConfig.isCertValidationRequired())
              .create(VaultSysAuthRestClient.class);

      VaultAppRoleLoginRequest loginRequest = VaultAppRoleLoginRequest.builder()
                                                  .roleId(vaultConfig.getAppRoleId())
                                                  .secretId(vaultConfig.getSecretId())
                                                  .build();
      Response<VaultAppRoleLoginResponse> response =
          restClient.appRoleLogin(vaultConfig.getNamespace(), loginRequest).execute();

      VaultAppRoleLoginResult result = null;
      if (response.isSuccessful()) {
        result = response.body().getAuth();
      } else {
        logAndThrowVaultError(vaultConfig, response, "AppRole Based Login");
      }
      return result;
    } catch (IOException e) {
      String message = "Failed to perform AppRole based login for secret manager " + vaultConfig.getName() + " at "
          + vaultConfig.getVaultUrl();
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }
  }

  @Override
  public boolean validateCyberArkConfig(CyberArkConfig cyberArkConfig) {
    String errorMessage;
    // Basic connectivity and certificate validity checks
    if (isNotEmpty(cyberArkConfig.getClientCertificate())
        && !CyberArkRestClientFactory.validateClientCertificate(cyberArkConfig.getClientCertificate())) {
      errorMessage = "Client certificate provided is not valid. Please check your configurations and try again";
      throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, errorMessage, USER);
    }

    try {
      CyberArkRestClient restClient = CyberArkRestClientFactory.create(cyberArkConfig);
      String testQuery = "Username=svc_account_harness_validate_config";
      Response<CyberArkReadResponse> response = restClient.readSecret(cyberArkConfig.getAppId(), testQuery).execute();
      // Expecting a 404 response (or 200 by accident) as the test query of a non-existent account in normal cases.
      int status = response.code();
      if (status != 404 && status != 200) {
        errorMessage = "Failed to query the CyberArk REST endpoint. Please check your configurations and try again";
        throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, errorMessage, USER);
      }
    } catch (IOException e) {
      errorMessage = "Failed to test a sample CyberArk query. Please check your configurations and try again";
      throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, errorMessage, e, USER);
    }

    return false;
  }

  private void logAndThrowVaultError(BaseVaultConfig baseVaultConfig, Response response, String operation)
      throws IOException {
    if (baseVaultConfig == null || response == null) {
      return;
    }
    String errorMsg = "";
    if (response.errorBody() != null) {
      errorMsg =
          String.format("Failed to %s for Vault: %s And Namespace: %s due to the following error from vault: \"%s\".",
              operation, baseVaultConfig.getName(), baseVaultConfig.getNamespace(), response.errorBody().string());
    } else {
      errorMsg = String.format(
          "Failed to %s for Vault: %s And Namespace: %s due to the following error from vault: \"%s\".", operation,
          baseVaultConfig.getName(), baseVaultConfig.getNamespace(), response.message() + response.body());
    }
    log.error(errorMsg);
    throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, errorMsg, USER);
  }

  private String getToken(BaseVaultConfig vaultConfig) {
    if (vaultConfig.isUseVaultAgent()) {
      try {
        byte[] content = Files.readAllBytes(Paths.get(URI.create("file://" + vaultConfig.getSinkPath())));
        return new String(content);
      } catch (IOException e) {
        throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR,
            "Using Vault Agent Cannot read Token From Sink Path:" + vaultConfig.getSinkPath(), e, USER);
      }
    } else {
      return vaultConfig.getAuthToken();
    }
  }
}
