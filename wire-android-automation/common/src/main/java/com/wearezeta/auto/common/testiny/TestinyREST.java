package com.wearezeta.auto.common.testiny;

import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.rest.CommonRESTHandlers;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * https://www.testiny.io/docs/api-quickstart/
 */
class TestinyREST {
    private static final Logger log = ZetaLogger.getLog(TestinyREST.class.getSimpleName());

    private static final int MAX_REQUEST_RETRY_COUNT = 2;

    private static final String testinyAPIKey = Credentials.get("TESTINY_API_KEY");

    private static final CommonRESTHandlers restHandlers = new CommonRESTHandlers(
            TestinyREST::verifyRequestResult, MAX_REQUEST_RETRY_COUNT);

    private static String getBaseURI() {
        return "https://app.testiny.io/api/v1";
    }

    private static void verifyRequestResult(int currentResponseCode,
                                            int[] acceptableResponseCodes, String message)
            throws TestinyRequestException {
        if (!ArrayUtils.contains(acceptableResponseCodes, currentResponseCode)) {
            throw new TestinyRequestException(
                    String.format("Testiny request failed. Request return code is: %d. Expected codes are: %s." +
                                    " Message from service is: %s", currentResponseCode,
                            Arrays.toString(acceptableResponseCodes), message),
                    currentResponseCode);
        }
    }

    private static final Timedelta CONNECT_TIMEOUT = Timedelta.ofSeconds(5);
    private static final Timedelta READ_TIMEOUT = Timedelta.ofSeconds(15);
    private static final Client client;

    static {
        final ClientConfig configuration = new ClientConfig();
        configuration.property(ClientProperties.CONNECT_TIMEOUT, (int) CONNECT_TIMEOUT.asMillis());
        configuration.property(ClientProperties.READ_TIMEOUT, (int) READ_TIMEOUT.asMillis());

        client = ClientBuilder.newBuilder().withConfig(configuration).build();
    }

    private static Invocation.Builder buildRequest(String restAction) {
        final String dstUrl = String.format("%s/%s", getBaseURI(), restAction);
        log.fine(String.format("Making request to %s...", dstUrl));
        return client
                .target(dstUrl)
                .request()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .header("X-Api-Key", testinyAPIKey);
    }

    static JSONObject getOpenTestRun(long projectId, String runName) {
        final Invocation.Builder webResource = buildRequest("testrun/find");
        final JSONObject requestBody = new JSONObject();
        final JSONObject filter = new JSONObject();

        filter.put("is_closed", false);
        filter.put("project_id", projectId);
        filter.put("title", runName);
        requestBody.put("filter", filter);

        final String output = restHandlers.httpPost(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }

    static JSONObject getNewIds(List<String> oldIds, long projectId) {
        final Invocation.Builder webResource = buildRequest("testcase/find");
        final JSONObject requestBody = new JSONObject();
        final JSONArray ids = new JSONArray();
        final JSONObject filter = new JSONObject();

        oldIds.forEach(ids::put);
        filter.put("cf__old_id", ids);
        filter.put("project_id", projectId);
        requestBody.put("filter", filter);
        requestBody.put("idOnly", true);

        final String output = restHandlers.httpPost(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }

    static String getTestrunDescription(long testRunId) {
        final Invocation.Builder webResource = buildRequest(String.format("testrun/%d", testRunId));
        final String output = restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK});
        return new JSONObject(output).getString("description");
    }

    static void updateTestRunDescription(long testRunId, String description) {
        final Invocation.Builder webResource = buildRequest(String.format("testrun/%d?force=true", testRunId));
        final JSONObject requestBody = new JSONObject();
        requestBody.put("description", description);
        restHandlers.httpPut(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK});
    }

    static void addTestCaseResult(long testRunId, long caseId, String status) {
        final Invocation.Builder webResource = buildRequest("testrun/mapping/bulk/testcase:testrun?op=add_or_update");
        final JSONArray bulk = new JSONArray();
        final JSONObject requestBody = new JSONObject();
        final JSONObject ids = new JSONObject();

        ids.put("testcase_id", caseId);
        ids.put("testrun_id", testRunId);
        requestBody.put("ids", ids);
        final JSONObject mapped = new JSONObject();
        mapped.put("result_status", status);
        mapped.put("assigned_to", "OWNER");
        requestBody.put("mapped", mapped);
        bulk.put(requestBody);

        restHandlers.httpPost(webResource, bulk.toString(), new int[]{HttpStatus.SC_OK});
    }

    static void bulkAddTestCaseResults(long testRunId, List<Long> caseIds, String status) {
        final Invocation.Builder webResource = buildRequest("testrun/mapping/bulk/testcase:testrun?op=add_or_update");
        final JSONArray bulk = new JSONArray();

        for (long caseId : caseIds) {
            final JSONObject requestBody = new JSONObject();
            final JSONObject ids = new JSONObject();
            final JSONObject mapped = new JSONObject();

            ids.put("testcase_id", caseId);
            ids.put("testrun_id", testRunId);
            mapped.put("result_status", status);
            mapped.put("assigned_to", "OWNER");

            requestBody.put("ids", ids);
            requestBody.put("mapped", mapped);
            bulk.put(requestBody);
        }
        restHandlers.httpPost(webResource, bulk.toString(), new int[]{HttpStatus.SC_OK});
    }

    static JSONObject createCommentEntity(String comment, long projectId) {
        final Invocation.Builder webResource = buildRequest("comment");
        final JSONObject requestBody = new JSONObject();

        requestBody.put("type", "TEXT");
        requestBody.put("text", comment);
        requestBody.put("project_id", projectId);

        final String output = restHandlers.httpPost(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK});
        return new JSONObject(output);
    }

    static JSONObject createTestRun(String title, long projectId) {
        return createTestRun(title, projectId, 0);
    }

    static JSONObject createTestRun(String title, long projectId, long testplan) {
        final Invocation.Builder webResource = buildRequest("testrun");
        final JSONObject requestBody = new JSONObject();

        requestBody.put("id", 1); // The ID doesn't seem to actually do anything, but request fails without it
        requestBody.put("title", title);
        requestBody.put("is_deleted", false);
        requestBody.put("project_id", projectId);
        requestBody.put("testplan_id", testplan);
        requestBody.put("is_closed", false);
        requestBody.put("description", "");

        return new JSONObject(restHandlers.httpPost(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK}));
    }

    static void addCommentToTestCaseInTestRun(long testCaseId, long testRunId, long commentId) {
        final Invocation.Builder webResource = buildRequest("comment/mapping/bulk/testcase:testrun?op=add");
        final JSONArray bulk = new JSONArray();
        final JSONObject requestBody = new JSONObject();
        final JSONObject ids = new JSONObject();

        ids.put("testcase_id", testCaseId);
        ids.put("testrun_id", testRunId);
        ids.put("comment_id", commentId);
        requestBody.put("ids", ids);
        bulk.put(requestBody);

        restHandlers.httpPost(webResource, bulk.toString(), new int[]{HttpStatus.SC_OK});
    }

    static void bulkAddCommentToTestCaseInTestRun(List<Long> testCaseIds, long testRunId, long commentId) {
        final Invocation.Builder webResource = buildRequest("comment/mapping/bulk/testcase:testrun?op=add");
        final JSONArray bulk = new JSONArray();

        for (long testCaseId : testCaseIds) {
            final JSONObject requestBody = new JSONObject();
            final JSONObject ids = new JSONObject();
            ids.put("testcase_id", testCaseId);
            ids.put("testrun_id", testRunId);
            ids.put("comment_id", commentId);
            requestBody.put("ids", ids);
            bulk.put(requestBody);
        }
        restHandlers.httpPost(webResource, bulk.toString(), new int[]{HttpStatus.SC_OK});
    }

    static void updateCase(long id, JSONObject newProperties) {
        final Invocation.Builder webResource = buildRequest(String.format("testcase/%d?force=true", id));
        restHandlers.httpPut(webResource, newProperties.toString(), new int[]{HttpStatus.SC_OK});
    }

    static void bulkUpdateCases(JSONArray newProperties) {
        final Invocation.Builder webResource = buildRequest("testcase/bulk?force=true");
        restHandlers.httpPut(webResource, newProperties.toString(), new int[]{HttpStatus.SC_OK});
    }
}
