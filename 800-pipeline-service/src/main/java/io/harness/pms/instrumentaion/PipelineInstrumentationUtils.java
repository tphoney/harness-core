/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.instrumentaion;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PipelineInstrumentationUtils {
  public String getIdentityFromAmbiance(Ambiance ambiance) {
    if (!ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getExtraInfoMap().get("email").isEmpty()) {
      return ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getExtraInfoMap().get("email");
    }
    return ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getIdentifier();
  }

  public Collection<io.harness.exception.FailureType> getFailureTypesFromNodeExecution(NodeExecution nodeExecution) {
    List<FailureType> failureTypes = new ArrayList<>();
    for (FailureData failureData : nodeExecution.getFailureInfo().getFailureDataList()) {
      failureTypes.addAll(failureData.getFailureTypesList());
    }
    return EngineExceptionUtils.transformToWingsFailureTypes(failureTypes);
  }

  public Collection<io.harness.exception.FailureType> getFailureTypesFromPipelineExecutionSummary(
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (pipelineExecutionSummaryEntity.getFailureInfo() == null) {
      return Collections.emptyList();
    }
    return pipelineExecutionSummaryEntity.getFailureInfo().getFailureTypeList();
  }

  public List<String> getErrorMessagesFromNodeExecution(NodeExecution nodeExecution) {
    return nodeExecution.getFailureInfo()
        .getFailureDataList()
        .stream()
        .map(o -> o.getMessage())
        .collect(Collectors.toList());
  }

  public Set<String> getErrorMessagesFromPipelineExecutionSummary(
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (pipelineExecutionSummaryEntity.getFailureInfo() == null) {
      return Collections.emptySet();
    }
    Set<String> errorMessages = new HashSet<>();
    if (!StringUtils.isEmpty(pipelineExecutionSummaryEntity.getFailureInfo().getMessage())) {
      errorMessages.add(pipelineExecutionSummaryEntity.getFailureInfo().getMessage());
    }
    errorMessages.addAll(pipelineExecutionSummaryEntity.getFailureInfo()
                             .getResponseMessages()
                             .stream()
                             .map(o -> o.getMessage())
                             .collect(Collectors.toList()));
    return errorMessages;
  }

  public String extractExceptionMessage(PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (pipelineExecutionSummaryEntity.getFailureInfo() == null) {
      return null;
    }
    return pipelineExecutionSummaryEntity.getFailureInfo()
        .getResponseMessages()
        .stream()
        .filter(o -> o.getCode() != ErrorCode.HINT && o.getCode() != ErrorCode.EXPLANATION)
        .map(ResponseMessage::getMessage)
        .collect(Collectors.toList())
        .toString();
  }
}
