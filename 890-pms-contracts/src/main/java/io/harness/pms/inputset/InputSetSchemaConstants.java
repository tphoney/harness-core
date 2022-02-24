package io.harness.pms.inputset;

public class InputSetSchemaConstants {
  public static final String PIPELINE_ID_FOR_INPUT_SET_PARAM_MESSAGE = "Pipeline Identifier for the entity.";
  public static final String RUNTIME_INPUT_TEMPLATE_MESSAGE = "Runtime Input template for the Pipeline";
  public static final String INPUT_SET_ID_MESSAGE = "Input Set Identifier";
  public static final String INPUT_SET_NAME_MESSAGE = "Input Set Name";
  public static final String INPUT_SET_YAML_MESSAGE = "Input Set YAML";
  public static final String OVERLAY_INPUT_SET_YAML_MESSAGE = "Overlay Input Set YAML";
  public static final String OVERLAY_INPUT_SET_REFERENCES_MESSAGE = "Input Set References in the Overlay Input Set";
  public static final String INPUT_SET_DESCRIPTION_MESSAGE = "Input Set description";
  public static final String INPUT_SET_TYPE_MESSAGE =
      "Type of Input Set needed: \"INPUT_SET\", or \"OVERLAY_INPUT_SET\", or \"ALL\". If nothing is sent, ALL is considered.";
  public static final String INPUT_SET_TAGS_MESSAGE = "Input Set tags";
  public static final String INPUT_SET_OUTDATED_MESSAGE =
      "This field is true if a Pipeline update has made this Input Set invalid, and cannot be used for Pipeline Execution";
  public static final String INPUT_SET_ERROR_MESSAGE =
      "This field is true if an Input Set had errors and hence could not be saved";
  public static final String OVERLAY_INPUT_SET_ERROR_MESSAGE =
      "This field is true if an Overlay Input Set had errors and hence could not be saved";
  public static final String OVERLAY_INPUT_SET_ERROR_MAP_MESSAGE =
      "This contains the invalid references in the Overlay Input Set, along with a message why they are invalid";
  public static final String INPUT_SET_ERROR_WRAPPER_MESSAGE =
      "This contains the error response if the Input Set save failed";
  public static final String INPUT_SET_ERROR_PIPELINE_YAML_MESSAGE =
      "If an Input Set save fails, this field contains the error fields, with the field values replaced with a UUID";
  public static final String INPUT_SET_UUID_TO_ERROR_YAML_MESSAGE =
      "If an Input Set save fails, this field contains the map from FQN to why that FQN threw an error";
  public static final String INPUT_SET_VERSION_MESSAGE = "The version of the Input Set";
  public static final String INPUT_SET_MODULES_MESSAGE = "Modules in which the Pipeline belongs";
  public static final String INPUT_SET_REPLACED_EXPRESSIONS_MESSAGE =
      "List of Expressions that need to be replaced for running selected Stages. Empty if the full Pipeline is being run or no expressions need to be replaced";
}
