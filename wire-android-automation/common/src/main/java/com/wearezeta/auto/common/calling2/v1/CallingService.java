package com.wearezeta.auto.common.calling2.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wearezeta.auto.common.backend.HttpRequestException;
import com.wearezeta.auto.common.calling2.v1.exception.CallingServiceCallException;
import com.wearezeta.auto.common.calling2.v1.exception.CallingServiceInstanceException;
import com.wearezeta.auto.common.calling2.v1.model.*;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.rest.RESTError;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.net.HttpURLConnection.HTTP_OK;

class CallingService {

    private static final Logger log = Logger.getLogger(CallingService.class.getName());

    private final String callingServiceAdress;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, HttpCookie> cookies = new ConcurrentHashMap<>();
    private String basicAuthUser = null;
    private String basicAuthPassword = null;

    public CallingService(String callingServiceAdress, boolean trace) {
        this.callingServiceAdress = callingServiceAdress;
    }

    private String getBasicAuthentication() {
        if (basicAuthUser == null) {
            basicAuthUser = "qa";
        }
        if (basicAuthPassword == null) {
            basicAuthPassword = Credentials.get("CALLINGSERVICE_BASIC_AUTH");
        }
        String auth = basicAuthUser + ":" + basicAuthPassword;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedAuth);
    }

    private HttpURLConnection buildRequestForInstance(String instanceId, String path, String mediaType) {
        return buildRequestForInstance(instanceId, path, mediaType, mediaType);
    }

    private HttpURLConnection buildRequestForInstance(String instanceId, String path, String contentType,
                                                      String accept) {
        URL url = getCallingServiceUrl(path);
        HttpURLConnection c;
        try {
            c = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        c.setRequestProperty("Authorization", getBasicAuthentication());
        if (contentType != null) {
            c.setRequestProperty("Content-Type", contentType);
        }
        if (accept != null) {
            c.setRequestProperty("Accept", accept);
        }
        if (cookies.containsKey(instanceId)) {
            HttpCookie cookie = cookies.get(instanceId);
            if (cookie.getName().equals("SERVERID")) {
                c.setRequestProperty("Cookie", cookie.getName() + "=" + cookie.getValue());
            }
        }
        return c;
    }

    private HttpURLConnection buildRequestForCertainInstance(String serverId, String path, String contentType, String accept) {
        URL url = getCallingServiceUrl(path);
        HttpURLConnection c;
        try {
            c = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        c.setRequestProperty("Authorization", getBasicAuthentication());
        if (contentType != null) {
            c.setRequestProperty("Content-Type", contentType);
        }
        if (accept != null) {
            c.setRequestProperty("Accept", accept);
        }
        c.setRequestProperty("Cookie", "SERVERID=" + serverId);
        return c;
    }

    private URL getCallingServiceUrl(String path) {
        try {
            return new URL(callingServiceAdress + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public Instance createInstance(InstanceRequest instanceRequest) throws CallingServiceInstanceException {
        try {
            URL url = getCallingServiceUrl("/api/v1/instance/create");
            HttpURLConnection c;
            try {
                c = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            c.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON);
            c.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
            c.setRequestProperty("Authorization", getBasicAuthentication());

            // Make request
            // TODO: Better error handling when password of basic auth is wrong
            String response = httpPost(c, mapper.writeValueAsString(instanceRequest), new int[]{HTTP_OK});
            Instance instance = mapper.readValue(response, Instance.class);
            String cookiesHeader = c.getHeaderField("Set-Cookie");
            // Save cookie from load balancer
            if (cookiesHeader != null) {
                List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
                cookies.stream()
                        .filter(x -> x.getName().equals("SERVERID"))
                        .findFirst()
                        .ifPresent(cookie -> this.cookies.put(instance.getId(), cookie));
            }
            return instance;
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Instance destroyInstance(Instance instance) throws CallingServiceInstanceException {
        final String target = String.format("/api/v1/instance/%s/destroy", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            // clear instance from cookie store
            cookies.remove(instance.getId());
            return mapper.readValue(response, Instance.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Instance getInstance(Instance instance) throws CallingServiceInstanceException {
        final String target = String.format("/api/v1/instance/%s/status", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpGet(c, new int[]{HTTP_OK});
            return mapper.readValue(response, Instance.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public List<Flow> getFlows(Instance instance) throws CallingServiceInstanceException {
        final String target = String.format("/api/v1/instance/%s/flows", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpGet(c, new int[]{HTTP_OK});
            return mapper.readValue(response, new TypeReference<List<Flow>>() {
            });
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public String getPackets(Instance instance) throws CallingServiceInstanceException {
        final String target = String.format("/api/v1/instance/%s/packets", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            return httpGet(c, new int[]{HTTP_OK});
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public BufferedImage getScreenshot(Instance instance) throws CallingServiceInstanceException {
        final String target = String.format("/api/v1/instance/%s/screenshot", instance.getId());
        // FIXME: Might be wrong media type
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, null);
        try {
            return httpGetImage(c, new int[]{HTTP_OK});
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public String getLog(Instance instance) throws CallingServiceInstanceException {
        final String target = String.format("/api/v1/instance/%s/log", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.TEXT_PLAIN);
        try {
            return httpGet(c, new int[]{HTTP_OK, HttpStatus.SC_NOT_FOUND});
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    /*
     *  Call related api calls
     */

    public Call start(Instance instance, CallRequest callRequest) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/start", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPost(c, mapper.writeValueAsString(callRequest), new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call startVideo(Instance instance, CallRequest callRequest) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/startVideo", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPost(c, mapper.writeValueAsString(callRequest), new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call acceptNext(Instance instance, CallRequest callRequest) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/acceptNext", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPost(c, mapper.writeValueAsString(callRequest), new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call acceptNextVideo(Instance instance, CallRequest callRequest) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/acceptNextVideo", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPost(c, mapper.writeValueAsString(callRequest), new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call stop(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/stop", instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call decline(Instance instance, CallRequest callRequest) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/decline", instance.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPost(c, mapper.writeValueAsString(callRequest), new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call getCall(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/status", instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpGet(c, new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call switchVideoOn(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/switchVideoOn", instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call switchVideoOff(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/switchVideoOff", instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call pauseVideoCall(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/pauseVideoCall", instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call unpauseVideoCall(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/unpauseVideoCall", instance.getId(),
                call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call switchScreensharingOn(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/switchScreensharingOn", instance.getId(),
                call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call switchScreensharingOff(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/switchScreensharingOff",
                instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call muteMicrophone(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/mute", instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call unmuteMicrophone(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/unmute", instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call maximiseCall(Instance instance, Call call) throws CallingServiceCallException {
        final String target = String.format("/api/v1/instance/%s/call/%s/maximiseVideoCall", instance.getId(), call.getId());
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceCallException(new RESTError(ex));
        }
    }

    public List<Instance> getAllRunningInstances() {
        final String target = "/api/v1/instance";
        String serverIds[] = new String[]{"c1", "c2", "c3", "c4"};

        List<Instance> instances = new ArrayList<>();

        for (String serverId : serverIds) {
            HttpURLConnection c = buildRequestForCertainInstance(serverId, target, MediaType.APPLICATION_JSON,
                    MediaType.APPLICATION_JSON);
            try {
                String response = httpGet(c, new int[]{HTTP_OK});
                List<Instance> instancesOfServer = mapper.readValue(response, new TypeReference<List<Instance>>() {
                });
                for (Instance instance : instancesOfServer) {
                    if (!cookies.containsKey(instance.getId())) {
                        HttpCookie cookie = new HttpCookie("SERVERID", serverId);
                        cookies.put(instance.getId(), cookie);
                    }
                }
                instances.addAll(instancesOfServer);
            } catch (Exception ex) {
                throw new CallingServiceInstanceException(new RESTError(ex));
            }
        }

        return instances;
    }

    // region moderation

    public Call muteParticipantX(Instance instance, Call call, String name) {
        name = name.replace(" ", "%20");
        final String target = String.format("/api/v1/instance/%s/call/%s/muteParticipant/%s", instance.getId(), call.getId(), name);
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    public Call muteAllOthers(Instance instance, Call call, String name) {
        name = name.replace(" ", "%20");
        final String target = String.format("/api/v1/instance/%s/call/%s/muteAllOthers/%s", instance.getId(), call.getId(), name);
        HttpURLConnection c = buildRequestForInstance(instance.getId(), target, MediaType.APPLICATION_JSON);
        try {
            String response = httpPut(c, "", new int[]{HTTP_OK});
            return mapper.readValue(response, Call.class);
        } catch (Exception ex) {
            throw new CallingServiceInstanceException(new RESTError(ex));
        }
    }

    // endregion

    // region HTTP connection logic

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

    private BufferedImage httpGetImage(HttpURLConnection c, int[] acceptableResponseCodes) {
        int status = -1;
        try {
            log.info("GET " + c.getURL());
            c.setRequestMethod("GET");
            logHttpRequestProperties(c);
            status = c.getResponseCode();
            BufferedImage response = ImageIO.read(c.getInputStream());
            assertResponseCode(status, acceptableResponseCodes);
            return response;
        } catch (IOException e) {
            String response = "";
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

    private String httpPost(HttpURLConnection c, String requestBody, int[] acceptableResponseCodes) {
        String response = "";
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
                if (status == 401) {
                    throw new RuntimeException("Not authorized to start instance. Make sure basic authentication is correct for callingservice.", e);
                }
                String error = String.format("%s (%s): %s", e.getMessage(), status, response);
                log.severe(error);
                throw new HttpRequestException(error, status);
            }
        } finally {
            c.disconnect();
        }
    }

    private String httpPut(HttpURLConnection c, String requestBody, int[] acceptableResponseCodes) {
        String response = "";
        int status = -1;
        try {
            log.info("PUT " + c.getURL());
            c.setRequestMethod("PUT");
            logHttpRequestProperties(c);
            log.fine(String.format(" >>> Request: %s", requestBody));
            c.setDoOutput(true);
            writeStream(requestBody, c.getOutputStream());
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

    private void logHttpRequestProperties(HttpURLConnection c) {
        if (log.isLoggable(Level.FINE)) {
            for (String property : c.getRequestProperties().keySet()) {
                List<String> values = Collections.singletonList(c.getRequestProperty(property));
                log.fine(String.format("%s: %s", property, String.join(", ", values)));
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

    private void assertResponseCode(int responseCode, int[] acceptableResponseCodes) {
        if (Arrays.stream(acceptableResponseCodes).noneMatch(a -> a == responseCode)) {
            throw new HttpRequestException(
                    String.format("Backend request failed. Request return code is: %d. Expected codes are: %s.",
                            responseCode,
                            Arrays.toString(acceptableResponseCodes)),
                    responseCode);
        }
    }

    private String truncate(String text) {
        final int MAX_LOG_ENTRY_LENGTH = 100;
        if (text.length() > MAX_LOG_ENTRY_LENGTH) {
            return text.substring(0, MAX_LOG_ENTRY_LENGTH) + "...";
        }
        return text;
    }

    // endregion
}
