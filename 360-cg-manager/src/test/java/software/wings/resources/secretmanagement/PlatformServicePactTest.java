package software.wings.resources.secretmanagement;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junitsupport.Consumer;
import au.com.dius.pact.provider.junitsupport.Provider;

import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.target.Target;
import au.com.dius.pact.provider.junitsupport.target.TestTarget;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.harness.delegate.beans.DelegateToken;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.BeforeClass;
import junit.runner.Version;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(PactRunner.class) // Say JUnit to run tests with custom Runner
@Provider("CG-Manager") // Set up name of tested provider
@Consumer("Platform-Service")
@PactBroker(host = "localhost", port = "9292")
public class PlatformServicePactTest {

    @BeforeClass
    public static void enablePublishingPact() {
        System.setProperty("pact.verifier.publishResults", "true");
    }

    @TestTarget
    public final Target target = new HttpTarget("https", "localhost", 9090, "/", true);

    @State("Hitting /api/version - for fetching the versions")
    public void validVersion() {
        System.out.println(target);
    }

    @State("invalid property - /invalidurl")
    public void invalidProperty(){

    }

    @State("Delete test app")
    public Map<String, Object> testUser() throws Exception {
//        DelegateToken delegateToken = new DelegateToken();
//        delegateToken.setValue("abcde");
//        doReturn(delegateToken).when(persistence).createQuery(DelegateToken.class);
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            CloseableHttpClient httpclient = HttpClients
                    .custom()
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLContext(sc)
                    .build();
        HttpPost httpPost = new HttpPost("https://localhost:9090/api/apps?accountId=kmpySmUISimoRrJL6NL73w");
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", "test14i");
        jsonObject.addProperty("description", "");
        jsonObject.addProperty("accountId", "kmpySmUISimoRrJL6NL73w");
        StringEntity entity = new StringEntity(jsonObject.toString(),"application/json","UTF-8");
        httpPost.setEntity(entity);
        httpPost.addHeader("authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhY2NvdW50SWQiOiJrbXB5U21VSVNpbW9SckpMNk5MNzN3IiwiYXV0aFRva2VuIjoiQ3J1Nm90VjdnTG9UNkZiRlQ1UkRSamRtMGNUcmMyV1QiLCJpc3MiOiJIYXJuZXNzIEluYyIsIm5hbWUiOiJsdjBldVJoS1JDeWlYV3pTN3BPZzZnIiwidXNySWQiOiJsdjBldVJoS1JDeWlYV3pTN3BPZzZnIiwiZXhwIjoxNjQ0NDg0Mzg3LCJlbnYiOiIiLCJ0eXBlIjoiVVNFUiIsImlhdCI6MTY0NDM5Nzk4NywiZW1haWwiOiJhZG1pbkBoYXJuZXNzLmlvIiwidXNlcm5hbWUiOiJBZG1pbiJ9.RCCzA3hLykLVfXc84JCBGIuZlBsEOCZUKsrBitshijw");
        CloseableHttpResponse response = httpclient.execute(httpPost);
        HttpEntity entity1 = response.getEntity();
        String responseString = EntityUtils.toString(entity1, "UTF-8");
        JSONObject jsonObject1 = new JSONObject(responseString);
        JSONObject resource = jsonObject1.getJSONObject("resource");
        log.info("status log sumo exception: " + responseString);
        log.info("printing uuid: " + resource.get("uuid") );
        HashMap<String, Object> map = new HashMap<>();
        map.put("appId", "" );
        return map;
    }


}