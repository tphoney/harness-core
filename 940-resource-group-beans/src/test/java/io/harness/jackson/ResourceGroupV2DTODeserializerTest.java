/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.jackson;

import static io.harness.rule.OwnerRule.REETIKA;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.resourcegroup.remote.dto.ResourceGroupV2DTO;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class ResourceGroupV2DTODeserializerTest extends CategoryTest {
  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    NGObjectMapperHelper.configureNGObjectMapper(objectMapper);
  }

  public static String readFileAsString(String file) {
    try {
      return new String(Files.readAllBytes(Paths.get(file)));
    } catch (Exception ex) {
      Assert.fail("Failed reading the json from " + file + " with error " + ex.getMessage());
      return "";
    }
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void testDeserializationOfAccountResourceGroup() {
    String accountLevelResourceGroup =
        readFileAsString("940-resource-group-beans/src/test/resources/resourcegroups/accountResourceGroupV2.json");

    try {
      objectMapper.readValue(accountLevelResourceGroup, ResourceGroupV2DTO.class);
    } catch (Exception ex) {
      Assert.fail("Encountered exception while deserializing resource group " + ex.getMessage());
    }
  }
}
