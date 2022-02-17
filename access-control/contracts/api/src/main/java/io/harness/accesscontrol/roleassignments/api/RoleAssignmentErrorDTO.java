package io.harness.accesscontrol.roleassignments.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.PL)
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel(value = "RoleAssignmentError")
@Schema(name = "RoleAssignmentError")
public class RoleAssignmentErrorDTO extends ErrorDTO {
  private RoleAssignmentDTO roleAssignmentPayload;

  public RoleAssignmentErrorDTO(
      Status status, ErrorCode code, String message, String detailedMessage, RoleAssignmentDTO roleAssignmentPayload) {
    super(status, code, message, detailedMessage);
    this.roleAssignmentPayload = roleAssignmentPayload;
  }
}
