/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.HarnessCDEventMetadata;
import io.harness.cvng.beans.change.HarnessCDEventMetadata.HarnessCDEventMetadataBuilder;
import io.harness.cvng.beans.change.HarnessCDEventMetadata.VerifyStepSummary;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class HarnessCDChangeEventTransformer
    extends ChangeEventMetaDataTransformer<DeploymentActivity, HarnessCDEventMetadata> {
  @Override
  public DeploymentActivity getEntity(ChangeEventDTO changeEventDTO) {
    HarnessCDEventMetadata metaData = (HarnessCDEventMetadata) changeEventDTO.getMetadata();
    return DeploymentActivity.builder()
        .accountId(changeEventDTO.getAccountId())
        .activityName(
            "Deployment of " + changeEventDTO.getServiceIdentifier() + " in " + changeEventDTO.getEnvIdentifier())
        .orgIdentifier(changeEventDTO.getOrgIdentifier())
        .projectIdentifier(changeEventDTO.getProjectIdentifier())
        .serviceIdentifier(changeEventDTO.getServiceIdentifier())
        .environmentIdentifier(changeEventDTO.getEnvIdentifier())
        .eventTime(Instant.ofEpochMilli(changeEventDTO.getEventTime()))
        .changeSourceIdentifier(changeEventDTO.getChangeSourceIdentifier())
        .monitoredServiceIdentifier(changeEventDTO.getMonitoredServiceIdentifier())
        .type(changeEventDTO.getType().getActivityType())
        .deploymentStatus(metaData.getStatus())
        .planExecutionId(metaData.getPlanExecutionId())
        .stageStepId(metaData.getStageStepId())
        .pipelineId(metaData.getPipelineId())
        .stageId(metaData.getStageId())
        .planExecutionId(metaData.getPlanExecutionId())
        .activityStartTime(Instant.ofEpochMilli(metaData.getDeploymentStartTime()))
        .activityEndTime(Instant.ofEpochMilli(metaData.getDeploymentEndTime()))
        .artifactTag(metaData.getArtifactTag())
        .artifactType(metaData.getArtifactType())
        .build();
  }

  @Override
  protected HarnessCDEventMetadata getMetadata(DeploymentActivity activity) {
    HarnessCDEventMetadataBuilder harnessCDEventMetadataBuilder =
        HarnessCDEventMetadata.builder()
            .deploymentEndTime(activity.getActivityEndTime().toEpochMilli())
            .deploymentStartTime(activity.getActivityStartTime().toEpochMilli())
            .stageId(activity.getStageId())
            .status(activity.getDeploymentStatus())
            .planExecutionId(activity.getPlanExecutionId())
            .stageStepId(activity.getStageStepId())
            .pipelineId(activity.getPipelineId())
            .stageId(activity.getStageId())
            .artifactTag(activity.getArtifactTag())
            .pipelinePath("/account/" + activity.getAccountId() + "/cd/orgs/" + activity.getOrgIdentifier()
                + "/projects/" + activity.getProjectIdentifier() + "/pipelines/" + activity.getPipelineId()
                + "/executions/" + activity.getPlanExecutionId() + "/pipeline?stage=" + activity.getStageStepId())
            .artifactType(activity.getArtifactType());
    if (activity.getVerificationSummary() != null
        && activity.getVerificationSummary().getVerficationStatusMap() != null) {
      List<VerifyStepSummary> verifyStepSummaries =
          activity.getVerificationSummary()
              .getVerficationStatusMap()
              .entrySet()
              .stream()
              .map(entry
                  -> VerifyStepSummary.builder().name(entry.getKey()).verificationStatus(entry.getValue()).build())
              .collect(Collectors.toList());
      harnessCDEventMetadataBuilder.verifyStepSummaries(verifyStepSummaries);
    }
    return harnessCDEventMetadataBuilder.build();
  }
}
