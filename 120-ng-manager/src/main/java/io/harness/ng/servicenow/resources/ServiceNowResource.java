/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.servicenow.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.servicenow.resources.service.ServiceNowResourceService;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.servicenow.ServiceNowFieldAllowedValueNG;
import io.harness.servicenow.ServiceNowFieldNG;
import io.harness.servicenow.ServiceNowTicketTypeDTO;
import io.harness.servicenow.ServiceNowTicketTypeNG;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(CDC)
@Api("servicenow")
@Path("/servicenow")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class ServiceNowResource {
  private final ServiceNowResourceService serviceNowResourceService;
  @GET
  @Path("ticketTypes")
  @ApiOperation(value = "Get serviceNow ticket types", nickname = "getServiceNowTicketTypes")
  public ResponseDTO<List<ServiceNowTicketTypeDTO>> getTicketTypes(
      @NotNull @QueryParam("connectorRef") String serviceNowConnectorRef,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    return ResponseDTO.newResponse(
        Arrays.stream(ServiceNowTicketTypeNG.values())
            .map(ticketType -> new ServiceNowTicketTypeDTO(ticketType.name(), ticketType.getDisplayName()))
            .collect(Collectors.toList()));
  }

  @GET
  @Path("createMetadata")
  @ApiOperation(value = "Get ServiceNow issue create metadata", nickname = "getServiceNowIssueCreateMetadata")
  public ResponseDTO<List<ServiceNowFieldNG>> getIssueCreateMetadata(
      @NotNull @QueryParam("connectorRef") String serviceNowConnectorRef,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId, @QueryParam("ticketType") String ticketType,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(serviceNowConnectorRef, accountId, orgId, projectId);
    List<ServiceNowFieldNG> issueCreateMetadataResponse =
        serviceNowResourceService.getIssueCreateMetadata(connectorRef, orgId, projectId, ticketType);
    return ResponseDTO.newResponse(issueCreateMetadataResponse);
  }

  @GET
  @Path("issueFields")
  @ApiOperation(value = "Get fields for an issue type", nickname = "getServiceNowIssueFields")
  public ResponseDTO<Map<String, List<ServiceNowFieldAllowedValueNG>>> getFieldsForIssueType(
      @NotNull @QueryParam("connectorRef") String serviceNowConnectorRef,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId, @QueryParam("ticketType") String ticketType,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(serviceNowConnectorRef, accountId, orgId, projectId);
    Map<String, List<ServiceNowFieldAllowedValueNG>> issueTypeFieldsMetadata = new HashMap<>();
    issueTypeFieldsMetadata.put("impact",
        new ArrayList<>(Arrays.asList(ServiceNowFieldAllowedValueNG.builder().id("1").name("1 - High").build())));

    return ResponseDTO.newResponse(issueTypeFieldsMetadata);
  }
}
