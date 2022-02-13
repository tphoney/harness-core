/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

public class NestedExceptionUtils {
  public static WingsException hintWithExplanationException(
      String hintMessage, String explanationMessage, Throwable cause) {
    return new HintException(hintMessage, new ExplanationException(explanationMessage, cause));
  }

  public static WingsException hintWithExplanationAndCommandException(
      String hintMessage, String explanationMessage, String commandExecuted, Throwable cause) {
    return new HintException(
        hintMessage, new ExplanationException(explanationMessage, new ExplanationException(commandExecuted, cause)));
  }
}
