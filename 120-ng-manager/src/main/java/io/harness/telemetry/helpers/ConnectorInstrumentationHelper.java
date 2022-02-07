package io.harness.telemetry.helpers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.data.structure.EmptyPredicate;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class ConnectorInstrumentationHelper {
    @Inject
    TelemetryReporter telemetryReporter;
    public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
    String ACCOUNT_ID = "account_id";
    String CONNECTOR_ID = "connector_id";
    String CONNECTOR_PROJECT = "connector_project";
    String CONNECTOR_ORG = "connector_org";
    String CONNECTOR_NAME = "connector_name";
    String CONNECTOR_TYPE = "connector_type";

    public void sendConnectorCreationFinishedEvent(ConnectorInfoDTO connector, String accountId) {
        log.info("Platform SendConnectorCreationFinishedEvent execution started.");
        try {
            if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(ACCOUNT_ID, accountId);
                map.put(CONNECTOR_ID, connector.getIdentifier());
                map.put(CONNECTOR_TYPE, connector.getConnectorType());
                map.put(CONNECTOR_NAME, connector.getName());
                map.put(CONNECTOR_ORG, connector.getOrgIdentifier());
                map.put(CONNECTOR_PROJECT, connector.getProjectIdentifier());
                telemetryReporter.sendTrackEvent("Connector Creation Finished", map,
                        ImmutableMap.<Destination, Boolean>builder()
                                .put(Destination.AMPLITUDE, true)
                                .put(Destination.ALL, false)
                                .build(),
                        Category.COMMUNITY
                );
                log.info("Connector Creation Finished event sent!");
            } else {
                log.info("There is no Account found!. Can not send Connector Creation Finished event.");
            }
        } catch (Exception e) {
            log.error("Platform SendConnectorCreationFinishedEvent execution failed.", e);
        } finally {
            log.info("Platform SendConnectorCreationFinishedEvent execution finished.");
        }
    }

    public void sendConnectorDeletionEvent(String orgIdentifier, String projectIdentifier, String connectorIdentifier,
                                           String accountId) {
        log.info("Platform SendConnectorDeletionEvent execution started.");
        try {
            if (EmptyPredicate.isNotEmpty(accountId) || !accountId.equals(GLOBAL_ACCOUNT_ID)) {
                HashMap<String, Object> map = new HashMap<>();
                map.put(CONNECTOR_PROJECT, projectIdentifier);
                map.put(CONNECTOR_ID, connectorIdentifier);
                map.put(CONNECTOR_ORG, orgIdentifier);
                telemetryReporter.sendTrackEvent("Connector Deletion", map,
                        ImmutableMap.<Destination, Boolean>builder()
                                .put(Destination.AMPLITUDE, true)
                                .put(Destination.ALL, false)
                                .build(),
                        Category.COMMUNITY
                );
                log.info("Connector deletion event sent!");
            } else {
                log.info("There is no Account found!. Can not send Connector Deletion event.");
            }
        } catch (Exception e) {
            log.error("Platform SendConnectorDeletionEvent execution failed.", e);
        } finally {
            log.info("Platform SendConnectorDeletionEvent execution finished.");
        }
    }
}
