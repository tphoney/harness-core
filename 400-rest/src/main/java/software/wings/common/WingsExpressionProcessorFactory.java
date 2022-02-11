/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.ContextElementType;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExpressionProcessor;
import software.wings.sm.ExpressionProcessorFactory;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.List;

/**
 * A factory for creating WingsExpressionProcessor objects.
 */
@OwnedBy(CDC)
@Singleton
public class WingsExpressionProcessorFactory implements ExpressionProcessorFactory {
  @Inject private Injector injector;

  /**
   * Gets matching expression processor.
   *
   * @param expression the expression
   * @param context    the context
   * @return the matching expression processor
   */
  public static ExpressionProcessor getMatchingExpressionProcessor(String expression, ExecutionContext context) {
    ExpressionProcessor processor = new ServiceExpressionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }

    processor = new HostExpressionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }
    processor = new InstanceExpressionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }
    processor = new InstancePartitionExpressionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }

    processor = new AwsLambdaFunctionProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }

    processor = new RancherK8sClusterProcessor(context);
    if (processor.matches(expression)) {
      return processor;
    }

    return null;
  }

  /**
   * Gets default expression.
   *
   * @param contextElementType the context element type
   * @return the default expression
   */
  public static String getDefaultExpression(ContextElementType contextElementType) {
    switch (contextElementType) {
      case SERVICE:
        return ServiceExpressionProcessor.DEFAULT_EXPRESSION;
      case HOST:
        return HostExpressionProcessor.DEFAULT_EXPRESSION;
      case INSTANCE:
        return InstanceExpressionProcessor.DEFAULT_EXPRESSION;
      case AWS_LAMBDA_FUNCTION:
        return AwsLambdaFunctionProcessor.DEFAULT_EXPRESSION;
      case RANCHER_K8S_CLUSTER_CRITERIA:
        return RancherK8sClusterProcessor.DEFAULT_EXPRESSION;
      default:
        return "";
    }
  }

  @Override
  public ExpressionProcessor getExpressionProcessor(String expression, ExecutionContext context) {
    ExpressionProcessor processor = getMatchingExpressionProcessor(expression, context);
    if (processor != null) {
      injector.injectMembers(processor);
    }
    return processor;
  }

  @Override
  public List<ExpressionProcessor> getExpressionProcessors(ExecutionContext context) {
    List<ExpressionProcessor> processorList =
        Lists.newArrayList(new ServiceExpressionProcessor(context), new HostExpressionProcessor(context));
    for (ExpressionProcessor processor : processorList) {
      injector.injectMembers(processor);
    }
    return processorList;
  }
}
