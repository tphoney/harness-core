package software.wings.delegatetasks.cv.utils;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import io.harness.concurrent.HTimeLimiter;
import software.wings.delegatetasks.cv.client.VerificationServiceClient;
import io.harness.rest.RestResponse;
import software.wings.delegatetasks.cv.beans.NewRelicMetricDataRecord;

import java.time.Duration;
import java.util.List;

import static io.harness.network.SafeHttpCall.execute;

public class MetricDataStoreUtil {

    @Inject
    private VerificationServiceClient verificationClient;
    @Inject private TimeLimiter timeLimiter;

    public boolean saveMetricData(String accountId, String applicationId, String stateExecutionId,
                                  String delegateTaskId, List<NewRelicMetricDataRecord> metricData) throws Exception {
        if (metricData.isEmpty()) {
            return true;
        }

        RestResponse<Boolean> restResponse = HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofSeconds(15),
                ()
                        -> execute(verificationClient.saveTimeSeriesMetrics(
                        accountId, applicationId, stateExecutionId, delegateTaskId, metricData)));
        return restResponse.getResource();
    }
}
