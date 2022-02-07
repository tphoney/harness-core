package software.wings.delegatetasks.azure.appservice.deployment;

import io.harness.azure.impl.SlotContainerLogStreamer;

import software.wings.delegatetasks.azure.appservice.deployment.context.SlotContainerDeploymentVerifierContext;

import lombok.Getter;

public class SlotContainerDeploymentVerifier extends SlotStatusVerifier {
  @Getter private final SlotContainerLogStreamer logStreamer;

  public SlotContainerDeploymentVerifier(SlotContainerDeploymentVerifierContext context) {
    super(context.getLogCallback(), context.getSlotName(), context.getAzureWebClient(),
        context.getAzureWebClientContext(), null);
    this.logStreamer = context.getLogStreamer();
  }

  @Override
  public boolean hasReachedSteadyState() {
    logStreamer.readContainerLogs();
    return logStreamer.isSuccess();
  }
  @Override
  public String getSteadyState() {
    return null;
  }

  @Override
  public boolean operationFailed() {
    return logStreamer.failed();
  }

  @Override
  public String getErrorMessage() {
    return logStreamer.getErrorLog();
  }
}
