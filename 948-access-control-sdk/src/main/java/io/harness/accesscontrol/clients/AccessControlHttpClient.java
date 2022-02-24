/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.clients;

import io.harness.accesscontrol.acl.api.AccessCheckRequestDTO;
import io.harness.accesscontrol.acl.api.AccessCheckResponseDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlHttpClient {
  String ACL_API = "acl";

  @POST(ACL_API)
  Call<ResponseDTO<AccessCheckResponseDTO>> checkForAccess(@Body AccessCheckRequestDTO accessCheckRequestDTO);
}
