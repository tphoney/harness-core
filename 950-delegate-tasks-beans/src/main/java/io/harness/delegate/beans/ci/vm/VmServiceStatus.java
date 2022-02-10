package io.harness.delegate.beans.ci.vm;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VmServiceStatus {
  @NotNull String identifier;
  String name;
  String image;
  String logKey;

  public enum Status {
    RUNNING,
    ERROR;
  }
  Status status;
  String errorMessage;
}