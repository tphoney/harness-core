/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.yaml.extended;

import io.harness.encryption.SecretRefData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName("Secret")
@TypeAlias("customSecretVariable")
public class CustomSecretVariable implements CustomVariable {
  @Builder.Default @NotNull Type type = Type.SECRET;
  @NotNull String name;
  @NotNull SecretRefData value;
}
