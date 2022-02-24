/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.data;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ResolverUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testPrepareLevelRuntimeIdIdx() {
    ImmutableList<Level> levels = ImmutableList.of(Level.newBuilder().setRuntimeId("pipelineId").build(),
        Level.newBuilder().setRuntimeId("stageId").build(), Level.newBuilder().setRuntimeId("stepId").build());

    assertThat(ResolverUtils.prepareLevelRuntimeIdIdx(levels)).isEqualTo("pipelineId|stageId|stepId");
    assertThat(ResolverUtils.prepareLevelRuntimeIdIdx(new ArrayList<>())).isEqualTo("");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testPrepareLevelRuntimeIdIndices() {
    Ambiance ambiance =
        Ambiance.newBuilder()
            .addAllLevels(ImmutableList.of(Level.newBuilder().setRuntimeId("pipelineId").build(),
                Level.newBuilder().setRuntimeId("stageId").build(), Level.newBuilder().setRuntimeId("stepId").build()))
            .build();
    assertThat(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance)).hasSize(4);
    assertThat(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance))
        .containsExactly("", "pipelineId", "pipelineId|stageId", "pipelineId|stageId|stepId");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testPrepareLevelRuntimeIdIndicesNoLevels() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    assertThat(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance)).hasSize(1);
    assertThat(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance)).containsExactly("");
  }
}
