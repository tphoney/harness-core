package software.wings.delegatetasks.azure.appservice.deployment.context;

import io.harness.azure.client.AzureWebClient;
import io.harness.azure.context.AzureWebClientContext;
import io.harness.azure.impl.SlotContainerLogStreamer;
import io.harness.logging.LogCallback;

import software.wings.delegatetasks.azure.AzureServiceCallBack;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

public class SlotContainerDeploymentVerifierContext extends StatusVerifierContext {
  @Getter private final SlotContainerLogStreamer logStreamer;

  @Builder
  public SlotContainerDeploymentVerifierContext(@NonNull LogCallback logCallback, @NonNull String slotName,
      @NonNull AzureWebClient azureWebClient, @NonNull AzureWebClientContext azureWebClientContext,
      AzureServiceCallBack restCallBack, SlotContainerLogStreamer logStreamer) {
    super(logCallback, slotName, azureWebClient, azureWebClientContext, restCallBack);
    this.logStreamer = logStreamer;
  }
}
