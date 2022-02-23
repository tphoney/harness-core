/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cv.beans.analysis;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.delegatetasks.DelegateStateType;

/**
 * Created by rsingh on 5/18/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataCollectionTaskResult implements DelegateTaskNotifyResponseData {
  private DataCollectionTaskStatus status;
  private String errorMessage;
  private DelegateStateType stateType;
  private DelegateMetaInfo delegateMetaInfo;

  // State specific results.....

  // NewRelicDeploymentMarker state
  private String newRelicDeploymentMarkerBody;

  public enum DataCollectionTaskStatus {
    /**
     * Success execution status.
     */
    SUCCESS,
    /**
     * Failure execution status.
     */
    FAILURE;
  }
}
