package io.harness.ng.core.delegate.client;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
@OwnedBy(DEL)
public class DelegateTokenNgClientFactory extends AbstractHttpClientFactory implements Provider<DelegateTokenNgClient> {
  protected DelegateTokenNgClientFactory(ServiceHttpClientConfig cgManagerConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(cgManagerConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public DelegateTokenNgClient get() {
    return getRetrofit().create(DelegateTokenNgClient.class);
  }
}
