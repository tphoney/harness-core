/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline;

public interface PipelineResourceConstants {
  String ACCOUNT_PARAM_MESSAGE = "Account Identifier for the entity.";
  String ORG_PARAM_MESSAGE = "Organization Identifier for the entity.";
  String PROJECT_PARAM_MESSAGE = "Project Identifier for the entity.";
  String PIPELINE_ID_PARAM_MESSAGE = "Pipeline Identifier";
  String STAGE_NODE_ID_PARAM_MESSAGE = "Stage Node Identifier to get execution stats.";
  String PIPELINE_ID_LIST_PARAM_MESSAGE = "Pipeline Identifier filter if exact pipelines needs to be filtered.";
  String PIPELINE_SEARCH_TERM_PARAM_MESSAGE =
      "Search term to filter out pipelines based on pipeline name, identifier, tags.";
  String INPUT_SET_SEARCH_TERM_PARAM_MESSAGE = "Search term to filter out Input Sets based on name, identifier, tags.";
  String IF_MATCH_PARAM_MESSAGE = "Version of entity to match";
  String MODULE_TYPE_PARAM_MESSAGE = "The module from which execution was triggered.";
  String ORIGINAL_EXECUTION_ID_PARAM_MESSAGE = "Id of the execution from which we are running";
  String USE_FQN_IF_ERROR_RESPONSE_ERROR_MESSAGE = "Use FQN in error response";
  String INPUT_SET_ID_PARAM_MESSAGE = "Identifier of the Input Set";
  String OVERLAY_INPUT_SET_ID_PARAM_MESSAGE = "Identifier of the Overlay Input Set";
  String CREATED_AT_MESSAGE = "Time at which the entity was created";
  String UPDATED_AT_MESSAGE = "Time at which the entity was last updated";
  String GIT_DETAILS_MESSAGE = "This contains the Git Details of the entity if the Project is Git enabled";
  String GIT_VALIDITY_MESSAGE = "This contains Validity Details of the Entity";

  String START_TIME_EPOCH_PARAM_MESSAGE = "Start Date Epoch time in ms";
  String END_TIME_EPOCH_PARAM_MESSAGE = "End Date Epoch time in ms";

  String PIPELINE_ID_FOR_INPUT_SET_PARAM_MESSAGE = "Pipeline Identifier for the entity.";
  String RUNTIME_INPUT_TEMPLATE_MESSAGE = "Runtime Input template for the Pipeline";
  String INPUT_SET_ID_MESSAGE = "Input Set Identifier";
  String INPUT_SET_NAME_MESSAGE = "Input Set Name";
  String INPUT_SET_YAML_MESSAGE = "Input Set YAML";
  String OVERLAY_INPUT_SET_YAML_MESSAGE = "Overlay Input Set YAML";
  String OVERLAY_INPUT_SET_REFERENCES_MESSAGE = "Input Set References in the Overlay Input Set";
  String INPUT_SET_DESCRIPTION_MESSAGE = "Input Set description";
  String INPUT_SET_TYPE_MESSAGE =
      "Type of Input Set needed: \"INPUT_SET\", or \"OVERLAY_INPUT_SET\", or \"ALL\". If nothing is sent, ALL is considered.";
  String INPUT_SET_TAGS_MESSAGE = "Input Set tags";
  String INPUT_SET_OUTDATED_MESSAGE =
      "This field is true if a Pipeline update has made this Input Set invalid, and cannot be used for Pipeline Execution";
  String INPUT_SET_ERROR_MESSAGE = "This field is true if an Input Set did not get saved";
  String OVERLAY_INPUT_SET_ERROR_MESSAGE = "This field is true if an Overlay Input Set did not get saved";
  String OVERLAY_INPUT_SET_ERROR_MAP_MESSAGE =
      "This contains the invalid references in the Overlay Input Set, along with a message why they are invalid";
  String INPUT_SET_ERROR_WRAPPER_MESSAGE = "This contains the error response if the Input Set save failed";
  String INPUT_SET_ERROR_PIPELINE_YAML_MESSAGE =
      "If an Input Set save fails, this field contains the error fields, with the field values replaced with a UUID";
  String INPUT_SET_UUID_TO_ERROR_YAML_MESSAGE =
      "If an Input Set save fails, this field contains the map from FQN to why that FQN threw an error";
  String INPUT_SET_VERSION_MESSAGE = "The version of the Input Set";
  String INPUT_SET_MODULES_MESSAGE = "Modules in which the Pipeline belongs";
  String INPUT_SET_REPLACED_EXPRESSIONS_MESSAGE = "List of Expressions that need to be replaced for running selected Stages. Empty if the full Pipeline is being run or no expressions need to be replaced";
}
