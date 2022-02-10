package io.harness.pms.health;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.NoResultFoundException;
import io.harness.health.HealthException;
import io.harness.health.HealthService;
import io.harness.maintenance.MaintenanceController;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import com.codahale.metrics.health.HealthCheck;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class HealthResourceTest extends CategoryTest {
  @InjectMocks HealthResource healthResource;
  @Mock HealthService healthService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGet() throws Exception {
    MaintenanceController.forceMaintenance(true);
    assertThatThrownBy(() -> healthResource.get())
        .hasMessage("in maintenance mode")
        .isInstanceOf(NoResultFoundException.class);

    MaintenanceController.forceMaintenance(false);
    doReturn(HealthCheck.Result.builder().healthy().build()).when(healthService).check();
    RestResponse<String> healthyResponse = healthResource.get();
    assertThat(healthyResponse.getResource()).isEqualTo("healthy");

    HealthCheck.Result unhealthy = HealthCheck.Result.builder().unhealthy().withMessage("any").build();
    doReturn(unhealthy).when(healthService).check();
    assertThatThrownBy(() -> healthResource.get()).isInstanceOf(HealthException.class).hasMessage("HEALTH_ERROR");
  }
}