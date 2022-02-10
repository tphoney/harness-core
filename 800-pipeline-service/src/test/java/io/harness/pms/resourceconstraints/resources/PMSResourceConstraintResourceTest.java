package io.harness.pms.resourceconstraints.resources;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.resourceconstraints.response.ResourceConstraintExecutionInfoDTO;
import io.harness.pms.resourceconstraints.service.PMSResourceConstraintService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class PMSResourceConstraintResourceTest {
  @InjectMocks PMSResourceConstraintResource resourceConstraintResource;
  @Mock PMSResourceConstraintService resourceConstraintService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetResourceConstraintsExecutionInfo() {
    ResourceConstraintExecutionInfoDTO resourceConstraintExecutionInfo =
        ResourceConstraintExecutionInfoDTO.builder().capacity(12).name("name").build();
    doReturn(resourceConstraintExecutionInfo)
        .when(resourceConstraintService)
        .getResourceConstraintExecutionInfo("acc", "unit");
    ResponseDTO<ResourceConstraintExecutionInfoDTO> resourceConstraintsExecutionInfo =
        resourceConstraintResource.getResourceConstraintsExecutionInfo("acc", "unit");
    assertThat(resourceConstraintsExecutionInfo.getData()).isEqualTo(resourceConstraintExecutionInfo);
  }
}