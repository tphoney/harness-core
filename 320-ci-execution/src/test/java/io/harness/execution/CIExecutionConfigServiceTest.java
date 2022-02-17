package io.harness.execution;

import static io.harness.rule.OwnerRule.AMAN;

import io.harness.category.element.UnitTests;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.repositories.CIExecutionConfigRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;

@Slf4j
public class CIExecutionConfigServiceTest extends CIExecutionTestBase {
  @Mock CIExecutionServiceConfig ciExecutionServiceConfig;
  @Mock CIExecutionConfigRepository configRepository;
  @Inject CIExecutionConfigService ciExecutionConfigService;

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void isUsingDeprecatedTagForCIContainer() {
    Mockito.when(ciExecutionServiceConfig.getCiImageTag()).thenReturn("abc");
    Mockito.when(configRepository.findFirstByAccountIdentifier(Mockito.anyString())).thenReturn(Optional.empty());
    ciExecutionConfigService.getDeprecatedTags("acca");
  }
}
