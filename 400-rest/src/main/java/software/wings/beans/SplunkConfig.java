/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.base.Preconditions;
import io.harness.delegate.beans.connector.cv.Connector;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import io.harness.expression.ExpressionEvaluator;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.security.UsageRestrictions;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;
import software.wings.yaml.setting.VerificationProviderYaml;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonTypeName("SPLUNK")
@Data
@Builder
@ToString(exclude = "password")
@EqualsAndHashCode(callSuper = false)
public class SplunkConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander, Connector {
  @Attributes(title = "URL", required = true) @NotEmpty private String splunkUrl;

  @NotEmpty @Attributes(title = "User Name", required = true) private String username;

  @Attributes(title = "Password", required = true) @Encrypted(fieldName = "password") private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new Splunk config.
   */
  public SplunkConfig() {
    super(SettingVariableTypes.SPLUNK.name());
  }

  private SplunkConfig(String splunkUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.splunkUrl = splunkUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(splunkUrl, maskingEvaluator));
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  @Override
  @JsonIgnore
  public String getBaseUrl() {
    return splunkUrl;
  }

  @Override
  @JsonIgnore
  public Map<String, String> collectionHeaders() {
    Preconditions.checkState(isDecrypted(), "Should be decrypted to use this");
    String usernameColonPassword = username + ":" + getPassword();
    String auth =
        "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes(Charset.forName("UTF-8")));
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", auth);
    return headers;
  }

  @Override
  @JsonIgnore
  public Map<String, String> collectionParams() {
    return new HashMap<>();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends VerificationProviderYaml {
    private String splunkUrl;
    private String username;
    private String password;

    @Builder
    public Yaml(String type, String harnessApiVersion, String splunkUrl, String username, String password,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.splunkUrl = splunkUrl;
      this.username = username;
      this.password = password;
    }
  }
}
