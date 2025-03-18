package com.wearezeta.auto.common.sso;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;

import com.wearezeta.auto.common.backend.HttpRequestException;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.rest.CommonRESTHandlers;
import org.apache.http.HttpStatus;
import java.util.logging.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;

public class OktaAPIClient {

    private static final Logger log = ZetaLogger.getLog(OktaAPIClient.class.getSimpleName());
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(240);
    private static final String BASEURI = "https://dev-500508-admin.oktapreview.com";
    private static final CommonRESTHandlers restHandlers = new CommonRESTHandlers(OktaAPIClient::verifyRequestResult, 3);
    private static String APIKEY;

    private String applicationId;
    private Set<String> userIds = new HashSet<>();
    private Client client;

    public OktaAPIClient() {
        final ClientConfig configuration = new ClientConfig();
        configuration.property(ClientProperties.CONNECT_TIMEOUT, (int) CONNECT_TIMEOUT.toMillis());
        configuration.property(ClientProperties.READ_TIMEOUT, (int) READ_TIMEOUT.toMillis());
        this.client = ClientBuilder.newClient(configuration);
    }

    private String getAPIKey() {
        // lazy load api key to not always ask for it on every run
        if (APIKEY == null) {
            APIKEY = Credentials.get("OKTA_API_KEY");
        }
        return APIKEY;
    }

    private Invocation.Builder buildRequest(String path) {
        final String target = String.format("%s/%s", BASEURI, path);
        log.fine(String.format("Making request to %s...", target));
        return client.target(target).request().accept(MediaType.APPLICATION_JSON).header("Content-Type",
                MediaType.APPLICATION_JSON).header("Authorization", "SSWS " + getAPIKey());
    }

    private Invocation.Builder buildRequestForMetadata(String path) {
        final String target = String.format("%s/%s", BASEURI, path);
        log.fine(String.format("Making request to %s...", target));
        return client.target(target).request().accept(MediaType.APPLICATION_XML).header("Content-Type",
                MediaType.APPLICATION_JSON).header("Authorization", "SSWS " + getAPIKey());
    }

    private static void verifyRequestResult(int currentResponseCode, int[] acceptableResponseCodes, String message)
            throws HttpRequestException {
        for (int acceptableResponseCode : acceptableResponseCodes) {
            if (currentResponseCode == acceptableResponseCode) return;
        }
        throw new HttpRequestException(
                String.format(
                        "Request to Okta API failed. "
                                + "Request return code is: %d. Expected codes are: %s. Message from service is: %s",
                        currentResponseCode, Arrays.toString(acceptableResponseCodes), message),
                currentResponseCode);
    }

    public static String getFinalizeUrlDependingOnBackend(String backendUrl) {
        return String.format("%s/sso/finalize-login",
                // Remove the slash at the end to not break SSO login with Okta
                backendUrl.substring(0, backendUrl.length() - 1));
    }

    public String createApplication(String label, String finalizeUrl) {
        final Invocation.Builder webResource = buildRequest("api/v1/apps");
        String data = new Scanner(this.getClass().getClassLoader().getResourceAsStream("okta/appCreation.json"), "UTF-8")
                .useDelimiter("\\A").next();
        JSONObject requestBody = new JSONObject(data);
        requestBody.put("label", label);
        JSONObject settings = requestBody.getJSONObject("settings");
        JSONObject signOn = settings.getJSONObject("signOn");
        signOn.put("ssoAcsUrl", finalizeUrl);
        signOn.put("audience", finalizeUrl);
        signOn.put("recipient", finalizeUrl);
        signOn.put("destination", finalizeUrl);
        settings.put("signOn", signOn);
        requestBody.put("settings", settings);
        String output = restHandlers.httpPost(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK});
        JSONObject responseBody = new JSONObject(output);
        applicationId = responseBody.getString("id");
        String groupId = fetchGroupId("Everyone");
        assignApplicationToGroup(applicationId, groupId);
        return applicationId;
    }

    public void createWronglyConfiguredApplication(String label, String backendUrl) {
        final Invocation.Builder webResource = buildRequest("api/v1/apps");
        String data = new Scanner(this.getClass().getClassLoader().getResourceAsStream("okta/appCreation.json"),
                "UTF-8").useDelimiter("\\A").next();
        JSONObject requestBody = new JSONObject(data);
        requestBody.put("label", "Wrongly configured application " + label);
        JSONObject settings = requestBody.getJSONObject("settings");
        JSONObject signOn = settings.getJSONObject("signOn");
        String finalizeUrl = getFinalizeUrlDependingOnBackend(backendUrl);
        signOn.put("ssoAcsUrl", finalizeUrl);
        signOn.put("audience", finalizeUrl);
        signOn.put("recipient", finalizeUrl);
        signOn.put("destination", "https://wrong-destination.local/");
        settings.put("signOn", signOn);
        requestBody.put("settings", settings);
        String output = restHandlers.httpPost(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK});
        JSONObject responseBody = new JSONObject(output);
        this.applicationId = responseBody.getString("id");
        String groupId = fetchGroupId("Everyone");
        assignApplicationToGroup(this.applicationId, groupId);
    }

    public String getApplicationId(String label) {
        final Invocation.Builder webResource = buildRequest("api/v1/apps");
        String output = restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK});
        JSONArray responseBody = new JSONArray(output);
        for (Object aResponseBody : responseBody) {
            JSONObject application = (JSONObject) aResponseBody;
            if (application.getString("label").equals(label)) {
                return application.getString("id");
            }
        }
        return null;
    }

    private String fetchGroupId(String groupName) {
        final Invocation.Builder webResource = buildRequest("api/v1/groups?limit=100");
        String output = restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK});
        JSONArray responseBody = new JSONArray(output);

        for (int i = 0; i < responseBody.length(); i++) {
            JSONObject group = responseBody.getJSONObject(i);
            if (group.getJSONObject("profile").getString("name").equals(groupName)) {
                return group.getString("id");
            }
        }
        throw new IllegalStateException(String.format("Cannot fetch id of a group with name '%s'", groupName));
    }

    private void assignApplicationToGroup(String appId, String groupId) {
        final Invocation.Builder webResource = buildRequest(String.format("api/v1/apps/%s/groups/%s", appId, groupId));
        restHandlers.httpPut(webResource, "{}", new int[]{HttpStatus.SC_OK});
    }

    public String getApplicationMetadata() {
        final Invocation.Builder webResource = buildRequestForMetadata(String.format("api/v1/apps/%s/sso/saml/metadata", applicationId));
        String xml = restHandlers.httpGet(webResource, new int[]{HttpStatus.SC_OK});
        DomUtils.toDocument(xml);
        return xml;
    }

    public void createUser(String name, String email, String password) {
        final Invocation.Builder webResource = buildRequest("api/v1/users?activate=true");
        JSONObject requestBody = new JSONObject();
        JSONObject profile = new JSONObject();
        profile.put("firstName", name);
        profile.put("lastName", name);
        profile.put("email", email);
        profile.put("login", email);
        requestBody.put("profile", profile);
        JSONObject credentials = new JSONObject();
        JSONObject credentialPassword = new JSONObject();
        credentialPassword.put("value", password);
        credentials.put("password", credentialPassword);
        JSONObject recoveryQuestion = new JSONObject();
        recoveryQuestion.put("question", "What is the answer to life, the universe and everything?");
        recoveryQuestion.put("answer", "fortytwo");
        credentials.put("recovery_question", recoveryQuestion);
        requestBody.put("credentials", credentials);
        String output = restHandlers.httpPost(webResource, requestBody.toString(), new int[]{HttpStatus.SC_OK});
        JSONObject responseBody = new JSONObject(output);
        userIds.add(responseBody.getString("id"));
    }

    public void deleteUser(String userId) {
        final Invocation.Builder webResource = buildRequest(String.format("api/v1/users/%s/lifecycle/deactivate", userId));
        restHandlers.httpPost(webResource, "", new int[]{HttpStatus.SC_OK});
        final Invocation.Builder webResource2 = buildRequest(String.format("api/v1/users/%s", userId));
        restHandlers.httpDelete(webResource2, new int[]{HttpStatus.SC_NO_CONTENT});
    }

    public void cleanUp() {
        if (applicationId != null) {
            final Invocation.Builder webResource = buildRequest(String.format("api/v1/apps/%s/lifecycle/deactivate",
                    applicationId));
            restHandlers.httpPost(webResource, "", new int[]{HttpStatus.SC_OK});
            final Invocation.Builder webResource2 = buildRequest(String.format("api/v1/apps/%s", applicationId));
            restHandlers.httpDelete(webResource2, new int[]{HttpStatus.SC_NO_CONTENT});
        }
        for (String userId : userIds) {
            deleteUser(userId);
        }
    }
}
