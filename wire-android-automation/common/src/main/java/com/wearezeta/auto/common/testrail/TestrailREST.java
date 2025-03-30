package com.wearezeta.auto.common.testrail;

import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.rest.CommonRESTHandlers;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import java.util.logging.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * http://docs.gurock.com/testrail-api2/start
 */
class TestrailREST {
    private static final Logger log = ZetaLogger.getLog(TestrailREST.class.getSimpleName());

    private static final int MAX_REQUEST_RETRY_COUNT = 2;

    private static final CommonRESTHandlers restHandlers = new CommonRESTHandlers(
            TestrailREST::verifyRequestResult, MAX_REQUEST_RETRY_COUNT);

    private static String getBaseURI() {
        final String host = Config.common().getTestrailServerUrl(TestrailREST.class);
        return String.format("%s/index.php?/api/v2", host);
    }

    private static String getTestrailUser() {
        return Config.current().getTestrailUsername(TestrailREST.class);
    }

    private static String getTestrailToken() {
        return Config.current().getTestrailToken(TestrailREST.class);
    }

    private static void verifyRequestResult(int currentResponseCode,
                                            int[] acceptableResponseCodes, String message)
            throws TestrailRequestException {
        if (!ArrayUtils.contains(acceptableResponseCodes, currentResponseCode)) {
            throw new TestrailRequestException(
                    String.format("Testrail request failed. Request return code is: %d. Expected codes are: %s." +
                                    " Message from service is: %s", currentResponseCode,
                            Arrays.toString(acceptableResponseCodes), message),
                    currentResponseCode);
        }
    }

    private static final Timedelta CONNECT_TIMEOUT = Timedelta.ofSeconds(5);
    private static final Timedelta READ_TIMEOUT = Timedelta.ofSeconds(15);
    private static final Client client;

    static TrustManager[] trustAllCerts = new X509TrustManager[] { new X509TrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    } };

    static HostnameVerifier allHostsValid = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    static {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        final ClientConfig configuration = new ClientConfig();
        configuration.property(ClientProperties.CONNECT_TIMEOUT, (int) CONNECT_TIMEOUT.asMillis());
        configuration.property(ClientProperties.READ_TIMEOUT, (int) READ_TIMEOUT.asMillis());
        try {
            sslContext.init(null, trustAllCerts, null);
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        ClientBuilder builder = ClientBuilder.newBuilder().sslContext(sslContext).hostnameVerifier(allHostsValid);
        client = builder.withConfig(configuration).build();
    }

    private static Invocation.Builder buildRequest(String restAction) {
        final String dstUrl = String.format("%s/%s", getBaseURI(), restAction);
        log.fine(String.format("Making request to %s...", dstUrl));
        return client
                .target(dstUrl)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Basic %s",
                        Base64.getEncoder().encodeToString(
                                String.format("%s:%s", getTestrailUser(), getTestrailToken()).getBytes())
                        )
                );
    }

    static JSONObject getProjects() {
        final Invocation.Builder webResource = buildRequest("get_projects");
        final String output = restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }

    static JSONObject getTestPlans(long projectId) {
        final Invocation.Builder webResource = buildRequest(String.format("get_plans/%s", projectId));
        final String output = restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }

    static JSONObject getTestPlan(long testPlanId) {
        final Invocation.Builder webResource = buildRequest(String.format("get_plan/%s", testPlanId));
        final String output = restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }

    static JSONObject addTestCaseResult(long testRunId, long caseId,
                                        int statusId, Optional<String> comment) {
        final Invocation.Builder webResource = buildRequest(String.format("add_result_for_case/%s/%s", testRunId,
                caseId));
        final JSONObject requestBody = new JSONObject();
        requestBody.put("status_id", statusId);
        comment.ifPresent(x -> requestBody.put("comment", x));
        final String output = restHandlers.httpPost(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }

    static JSONObject getTestCaseResults(long testRunId, long caseId) {
        final Invocation.Builder webResource = buildRequest(String.format("get_results_for_case/%s/%s", testRunId,
                caseId));
        final String output = restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }

    static JSONObject updateCase(long caseId, JSONObject newProperties) {
        final Invocation.Builder webResource = buildRequest(String.format("update_case/%s", caseId));
        final String output = restHandlers.httpPost(webResource, newProperties.toString(), new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }
}
