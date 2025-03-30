package com.wearezeta.auto.common.sso;

import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.backend.HttpRequestException;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.log.ZetaLogger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.net.HttpURLConnection.*;

public class KeycloakAPIClient {

    private static final Logger log = ZetaLogger.getLog(KeycloakAPIClient.class.getSimpleName());
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(240);
    private static final String REALM = "master";
    private static final String ADMIN = "admin";
    private final String backendName;

    private String clientId;
    private Set<String> userIds = new HashSet<>();

    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
    };

    public KeycloakAPIClient(String backendName) {
        this.backendName = backendName;
        final ClientConfig configuration = new ClientConfig();
        configuration.property(ClientProperties.CONNECT_TIMEOUT, (int) CONNECT_TIMEOUT.toMillis());
        configuration.property(ClientProperties.READ_TIMEOUT, (int) READ_TIMEOUT.toMillis());
        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            log.severe("Could not install all-trusting trust manager: " + e.getMessage());
        }
    }

    private String getBaseURI() {
        return BackendConnections.get(backendName).getKeycloakUrl();
    }

    public String getMetadata() {
        HttpURLConnection c = buildDefaultRequest(
                String.format("/realms/%s/protocol/saml/descriptor", REALM),
                MediaType.APPLICATION_XML);
        final String output = httpGet(c, new int[]{HTTP_OK});
        DomUtils.toDocument(output);
        return output;
    }

    public void createSAMLClient(String teamId, String backendURL) {
        String token = authorize();
        log.info("Token: " + token);
        HttpURLConnection c = buildAuthorizedRequest(String.format("admin/realms/%s/clients", REALM),
                MediaType.APPLICATION_JSON, token);
        String finalizeUrl = String.format("%ssso/finalize-login/%s", backendURL, teamId);
        JSONObject requestBody = new JSONObject();
        requestBody.put("clientId", finalizeUrl);
        //requestBody.put("surrogateAuthRequired", false);
        requestBody.put("enabled", true);
        requestBody.put("adminUrl", "");
        requestBody.put("baseUrl", "");
        requestBody.put("rootUrl", "");
        requestBody.put("name", "");
        requestBody.put("description", "");
        //requestBody.put("clientAuthenticatorType", "client-secret");
        //requestBody.put("secret", "iJg6Ysa0qWdmGCnNzLkqb6rTjjhRgPF6");
        JSONArray redirectUris = new JSONArray();
        redirectUris.put(finalizeUrl);
        requestBody.put("redirectUris", redirectUris);
        JSONArray webOrigins = new JSONArray();
        webOrigins.put(backendURL.substring(0, backendURL.length() - 1));
        requestBody.put("webOrigins", webOrigins);
        requestBody.put("protocol", "saml");
        JSONObject attributes = new JSONObject();
        attributes.put("display.on.consent.screen", "false");
        attributes.put("saml.encrypt", "false");
        attributes.put("saml_assertion_consumer_url_post", finalizeUrl);
        attributes.put("saml.client.signature", "false");
        attributes.put("saml.artifact.binding", "false");
        attributes.put("saml.assertion.signature", "true");
        attributes.put("saml.onetimeuse.condition", "false");
        attributes.put("saml.server.signature.keyinfo.ext", "false");
        attributes.put("saml.server.signature.keyinfo.xmlSigKeyInfoKeyNameTransformer", "NONE");
        requestBody.put("attributes", attributes);
        String location = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        this.clientId = getIdFromLocation(location);
    }

    public void createUser(String username, String firstname, String lastname, String email, String password) {
        HttpURLConnection c = buildAuthorizedRequest(String.format("admin/realms/%s/users", REALM),
                MediaType.APPLICATION_JSON, authorize());
        JSONObject user = new JSONObject();
        user.put("username", username);
        user.put("firstName", firstname);
        user.put("lastName", lastname);
        user.put("email", email);
        user.put("emailVerified", true);
        user.put("enabled", true);
        JSONArray credentials = new JSONArray();
        JSONObject passwordCredential = new JSONObject();
        passwordCredential.put("type", "password");
        passwordCredential.put("value", password);
        passwordCredential.put("temporary", false);
        credentials.put(passwordCredential);
        user.put("credentials", credentials);
        String location = httpPost(c, user.toString(), new int[]{HTTP_CREATED});
        userIds.add(getIdFromLocation(location));
    }

    public void cleanUp() {
        if (clientId != null) {
            deleteSAMLClient(clientId);
        }
        for (String userId : userIds) {
            deleteUser(userId);
        }
    }

    private void deleteSAMLClient(String clientId) {
        HttpURLConnection c = buildAuthorizedRequest(String.format("admin/realms/%s/clients/%s", REALM, clientId),
                MediaType.APPLICATION_JSON, authorize());
        httpDelete(c, new int[]{HTTP_NO_CONTENT});
    }

    private void deleteUser(String userId) {
        HttpURLConnection c = buildAuthorizedRequest(String.format("admin/realms/%s/users/%s", REALM, userId),
                MediaType.APPLICATION_JSON, authorize());
        httpDelete(c, new int[]{HTTP_NO_CONTENT});
    }

    private String getIdFromLocation(String location) {
        log.info("Location: " + location);
        return location.substring(location.lastIndexOf("/") + 1);
    }

    private String authorize() {
        String PASSWORD = Credentials.get("KEYCLOAK_PASSWORD");
        HttpURLConnection c = null;
        try {
            URL url = new URL(String.format("%s/realms/%s/protocol/openid-connect/token", getBaseURI(), REALM));
            c = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        c.setRequestProperty("Content-Type", MediaType.APPLICATION_FORM_URLENCODED);
        c.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
        String response = "";
        int status = -1;
        int[] acceptableResponseCodes = new int[]{HTTP_OK};
        try {
            String requestBody = "client_id=" + URLEncoder.encode("admin-cli", StandardCharsets.UTF_8) +
                    "&username=" + URLEncoder.encode(ADMIN, StandardCharsets.UTF_8) +
                    "&password=" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8) +
                    "&grant_type=" + URLEncoder.encode("password", StandardCharsets.UTF_8);
            log.info("POST " + c.getURL());
            c.setRequestMethod("POST");
            logHttpRequestProperties(c);
            logRequest(requestBody);
            c.setDoOutput(true);
            writeStream(requestBody, c.getOutputStream());
            status = c.getResponseCode();
            response = readStream(c.getInputStream());
            logResponseAndStatusCode(response, status);
            assertResponseCode(status, acceptableResponseCodes);
            return new JSONObject(response).getString("access_token");
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            if (Arrays.stream(acceptableResponseCodes).anyMatch(acceptable -> acceptable > 400)) {
                assertResponseCode(status, acceptableResponseCodes);
                log.info(String.format(">>> Response (%s): %s", status, response));
                return response;
            } else {
                String error = String.format("%s (%s): %s", e.getMessage(), status, response);
                log.severe(error);
                throw new HttpRequestException(error, status);
            }
        } finally {
            c.disconnect();
        }
    }

    private HttpURLConnection buildAuthorizedRequest(String path, String mediaType, String token) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(String.format("%s/%s", getBaseURI(), path));
            c = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        c.setRequestProperty("Content-Type", mediaType);
        c.setRequestProperty("Accept", mediaType);
        c.setRequestProperty("Authorization", "Bearer " + token);
        return c;
    }

    private HttpURLConnection buildDefaultRequest(String path, String mediaType) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(String.format("%s%s", getBaseURI(), path));
            c = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        c.setRequestProperty("Content-Type", mediaType);
        c.setRequestProperty("Accept", mediaType);
        return c;
    }

    private String httpGet(HttpURLConnection c, int[] acceptableResponseCodes) {
        String response = "";
        int status = -1;
        try {
            log.info("GET " + c.getURL());
            c.setRequestMethod("GET");
            logHttpRequestProperties(c);
            status = c.getResponseCode();
            response = readStream(c.getInputStream());
            logResponseAndStatusCode(response, status);
            assertResponseCode(status, acceptableResponseCodes);
            return response;
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            if (Arrays.stream(acceptableResponseCodes).anyMatch(acceptable -> acceptable > 400)) {
                assertResponseCode(status, acceptableResponseCodes);
                log.info(String.format(">>> Response (%s): %s", status, response));
                return response;
            } else {
                String error = String.format("%s (%s): %s", e.getMessage(), status, response);
                log.severe(error);
                throw new HttpRequestException(error, status);
            }
        } finally {
            c.disconnect();
        }
    }

    private String httpPost(HttpURLConnection c, String requestBody, int[] acceptableResponseCodes) {
        String response = "";
        String location = "";
        int status = -1;
        try {
            log.info("POST " + c.getURL());
            c.setRequestMethod("POST");
            logHttpRequestProperties(c);
            logRequest(requestBody);
            c.setDoOutput(true);
            writeStream(requestBody, c.getOutputStream());
            status = c.getResponseCode();
            response = readStream(c.getInputStream());
            location = c.getHeaderField("Location");
            logResponseAndStatusCode(response, status);
            assertResponseCode(status, acceptableResponseCodes);
            return location;
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            if (Arrays.stream(acceptableResponseCodes).anyMatch(acceptable -> acceptable > 400)) {
                assertResponseCode(status, acceptableResponseCodes);
                log.info(String.format(">>> Response (%s): %s", status, response));
                return response;
            } else {
                String error = String.format("%s (%s): %s", e.getMessage(), status, response);
                log.severe(error);
                throw new HttpRequestException(error, status);
            }
        } finally {
            c.disconnect();
        }
    }

    private String httpDelete(HttpURLConnection c, int[] acceptableResponseCodes) {
        String response = "";
        int status = -1;
        try {
            log.info("DELETE " + c.getURL());
            c.setRequestMethod("DELETE");
            logHttpRequestProperties(c);
            c.setDoOutput(true);
            writeStream("", c.getOutputStream());
            status = c.getResponseCode();
            response = readStream(c.getInputStream());
            logResponseAndStatusCode(response, status);
            assertResponseCode(status, acceptableResponseCodes);
            return response;
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            String error = String.format("%s (%s): %s", e.getMessage(), status, response);
            log.severe(error);
            throw new HttpRequestException(error, status);
        } finally {
            c.disconnect();
        }
    }

    private void writeStream(String data, OutputStream os) throws IOException {
        DataOutputStream wr = new DataOutputStream(os);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, StandardCharsets.UTF_8));
        try {
            writer.write(data);
        } finally {
            writer.close();
            wr.close();
        }
    }

    private String readStream(InputStream is) throws IOException {
        if (is != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                return content.toString();
            }
        }
        return "";
    }

    private void logRequest(String request) {
        if (request.isEmpty()) {
            log.info(" >>> Request with no request body");
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Request: %s", request));
            } else {
                log.info(String.format(" >>> Request: %s", truncate(request)));
            }
        }
    }

    private void logResponseAndStatusCode(String response, int responseCode) {
        if (response.isEmpty()) {
            log.info(String.format(" >>> Response (%s) with no response body", responseCode));
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Response (%s): %s", responseCode, response));
            } else {
                log.info(String.format(" >>> Response (%s): %s", responseCode, truncate(response)));
            }
        }
    }

    private String truncate(String text) {
        final int MAX_LOG_ENTRY_LENGTH = 280;
        if (text.length() > MAX_LOG_ENTRY_LENGTH) {
            return text.substring(0, MAX_LOG_ENTRY_LENGTH) + "...";
        }
        return text;
    }

    private void assertResponseCode(int responseCode, int[] acceptableResponseCodes) {
        if (Arrays.stream(acceptableResponseCodes).noneMatch(a -> a == responseCode)) {
            throw new HttpRequestException(
                    String.format("Backend request failed. Request return code is: %d. Expected codes are: %s.",
                            responseCode,
                            Arrays.toString(acceptableResponseCodes)),
                    responseCode);
        }
    }

    private void logHttpRequestProperties(HttpURLConnection c) {
        if (log.isLoggable(Level.FINE)) {
            for (String property : c.getRequestProperties().keySet()) {
                List<String> values = Collections.singletonList(c.getRequestProperty(property));
                log.fine(String.format("%s: %s", property, String.join(", ", values)));
            }
        }
    }
}
