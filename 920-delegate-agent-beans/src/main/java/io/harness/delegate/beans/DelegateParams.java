/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.DEL)
public class DelegateParams {
  String delegateId;
  String accountId;
  String sessionIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String delegateSize;
  String ip;
  String hostName;
  String delegateName;
  String delegateGroupName;
  String delegateGroupId;
  String delegateProfileId;
  String description;
  String version;
  String delegateType;
  String delegateRandomToken;
  String sequenceNum;
  String location;
  long lastHeartBeat;

  boolean ng;
  boolean sampleDelegate;
  boolean keepAlivePacket;
  boolean pollingModeEnabled;
  boolean proxy;
  boolean ceEnabled;

  List<String> supportedTaskTypes;

  List<String> currentlyExecutingDelegateTasks;
  List<String> tags;
}
