package io.harness.accesscontrol.health;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.ExposeInternalException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.PublicApi;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("health")
@Path("/health")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ExposeInternalException
@PublicApi
@OwnedBy(PL)
public interface HealthResource {
  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(
      value = "get health for Access Control service", nickname = "getAccessControlHealthStatus", hidden = true)
  @Operation(hidden = true)
  ResponseDTO<String>
  get() throws Exception;
}
