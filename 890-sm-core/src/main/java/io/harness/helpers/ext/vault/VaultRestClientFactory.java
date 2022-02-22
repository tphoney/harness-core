/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.runtime.HashiCorpVaultRuntimeException;
import io.harness.network.Http;

import software.wings.beans.VaultConfig;
import software.wings.settings.SettingVariableTypes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * A Factory class that's capable of constructing a REST client to talked to V1 or V2 secret engine backed
 * Vault server.
 */

@UtilityClass
@Slf4j
@OwnedBy(PL)
public class VaultRestClientFactory {
  private static final String PATH_SEPARATOR = "/";
  private static final String KEY_NAME_SEPARATOR = "#";

  private static final String DEFAULT_BASE_PATH = "harness";
  private static final String DEFAULT_KEY_NAME = "value";

  // This Jackson object mapper always ignore unknown properties while deserialize JSON documents.
  private static ObjectMapper objectMapper = new ObjectMapper();
  private static HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    loggingInterceptor.setLevel(Level.NONE);
  }

  public static Retrofit getVaultRetrofit(String vaultUrl, boolean isCertValidationRequired) {
    OkHttpClient httpClient;
    if (isCertValidationRequired) {
      httpClient = Http.getSafeOkHttpClientBuilder(vaultUrl, 10, 10).addInterceptor(loggingInterceptor).build();
    } else {
      httpClient = Http.getUnsafeOkHttpClientBuilder(vaultUrl, 10, 10).addInterceptor(loggingInterceptor).build();
    }
    return new Retrofit.Builder()
        .baseUrl(vaultUrl)
        .addConverterFactory(JacksonConverterFactory.create(objectMapper))
        .client(httpClient)
        .build();
  }

  public static VaultRestClient create(final VaultConfig vaultConfig) {
    final Retrofit retrofit = getVaultRetrofit(vaultConfig.getVaultUrl(), vaultConfig.isCertValidationRequired());

    int version = vaultConfig.getSecretEngineVersion();
    switch (version) {
      case 0:
      case 1:
        return new V1Impl(retrofit.create(VaultRestClientV1.class));
      case 2:
        return new V2Impl(retrofit.create(VaultRestClientV2.class));
      default:
        throw new IllegalArgumentException("Unsupported Vault secret engine version: " + version);
    }
  }

  public static String getFullPath(String basePath, String secretName, SettingVariableTypes settingVariableType) {
    if (isEmpty(basePath)) {
      return DEFAULT_BASE_PATH + PATH_SEPARATOR + settingVariableType + PATH_SEPARATOR + secretName;
    } else {
      String fullPath = StringUtils.stripStart(basePath, PATH_SEPARATOR);
      return StringUtils.stripEnd(fullPath, PATH_SEPARATOR) + PATH_SEPARATOR + settingVariableType + PATH_SEPARATOR
          + secretName;
    }
  }

  /**
   * Note: the absolute path may look like something below:
   * /foo/bar/MySecret#MyKeyName
   */
  public static String getFullPath(String basePath, String secretPath) {
    if (isEmpty(basePath)) {
      return DEFAULT_BASE_PATH + PATH_SEPARATOR + secretPath;
    } else {
      String fullPath = StringUtils.stripStart(basePath, PATH_SEPARATOR);
      return StringUtils.stripEnd(fullPath, PATH_SEPARATOR) + PATH_SEPARATOR + secretPath;
    }
  }

  private static VaultPathAndKey parseFullPath(String fullPath) {
    // Strip the leading '/' if present.
    fullPath = fullPath.startsWith(PATH_SEPARATOR) ? fullPath.substring(1) : fullPath;
    VaultPathAndKey result = new VaultPathAndKey();
    int index = fullPath.indexOf(KEY_NAME_SEPARATOR);
    if (index > 0) {
      result.path = fullPath.substring(0, index);
      result.keyName = fullPath.substring(index + 1);
    } else {
      result.path = fullPath;
      result.keyName = DEFAULT_KEY_NAME;
    }
    return result;
  }

  static class V1Impl implements VaultRestClient {
    private VaultRestClientV1 vaultRestClient;

    V1Impl(VaultRestClientV1 vaultRestClient) {
      this.vaultRestClient = vaultRestClient;
    }

    @Override
    public boolean writeSecret(String authToken, String namespace, String secretEngine, String fullPath, String value)
        throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      Map<String, String> valueMap = new HashMap<>();
      valueMap.put(pathAndKey.keyName, value);
      Response<Void> response =
          vaultRestClient.writeSecret(authToken, namespace, secretEngine, pathAndKey.path, valueMap).execute();
      logErrorIfRequestFailed(response, "V1Impl- writeSecret", true);
      return response.isSuccessful();
    }

    @Override
    public boolean deleteSecret(String authToken, String namespace, String secretEngine, String fullPath)
        throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      Response<Void> response =
          vaultRestClient.deleteSecret(authToken, namespace, secretEngine, pathAndKey.path).execute();
      logErrorIfRequestFailed(response, "V1Impl-deleteSecret", false);
      return response.isSuccessful();
    }

    @Override
    public String readSecret(String authToken, String namespace, String secretEngine, String fullPath)
        throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      Response<VaultReadResponse> result =
          vaultRestClient.readSecret(authToken, namespace, secretEngine, pathAndKey.path).execute();
      logErrorIfRequestFailed(result, "V1Impl-readSecret", false);
      VaultReadResponse response = result.body();
      return response == null || response.getData() == null ? null : response.getData().get(pathAndKey.keyName);
    }

    @Override
    public VaultSecretMetadata readSecretMetadata(
        String authToken, String namespace, String secretEngine, String fullPath) throws IOException {
      // Older Vault services doesn't have secret version metadata available!
      return null;
    }
  }

  static class V2Impl implements VaultRestClient {
    private VaultRestClientV2 vaultRestClient;

    V2Impl(VaultRestClientV2 vaultRestClient) {
      this.vaultRestClient = vaultRestClient;
    }

    @Override
    public boolean writeSecret(String authToken, String namespace, String secretEngine, String fullPath, String value)
        throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      Map<String, String> dataMap = new HashMap<>();
      dataMap.put(pathAndKey.keyName, value);
      VaultSecretValue vaultSecretValue = new VaultSecretValue(dataMap);
      Response<Void> response =
          vaultRestClient.writeSecret(authToken, namespace, secretEngine, pathAndKey.path, vaultSecretValue).execute();
      logErrorIfRequestFailed(response, "V2Impl-writeSecret", true);
      return response.isSuccessful();
    }

    @Override
    public boolean deleteSecret(String authToken, String namespace, String secretEngine, String fullPath)
        throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      Response<Void> response =
          vaultRestClient.deleteSecret(authToken, namespace, secretEngine, pathAndKey.path).execute();
      logErrorIfRequestFailed(response, "V2Impl-deleteSecret", false);
      return response.isSuccessful();
    }

    @Override
    public String readSecret(String authToken, String namespace, String secretEngine, String fullPath)
        throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);

      Response<VaultReadResponseV2> result =
          vaultRestClient.readSecret(authToken, namespace, secretEngine, pathAndKey.path).execute();
      if (result.isSuccessful()) {
        VaultReadResponseV2 response = result.body();
        return response == null || response.getData() == null ? null
                                                              : response.getData().getData().get(pathAndKey.keyName);
      }
      logErrorIfRequestFailed(result, "V2Impl-readSecret", false);
      return null;
    }

    @Override
    public VaultSecretMetadata readSecretMetadata(
        String authToken, String namespace, String secretEngine, String fullPath) throws IOException {
      VaultPathAndKey pathAndKey = parseFullPath(fullPath);
      Response<VaultMetadataReadResponse> result =
          vaultRestClient.readSecretMetadata(authToken, namespace, secretEngine, pathAndKey.path).execute();
      logErrorIfRequestFailed(result, "V2Impl-readSecretMetadata", false);
      VaultMetadataReadResponse response = result.body();
      return response == null ? null : response.getData();
    }
  }

  private static void logErrorIfRequestFailed(Response response, String action, Boolean throwError) throws IOException {
    if (response == null) {
      return;
    }
    if (!response.isSuccessful()) {
      String message;
      if (response.errorBody() != null) {
        message = response.errorBody().string();
      } else {
        message = response.message() + response.body();
      }
      log.error("Could not {} secret in the vault due to the following error {}", action, message);
      if (throwError) {
        throw new HashiCorpVaultRuntimeException(message);
      }
    }
  }

  private static class VaultPathAndKey {
    String path;
    String keyName;
  }
}
