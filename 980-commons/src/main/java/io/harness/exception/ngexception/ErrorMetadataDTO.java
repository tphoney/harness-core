/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception.ngexception;

import io.harness.exception.ngexception.beans.SampleErrorMetadataDTO;
import io.harness.exception.ngexception.beans.ScmErrorMetadataDTO;
import io.harness.exception.ngexception.beans.templateservice.TemplateInputsErrorMetadataDTO;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SampleErrorMetadataDTO.class, name = "Sample")
      , @JsonSubTypes.Type(value = TemplateInputsErrorMetadataDTO.class, name = "TemplateInputsErrorMetadata"),
          @JsonSubTypes.Type(value = ScmErrorMetadataDTO.class, name = "ScmErrorMetadataDTO")
    })
@Schema(name = "ErrorMetadata", description = "This implements different error meta data objects")
public interface ErrorMetadataDTO {
  String getType();
}
