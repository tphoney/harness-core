/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

public class SecretManagerDescriptionConstants {
  public static final String HARNESS_MANAGED =
      "This is true if this Secret Manager is managed by Harness. Harness can manage and edit this Secret Manager in this case.";
  public static final String DEFAULT =
      "Boolean value to indicate if the Secret Manager is your default Secret Manager.";
  public static final String DELEGATE_SELECTORS =
      "List of Delegate Selectors that belong to the same Delegate and are used to connect to the Secret Manager.";
  public static final String BASE_PATH = "This is the location of the Vault directory where Secret will be stored.";
  public static final String AUTH_TOKEN = "This is the authentication token for Vault.";
  public static final String NAMESPACE = "This is the Vault namespace where Secret will be created.";
  public static final String VAULT_AWS_IAM_ROLE =
      "This is the Vault role defined to bind to aws iam account/role being accessed.";
  public static final String USE_VAULT_AGENT = "Boolean value to indicate if Vault Agent is used for authentication.";
  public static final String USE_AWS_IAM = "Boolean value to indicate if Aws Iam is used for authentication.";
  public static final String AWS_REGION = "This is the Aws region where aws iam auth will happen.";
  public static final String VAULT_AWS_IAM_HEADER =
      "This is the Aws Iam Header Server ID that has been configured for this Aws Iam instance.";
  public static final String VAULT_URL = "URL of the Vault.";
  public static final String READ_ONLY = "Boolean value to indicate if the Secret Manager created is read only.";
  public static final String RENEWAL_INTERVAL_MINUTES = "This is the time interval for token renewal.";
  public static final String SECRET_ENGINE_NAME = "Name of the Secret Engine.";
  public static final String APP_ROLE_ID = "ID of App Role.";
  public static final String SECRET_ID = "ID of the Secret.";
  public static final String SECRET_ENGINE_VERSION = "Version of Secret Engine.";
  public static final String ENGINE_ENTERED_MANUALLY = "Manually entered Secret Engine.";
  public static final String AWS_CREDENTIAL =
      "This indicates AWS credential types, Manual Credential, Assume IAM Role, Assume STS Role.";
  public static final String ACCESS_KEY = "Access Key for AWS authentication.";
  public static final String SECRET_KEY = "Secret Key for AWS authentication.";
  public static final String ROLE_ARN = "Role ARN for the Delegate with STS Role.";
  public static final String EXTERNAL_NAME = "External Name.";
  public static final String ASSUME_STS_ROLE_DURATION = "This is the time duration for STS Role.";
  public static final String SINK_PATH = "This is the location at which auth token is to be read from.";
}
