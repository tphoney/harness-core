/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.perpetualtask;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public final class PerpetualTaskType {
  public static final String K8S_WATCH = "K8S_WATCH";
  public static final String ECS_CLUSTER = "ECS_CLUSTER";
  public static final String SAMPLE = "SAMPLE";
  public static final String ARTIFACT_COLLECTION = "ARTIFACT_COLLECTION";
  public static final String PCF_INSTANCE_SYNC = "PCF_INSTANCE_SYNC";
  public static final String AWS_SSH_INSTANCE_SYNC = "AWS_SSH_INSTANCE_SYNC";
  public static final String PDC_INSTANCE_SYNC = "PDC_INSTANCE_SYNC";
  public static final String AWS_AMI_INSTANCE_SYNC = "AWS_AMI_INSTANCE_SYNC";
  public static final String AWS_CODE_DEPLOY_INSTANCE_SYNC = "AWS_CODE_DEPLOY_INSTANCE_SYNC";
  public static final String SPOT_INST_AMI_INSTANCE_SYNC = "SPOT_INST_AMI_INSTANCE_SYNC";
  public static final String CONTAINER_INSTANCE_SYNC = "CONTAINER_INSTANCE_SYNC";
  public static final String AWS_LAMBDA_INSTANCE_SYNC = "AWS_LAMBDA_INSTANCE_SYNC";
  public static final String CUSTOM_DEPLOYMENT_INSTANCE_SYNC = "CUSTOM_DEPLOYMENT_INSTANCE_SYNC";
  public static final String DATA_COLLECTION_TASK = "DATA_COLLECTION_TASK";
  public static final String K8_ACTIVITY_COLLECTION_TASK = "KUBERNETES_ACTIVITY_COLLECTION_TASK";
  public static final String AZURE_VMSS_INSTANCE_SYNC = "AZURE_VMSS_INSTANCE_SYNC";
  public static final String MANIFEST_COLLECTION = "MANIFEST_COLLECTION";
  public static final String CONNECTOR_TEST_CONNECTION = "CONNECTOR_TEST_CONNECTION";
  public static final String AZURE_WEB_APP_INSTANCE_SYNC = "AZURE_WEB_APP_INSTANCE_SYNC";

  // NG
  public static final String K8S_INSTANCE_SYNC = "K8S_INSTANCE_SYNC";
  public static final String MANIFEST_COLLECTION_NG = "MANIFEST_COLLECTION_NG";
  public static final String ARTIFACT_COLLECTION_NG = "ARTIFACT_COLLECTION_NG";
  public static final String NATIVE_HELM_INSTANCE_SYNC = "NATIVE_HELM_INSTANCE_SYNC";

  private PerpetualTaskType() {}
}
