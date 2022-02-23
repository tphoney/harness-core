package io.harness.engine.pms.resume;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.rule.Owner;

import java.util.HashMap;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class EngineResumeAllCallbackTest extends OrchestrationTestBase {
  @Mock OrchestrationEngine engine;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testNotify() {
    Ambiance ambiance = Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build();
    EngineResumeAllCallback callback = EngineResumeAllCallback.builder().ambiance(ambiance).build();
    Reflect.on(callback).set("orchestrationEngine", engine);
    callback.notify(new HashMap<>());
    verify(engine).startNodeExecution(eq(ambiance));
  }
}