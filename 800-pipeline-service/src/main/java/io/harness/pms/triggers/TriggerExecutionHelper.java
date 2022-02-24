/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.triggers;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.ngtriggers.Constants.EVENT_CORRELATION_ID;
import static io.harness.ngtriggers.Constants.GIT_USER;
import static io.harness.ngtriggers.Constants.PR;
import static io.harness.ngtriggers.Constants.PUSH;
import static io.harness.ngtriggers.Constants.TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER;
import static io.harness.ngtriggers.Constants.TRIGGER_REF;
import static io.harness.ngtriggers.Constants.TRIGGER_REF_DELIMITER;
import static io.harness.pms.contracts.plan.TriggerType.WEBHOOK;
import static io.harness.pms.contracts.plan.TriggerType.WEBHOOK_CUSTOM;
import static io.harness.pms.plan.execution.PlanExecutionInterruptType.ABORTALL;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.TriggerException;
import io.harness.execution.PlanExecution;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAware;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.IssuedBy;
import io.harness.pms.contracts.interrupts.TriggerIssuer;
import io.harness.pms.contracts.plan.TriggerType;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.triggers.ParsedPayload;
import io.harness.pms.contracts.triggers.SourceType;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.contracts.triggers.Type;
import io.harness.pms.plan.execution.PipelineExecutor;
import io.harness.pms.plan.execution.beans.TriggerFlowPlanDetails;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.product.ci.scm.proto.PushHook;
import io.harness.product.ci.scm.proto.User;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;
import io.harness.serializer.ProtoUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class TriggerExecutionHelper {
  private final PlanExecutionService planExecutionService;
  private final PMSExecutionService pmsExecutionService;
  private final PipelineExecutor pipelineExecutor;

  public PlanExecution resolveRuntimeInputAndSubmitExecutionRequest(
      TriggerDetails triggerDetails, TriggerPayload triggerPayload) {
    return resolveRuntimeInputAndSubmitExecutionRequest(triggerDetails, triggerPayload, null, null);
  }

  public PlanExecution resolveRuntimeInputAndSubmitExecutionRequest(TriggerDetails triggerDetails,
      TriggerPayload triggerPayload, TriggerWebhookEvent triggerWebhookEvent, String payload) {
    String executionTagForGitEvent = generateExecutionTagForEvent(triggerDetails, triggerPayload);
    String eventId = triggerWebhookEvent != null ? triggerWebhookEvent.getUuid() : null;

    TriggeredBy embeddedUser =
        generateTriggerdBy(executionTagForGitEvent, triggerDetails.getNgTriggerEntity(), triggerPayload, eventId);
    TriggerType triggerType = findTriggerType(triggerPayload);

    return createPlanExecution(
        triggerDetails, triggerPayload, payload, executionTagForGitEvent, triggerType, embeddedUser);
  }

  private PlanExecution createPlanExecution(TriggerDetails triggerDetails, TriggerPayload triggerPayload,
      String payload, String executionTagForGitEvent, TriggerType triggerType, TriggeredBy triggeredBy) {
    try {
      NGTriggerEntity ngTriggerEntity = triggerDetails.getNgTriggerEntity();
      String accountId = ngTriggerEntity.getAccountId();
      String orgIdentifier = ngTriggerEntity.getOrgIdentifier();
      String projectIdentifier = ngTriggerEntity.getProjectIdentifier();
      String pipelineIdentifier = ngTriggerEntity.getTargetIdentifier();
      String runtimeInputYaml = triggerDetails.getNgTriggerConfigV2().getInputYaml();

      TriggerFlowPlanDetails triggerFlowPlanDetails = TriggerFlowPlanDetails.builder()
                                                          .payload(payload)
                                                          .triggerPayload(triggerPayload)
                                                          .triggeredBy(triggeredBy)
                                                          .triggerType(triggerType)
                                                          .build();

      SecurityContextBuilder.setContext(new ServicePrincipal(PIPELINE_SERVICE.getServiceId()));
      PlanExecution planExecution =
          pipelineExecutor
              .startPlanExecution(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, null, "",
                  runtimeInputYaml, Collections.emptyList(), Collections.emptyMap(), false, triggerFlowPlanDetails)
              .getPlanExecution();
      requestPipelineExecutionAbortForSameExecTagIfNeeded(triggerDetails, planExecution, executionTagForGitEvent);
      return planExecution;
    } catch (Exception e) {
      throw new TriggerException(
          "Failed while requesting Pipeline Execution through Trigger: " + e.getMessage(), e, USER);
    }
  }

  @VisibleForTesting
  TriggeredBy generateTriggerdBy(
      String executionTagForGitEvent, NGTriggerEntity ngTriggerEntity, TriggerPayload triggerPayload, String eventId) {
    TriggeredBy.Builder builder =
        TriggeredBy.newBuilder().setIdentifier(ngTriggerEntity.getIdentifier()).setUuid("systemUser");
    if (isNotBlank(executionTagForGitEvent)) {
      builder.putExtraInfo(PlanExecution.EXEC_TAG_SET_BY_TRIGGER, executionTagForGitEvent);
      builder.putExtraInfo(TRIGGER_REF, generateTriggerRef(ngTriggerEntity));

      if (isNotBlank(eventId)) {
        builder.putExtraInfo(EVENT_CORRELATION_ID, eventId);
      }

      if (triggerPayload.hasParsedPayload()) {
        ParsedPayload parsedPayload = triggerPayload.getParsedPayload();
        User sender = null;
        if (parsedPayload.hasPush()) {
          sender = parsedPayload.getPush().getSender();
        } else if (parsedPayload.hasPr()) {
          sender = parsedPayload.getPr().getSender();
        }

        if (sender != null) {
          builder.putExtraInfo(GIT_USER, sender.getLogin());
          if (isNotEmpty(sender.getEmail())) {
            builder.putExtraInfo("email", sender.getEmail());
          }
          if (isNotEmpty(sender.getLogin())) {
            builder.setIdentifier(sender.getLogin());
          }
          if (isNotEmpty(sender.getName())) {
            builder.setUuid(sender.getName());
          }
        }
      }
    }
    return builder.build();
  }

  @VisibleForTesting
  String generateTriggerRef(NGTriggerEntity ngTriggerEntity) {
    return new StringBuilder(256)
        .append(ngTriggerEntity.getAccountId())
        .append(TRIGGER_REF_DELIMITER)
        .append(ngTriggerEntity.getOrgIdentifier())
        .append(TRIGGER_REF_DELIMITER)
        .append(ngTriggerEntity.getProjectIdentifier())
        .append(TRIGGER_REF_DELIMITER)
        .append(ngTriggerEntity.getIdentifier())
        .toString();
  }

  @VisibleForTesting
  TriggerType findTriggerType(TriggerPayload triggerPayload) {
    TriggerType triggerType = WEBHOOK;
    if (triggerPayload.getType() == Type.SCHEDULED) {
      triggerType = TriggerType.SCHEDULER_CRON;
    } else if (triggerPayload.getSourceType() == SourceType.CUSTOM_REPO) {
      triggerType = WEBHOOK_CUSTOM;
    }

    return triggerType;
  }

  /**
   * Generate execution tag to identify pipeline executions caused by similar trigger git events.
   * PR: {accId:orgId:projectId:pipelineIdentifier:PR:RepoUrl:PrNum:SourceBranch:TargetBranch}
   * PUSH: {accId:orgId:projectId:pipelineIdentifier:PUSH:RepoUrl:Ref}
   *
   * @param triggerDetails
   * @param triggerPayload
   * @return
   */
  public String generateExecutionTagForEvent(TriggerDetails triggerDetails, TriggerPayload triggerPayload) {
    String triggerRef = new StringBuilder(256)
                            .append(triggerDetails.getNgTriggerEntity().getAccountId())
                            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
                            .append(triggerDetails.getNgTriggerEntity().getOrgIdentifier())
                            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
                            .append(triggerDetails.getNgTriggerEntity().getProjectIdentifier())
                            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
                            .append(triggerDetails.getNgTriggerEntity().getTargetIdentifier())
                            .toString();

    try {
      if (!triggerPayload.hasParsedPayload()) {
        return triggerRef;
      }

      ParsedPayload parsedPayload = triggerPayload.getParsedPayload();
      StringBuilder executionTag = new StringBuilder(512).append(triggerRef);

      if (parsedPayload.hasPr()) {
        executionTag.append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER).append(PR);
        PullRequestHook pullRequestHook = parsedPayload.getPr();
        PullRequest pr = pullRequestHook.getPr();
        executionTag.append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pullRequestHook.getRepo().getLink())
            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pr.getNumber())
            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pr.getSource())
            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pr.getTarget());
      } else if (parsedPayload.hasPush()) {
        executionTag.append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER).append(PUSH);
        PushHook pushHook = parsedPayload.getPush();
        executionTag.append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pushHook.getRepo().getLink())
            .append(TRIGGER_EXECUTION_TAG_TAG_VALUE_DELIMITER)
            .append(pushHook.getRef());
      }
      return executionTag.toString();
    } catch (Exception e) {
      log.error("failed to generate complete Execution Tag for Trigger: " + triggerRef, e);
    }

    return triggerRef;
  }

  @VisibleForTesting
  void requestPipelineExecutionAbortForSameExecTagIfNeeded(
      TriggerDetails triggerDetails, PlanExecution planExecution, String executionTag) {
    try {
      if (!isAutoAbortSelected(triggerDetails.getNgTriggerConfigV2())) {
        return;
      }

      List<PlanExecution> executionsToAbort =
          planExecutionService.findPrevUnTerminatedPlanExecutionsByExecutionTag(planExecution, executionTag);
      if (isEmpty(executionsToAbort)) {
        return;
      }

      for (PlanExecution execution : executionsToAbort) {
        registerPipelineExecutionAbortInterrupt(execution, executionTag, triggerDetails.getNgTriggerEntity());
      }
    } catch (Exception e) {
      log.error("Failed while requesting abort for pipeline executions using executionTag: " + executionTag, e);
    }
  }

  @VisibleForTesting
  boolean isAutoAbortSelected(NGTriggerConfigV2 ngTriggerConfigV2) {
    boolean autoAbortPreviousExecutions = false;
    if (WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerConfigV2.getSource().getSpec().getClass())) {
      WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerConfigV2.getSource().getSpec();
      GitAware gitAware = webhookTriggerConfigV2.getSpec().fetchGitAware();
      if (gitAware != null && gitAware.fetchAutoAbortPreviousExecutions()) {
        autoAbortPreviousExecutions = gitAware.fetchAutoAbortPreviousExecutions();
      }
    }

    return autoAbortPreviousExecutions;
  }

  private void registerPipelineExecutionAbortInterrupt(
      PlanExecution execution, String executionTag, NGTriggerEntity ngTriggerEntity) {
    try {
      log.info(new StringBuilder(128)
                   .append("Requesting Pipeline Execution Abort for planExecutionId")
                   .append(execution.getUuid())
                   .append(", with Tag: ")
                   .append(executionTag)
                   .toString());

      InterruptConfig interruptConfig =
          InterruptConfig.newBuilder()
              .setIssuedBy(IssuedBy.newBuilder()
                               .setTriggerIssuer(TriggerIssuer.newBuilder()
                                                     .setTriggerRef(generateTriggerRef(ngTriggerEntity))
                                                     .setAbortPrevConcurrentExecution(true)
                                                     .build())
                               .setIssueTime(ProtoUtils.unixMillisToTimestamp(System.currentTimeMillis()))
                               .build())
              .build();
      pmsExecutionService.registerInterrupt(ABORTALL, execution.getUuid(), null, interruptConfig);
    } catch (Exception e) {
      log.error("Exception white requesting Pipeline Execution Abort: " + executionTag, e);
    }
  }
}
