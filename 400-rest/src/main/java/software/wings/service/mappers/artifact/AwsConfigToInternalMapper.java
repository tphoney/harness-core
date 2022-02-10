/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.mappers.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;

import software.wings.beans.AwsConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
public class AwsConfigToInternalMapper {
  public AwsInternalConfig toAwsInternalConfig(AwsConfig awsConfig) {
    return AwsInternalConfig.builder()
        .accessKey(awsConfig.getAccessKey())
        .secretKey(awsConfig.getSecretKey())
        .useEc2IamCredentials(awsConfig.isUseEc2IamCredentials())
        .assumeCrossAccountRole(awsConfig.isAssumeCrossAccountRole())
        .crossAccountAttributes(awsConfig.getCrossAccountAttributes())
        .defaultRegion(awsConfig.getDefaultRegion())
        .useIRSA(awsConfig.isUseIRSA())
        .amazonClientSDKDefaultBackoffStrategy(awsConfig.getAmazonClientSDKDefaultBackoffStrategy())
        .build();
  }
}
