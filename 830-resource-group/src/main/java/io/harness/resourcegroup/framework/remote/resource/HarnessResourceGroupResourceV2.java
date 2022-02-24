/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.resourcegroup.framework.remote.resource;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_CODE;
import static io.harness.NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.IDENTIFIER_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE;
import static io.harness.NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;
import static io.harness.resourcegroup.ResourceGroupPermissions.DELETE_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupPermissions.EDIT_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupPermissions.VIEW_RESOURCEGROUP_PERMISSION;
import static io.harness.resourcegroup.ResourceGroupResourceTypes.RESOURCE_GROUP;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccessDeniedErrorDTO;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.enforcement.client.annotation.FeatureRestrictionCheck;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.framework.service.ResourceGroupV2Service;
import io.harness.resourcegroup.framework.service.impl.ResourceGroupV2ValidatorImpl;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2DTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2FilterDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2Request;
import io.harness.resourcegroupclient.ResourceGroupV2Response;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

@Api("/v2/resourcegroup")
@Path("/v2/resourcegroup")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@Tag(name = "Harness Resource Group", description = "This contains APIs specific to the Harness Resource Group")
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = BAD_REQUEST_CODE, description = BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.
ApiResponse(responseCode = INTERNAL_SERVER_ERROR_CODE, description = INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = APPLICATION_JSON_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = APPLICATION_YAML_MEDIA_TYPE, schema = @Schema(implementation = ErrorDTO.class))
    })
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error"),
          @ApiResponse(code = 403, response = AccessDeniedErrorDTO.class, message = "Unauthorized")
    })
@NextGenManagerAuth
@OwnedBy(HarnessTeam.PL)
public class HarnessResourceGroupResourceV2 {
  ResourceGroupV2Service resourceGroupV2Service;
  ResourceGroupV2ValidatorImpl resourceGroupV2Validator;

  @Path("{identifier}")
  @ApiOperation(value = "Get a resource group by Identifier", nickname = "getResourceGroupV2")
  @Operation(operationId = "getResourceGroupV2", summary = "Get a resource group by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This returns a Resource Group specific to the Identifier")
      })
  @NGAccessControlCheck(resourceType = RESOURCE_GROUP, permission = VIEW_RESOURCEGROUP_PERMISSION)
  public ResponseDTO<ResourceGroupV2Response>
  get(@Parameter(description = IDENTIFIER_PARAM_MESSAGE, required = true) @NotNull @PathParam(
          NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(ResourceGroupV2Response.builder().build());
  }

  @GET
  @Path("internal/{identifier}")
  @ApiOperation(
      value = "Get a resource group by identifier internal", nickname = "getResourceGroupInternalV2", hidden = true)
  @Operation(operationId = "getResourceGroupInternalV2", summary = "Get a resource group by Identifier internal",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Internal API to return Resource Group by Identifier")
      },
      hidden = true)
  @InternalApi
  @NGAccessControlCheck(resourceType = RESOURCE_GROUP, permission = VIEW_RESOURCEGROUP_PERMISSION)
  public ResponseDTO<ResourceGroupV2Response>
  getInternal(@Parameter(description = IDENTIFIER_PARAM_MESSAGE) @NotNull @PathParam(
                  NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(ResourceGroupV2Response.builder().build());
  }

  @GET
  @ApiOperation(value = "Get list of resource groups", nickname = "getResourceGroupListV2")
  @Operation(operationId = "getResourceGroupListV2", summary = "Get list of resource groups",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "This contains a list of Resource Groups")
      })
  public ResponseDTO<PageResponse<ResourceGroupV2Response>>
  list(@Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull @QueryParam(
           NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Parameter(
          description =
              "Details of all the resource groups having this string in their name or identifier will be returned.")
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @BeanParam PageRequest pageRequest) {
    return ResponseDTO.newResponse(getNGPageResponse(resourceGroupV2Service.list(
        Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), pageRequest, searchTerm)));
  }

  @POST
  @Path("filter")
  @ApiOperation(value = "Get filtered resource group list", nickname = "getFilterResourceGroupListV2")
  @Operation(operationId = "getFilterResourceGroupListV2",
      description = "This fetches a filtered list of Resource Groups",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "This fetches the list of Resource Groups filtered by multiple fields.")
      })
  public ResponseDTO<PageResponse<ResourceGroupV2Response>>
  list(@RequestBody(description = "Filter Resource Groups based on multiple parameters",
           required = true) @NotNull ResourceGroupV2FilterDTO resourceGroupFilterDTO,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotNull
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier, @BeanParam PageRequest pageRequest) {
    return ResponseDTO.newResponse(getNGPageResponse(resourceGroupV2Service.list(resourceGroupFilterDTO, pageRequest)));
  }

  @POST
  @ApiOperation(value = "Create a resource group", nickname = "createResourceGroupV2")
  @Operation(operationId = "createResourceGroupV2", summary = "Create a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully created a Resource Group")
      })
  @NGAccessControlCheck(resourceType = RESOURCE_GROUP, permission = EDIT_RESOURCEGROUP_PERMISSION)
  @FeatureRestrictionCheck(FeatureRestrictionName.CUSTOM_RESOURCE_GROUPS)
  public ResponseDTO<ResourceGroupV2Response>
  create(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @RequestBody(description = "This contains the details required to create a Resource Group",
          required = true) @Valid ResourceGroupV2Request resourceGroupV2Request) {
    resourceGroupV2Validator.validateResourceGroup(resourceGroupV2Request);
    ResourceGroupV2Response resourceGroupResponse =
        resourceGroupV2Service.create(resourceGroupV2Request.getResourceGroup(), false);
    return ResponseDTO.newResponse(resourceGroupResponse);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a resource group", nickname = "updateResourceGroup")
  @Operation(operationId = "updateResourceGroup", summary = "Update a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully updated a Resource Group")
      })
  @NGAccessControlCheck(resourceType = RESOURCE_GROUP, permission = EDIT_RESOURCEGROUP_PERMISSION)
  public ResponseDTO<ResourceGroupV2Response>
  update(@Parameter(description = IDENTIFIER_PARAM_MESSAGE) @NotNull @PathParam(
             NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @RequestBody(description = "This contains the details required to create a Resource Group",
          required = true) @Valid ResourceGroupV2Request resourceGroupV2Request) {
    resourceGroupV2Validator.validateResourceGroup(resourceGroupV2Request);
    validateRequest(accountIdentifier, orgIdentifier, projectIdentifier, resourceGroupV2Request.getResourceGroup());
    return ResponseDTO.newResponse(
        (resourceGroupV2Service.update(resourceGroupV2Request.getResourceGroup(), false)).orElse(null));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a resource group", nickname = "deleteResourceGroupV2")
  @Operation(operationId = "deleteResourceGroupV2", summary = "Delete a resource group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Successfully deleted a Resource Group")
      })
  @Produces("application/json")
  @Consumes()
  @NGAccessControlCheck(resourceType = RESOURCE_GROUP, permission = DELETE_RESOURCEGROUP_PERMISSION)
  public ResponseDTO<Boolean>
  delete(@NotNull @Parameter(description = IDENTIFIER_PARAM_MESSAGE) @PathParam(
             NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        resourceGroupV2Service.delete(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), identifier));
  }

  private void validateRequest(
      String accountIdentifier, String orgIdentifier, String identifier, ResourceGroupV2DTO resourceGroupV2DTO) {
    verifyValuesNotChanged(
        Lists.newArrayList(Pair.of(accountIdentifier, resourceGroupV2DTO.getAccountIdentifier())), false);
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(orgIdentifier, resourceGroupV2DTO.getOrgIdentifier())), false);
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(identifier, resourceGroupV2DTO.getIdentifier())), false);
  }
}
