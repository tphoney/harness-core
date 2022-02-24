/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.data;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.ExecutionSweepingOutputInstance;
import io.harness.data.ExecutionSweepingOutputInstance.ExecutionSweepingOutputKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.expressions.functors.NodeExecutionEntityType;
import io.harness.exception.UnresolvedExpressionsException;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.data.output.PmsSweepingOutput;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mongodb.DuplicateKeyException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.jexl3.JexlException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSweepingOutputServiceImpl implements PmsSweepingOutputService {
  @Inject private ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Inject private Injector injector;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private PmsOutcomeService pmsOutcomeService;

  @Override
  public String resolve(Ambiance ambiance, RefObject refObject) {
    if (!refObject.getName().contains(".")) {
      // It is not an expression-like ref-object.
      return resolveUsingRuntimeId(ambiance, refObject);
    }

    EngineExpressionEvaluator evaluator =
        expressionEvaluatorProvider.get(null, ambiance, EnumSet.of(NodeExecutionEntityType.SWEEPING_OUTPUT), true);
    injector.injectMembers(evaluator);
    Object value = evaluator.evaluateExpression(EngineExpressionEvaluator.createExpression(refObject.getName()));
    return value == null ? null : RecastOrchestrationUtils.toJson(value);
  }

  private String resolveUsingRuntimeId(Ambiance ambiance, RefObject refObject) {
    String name = refObject.getName();
    ExecutionSweepingOutputInstance instance = getInstance(ambiance, refObject);
    if (instance == null) {
      throw new SweepingOutputException(format("Could not resolve sweeping output with name '%s'", name));
    }

    return instance.getOutputValueJson();
  }

  @Override
  public List<RawOptionalSweepingOutput> findOutputsUsingNodeId(Ambiance ambiance, String name, List<String> nodeIds) {
    Query query = query(where(ExecutionSweepingOutputKeys.planExecutionId).is(ambiance.getPlanExecutionId()))
                      .addCriteria(where(ExecutionSweepingOutputKeys.name).is(name))
                      .addCriteria(where(ExecutionSweepingOutputKeys.producedBy + ".setupId").in(nodeIds));
    List<ExecutionSweepingOutputInstance> instances = mongoTemplate.find(query, ExecutionSweepingOutputInstance.class);
    return instances.stream()
        .map(instance -> RawOptionalSweepingOutput.builder().found(true).output(instance.getOutputValueJson()).build())
        .collect(Collectors.toList());
  }

  @Override
  public List<ExecutionSweepingOutputInstance> fetchOutcomeInstanceByRuntimeId(String runtimeId) {
    Query query = query(where(ExecutionSweepingOutputKeys.producedBy + "."
        + "runtimeId")
                            .is(runtimeId));
    return mongoTemplate.find(query, ExecutionSweepingOutputInstance.class);
  }

  @Override
  public List<String> cloneForRetryExecution(Ambiance ambiance, String originalNodeExecutionUuid) {
    List<String> outputUuids = new ArrayList<>();
    List<ExecutionSweepingOutputInstance> outputInstances = fetchOutcomeInstanceByRuntimeId(originalNodeExecutionUuid);
    for (ExecutionSweepingOutputInstance outputInstance : outputInstances) {
      String uuid = consume(
          ambiance, outputInstance.getName(), outputInstance.getValueOutput().toJson(), outputInstance.getGroupName());
      outputUuids.add(uuid);
    }
    return outputUuids;
  }

  @Override
  public RawOptionalSweepingOutput resolveOptional(Ambiance ambiance, RefObject refObject) {
    if (!refObject.getName().contains(".")) {
      // It is not an expression-like ref-object.
      return resolveOptionalUsingRuntimeId(ambiance, refObject);
    }

    EngineExpressionEvaluator evaluator =
        expressionEvaluatorProvider.get(null, ambiance, EnumSet.of(NodeExecutionEntityType.SWEEPING_OUTPUT), true);
    injector.injectMembers(evaluator);
    try {
      Object value = evaluator.evaluateExpression(EngineExpressionEvaluator.createExpression(refObject.getName()));
      return value == null
          ? RawOptionalSweepingOutput.builder().found(false).build()
          : RawOptionalSweepingOutput.builder().found(true).output(RecastOrchestrationUtils.toJson(value)).build();
    } catch (UnresolvedExpressionsException | JexlException e) {
      return RawOptionalSweepingOutput.builder().found(false).build();
    }
  }

  private RawOptionalSweepingOutput resolveOptionalUsingRuntimeId(Ambiance ambiance, RefObject refObject) {
    ExecutionSweepingOutputInstance instance = getInstance(ambiance, refObject);
    if (instance == null) {
      return RawOptionalSweepingOutput.builder().found(false).build();
    }
    return RawOptionalSweepingOutput.builder().found(true).output(instance.getOutputValueJson()).build();
  }

  private ExecutionSweepingOutputInstance getInstance(Ambiance ambiance, RefObject refObject) {
    String name = refObject.getName();
    Query query = query(where(ExecutionSweepingOutputKeys.planExecutionId).is(ambiance.getPlanExecutionId()))
                      .addCriteria(where(ExecutionSweepingOutputKeys.name).is(name))
                      .addCriteria(where(ExecutionSweepingOutputKeys.levelRuntimeIdIdx)
                                       .in(ResolverUtils.prepareLevelRuntimeIdIndices(ambiance)));
    List<ExecutionSweepingOutputInstance> instances = mongoTemplate.find(query, ExecutionSweepingOutputInstance.class);
    // Multiple instances might be returned if the same name was saved at different levels/specificity.
    return EmptyPredicate.isEmpty(instances)
        ? null
        : instances.stream()
              .max(Comparator.comparing(ExecutionSweepingOutputInstance::getLevelRuntimeIdIdx))
              .orElse(null);
  }

  @Override
  public String consumeInternal(Ambiance ambiance, Level producedBy, String name, String value, String groupName) {
    try {
      ExecutionSweepingOutputInstance instance =
          mongoTemplate.insert(ExecutionSweepingOutputInstance.builder()
                                   .uuid(generateUuid())
                                   .planExecutionId(ambiance.getPlanExecutionId())
                                   .stageExecutionId(ambiance.getStageExecutionId())
                                   .producedBy(producedBy)
                                   .name(name)
                                   .valueOutput(PmsSweepingOutput.parse(value))
                                   .levelRuntimeIdIdx(ResolverUtils.prepareLevelRuntimeIdIdx(ambiance.getLevelsList()))
                                   .groupName(groupName)
                                   .build());
      return instance.getUuid();
    } catch (DuplicateKeyException ex) {
      throw new SweepingOutputException(format("Sweeping output with name %s is already saved", name), ex);
    }
  }
}
