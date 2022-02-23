package io.harness.engine.pms.execution.strategy;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.pms.execution.strategy.identity.IdentityNodeExecutionStrategy;
import io.harness.engine.pms.execution.strategy.plan.PlanExecutionStrategy;
import io.harness.engine.pms.execution.strategy.plannode.PlanNodeExecutionStrategy;
import io.harness.plan.NodeType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NodeExecutionStrategyFactoryTest extends OrchestrationTestBase {
  @Inject private NodeExecutionStrategyFactory factory;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainStrategy() {
    assertThat(factory.obtainStrategy(NodeType.PLAN_NODE)).isInstanceOf(PlanNodeExecutionStrategy.class);
    assertThat(factory.obtainStrategy(NodeType.PLAN)).isInstanceOf(PlanExecutionStrategy.class);
    assertThat(factory.obtainStrategy(NodeType.IDENTITY_PLAN_NODE)).isInstanceOf(IdentityNodeExecutionStrategy.class);
  }
}