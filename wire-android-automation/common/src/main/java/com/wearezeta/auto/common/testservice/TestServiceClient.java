package com.wearezeta.auto.common.testservice;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.backend.HttpRequestException;
import com.wearezeta.auto.common.backend.models.TypingStatus;
import com.wearezeta.auto.common.testservice.models.Mention;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.usrmgmt.ClientUser;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class TestServiceClient {

    private static final Logger log = ZetaLogger.getLog(TestServiceClient.class.getSimpleName());

    private static final int CONNECT_TIMEOUT = (int) Duration.ofSeconds(120).toMillis();
    private static final int READ_TIMEOUT = (int) Duration.ofSeconds(120).toMillis();

    // userAliases => deviceName => instanceId
    private final ConcurrentHashMap<String, Map<String, String>> userAliases = new ConcurrentHashMap<>();

    private final String baseUri;
    private final String testName;

    public TestServiceClient(String baseUri, String testName) {
        this.baseUri = baseUri;
        this.testName = testName;
    }

    // region HTTP connection logic

    private HttpURLConnection buildRequest(String path, String requestType) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(String.format("%s/%s", baseUri, path));
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod(requestType); // PUT, POST, DELETE, GET
            c.setDoOutput(true);
            c.setConnectTimeout(CONNECT_TIMEOUT);
            c.setReadTimeout(READ_TIMEOUT);
            c.setRequestProperty("Accept-Encoding", "UTF-8");
            c.setRequestProperty("Content-Type", "application/json");
        } catch (IOException e) {
            log.severe("Connection failed: " + e);
            throw new RuntimeException();
        }
        return c;
    }

    private String sendHttpRequest(HttpURLConnection c, @Nullable JSONObject request) {
        String response = "";
        int status = -1;
        try {
            log.info(String.format("%s: %s", c.getRequestMethod(), c.getURL()));
            if (request != null) {
                if (log.isLoggable(Level.FINE)) {
                    log.info(String.format(" >>> Request: %s", truncateOnlyOnBig(request.toString())));
                } else {
                    log.info(String.format(" >>> Request: %s", truncateOnlyOnBig(request.toString())));
                }
                writeStream(request.toString(), c.getOutputStream());
            }
            status = c.getResponseCode();
            response = readStream(c.getInputStream());
            logResponseAndStatusCode(response, status);
            assertResponseCode(status, List.of(HttpStatus.SC_OK, HttpStatus.SC_NO_CONTENT));
            return response;
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            String error = String.format("%s (%s): %s", e.getMessage(), status, response);
            log.severe(error);
            throw new HttpRequestException(error);
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
        } else {
            return "";
        }
    }

    private void logResponseAndStatusCode(String response, int responseCode) {
        if (response.isEmpty()) {
            log.info(String.format(" >>> Response (%s) with no response body", responseCode));
        } else {
            if (log.isLoggable(Level.FINE)) {
                log.info(String.format(" >>> Response (%s): %s", responseCode, truncateOnlyOnBig(response)));
            } else {
                log.info(String.format(" >>> Response (%s): %s", responseCode, response));
            }
        }
    }

    private String truncate(String text) {
        return truncate(text, 100);
    }

    private String truncateOnlyOnBig(String text) {
        return truncate(text, 5000);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() > maxLength) {
            return text.substring(0, maxLength) + "...";
        }
        return text;
    }

    private void assertResponseCode(int responseCode, List<Integer> acceptableResponseCodes) {
        if (!acceptableResponseCodes.contains(responseCode)) {
            throw new HttpRequestException(
                    String.format("Testservice request failed. Request return code is: %d. Expected code is: %s.",
                            responseCode,
                            acceptableResponseCodes.stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(","))
                    ), responseCode);
        }
    }

    // endregion HTTP connection logic

    public void login(ClientUser owner, @Nullable String verificationCode, @Nullable String deviceName,
                      boolean developmentApiEnabled) {
        final HttpURLConnection connection = buildRequest("api/v1/instance", "PUT");
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", owner.getEmail());
        requestBody.put("password", owner.getPassword());
        if (verificationCode != null) {
            requestBody.put("verificationCode", verificationCode);
        }
        requestBody.put("deviceName", deviceName);
        requestBody.put("name", testName);
        if (owner.getBackendName().equals("staging")) {
            requestBody.put("backend", "staging");
        } else {
            Backend ownerBackend = BackendConnections.get(owner.getBackendName());
            JSONObject customBackend = new JSONObject();
            customBackend.put("name", ownerBackend.getBackendName());
            customBackend.put("rest", ownerBackend.getBackendUrl());
            customBackend.put("ws", ownerBackend.getBackendWebsocket());
            requestBody.put("customBackend", customBackend);
            requestBody.put("federationDomain", ownerBackend.getDomain());
        }
        if (developmentApiEnabled) {
            if (isKaliumTestservice()) {
                requestBody.put("developmentApiEnabled", true);
            }
        }
        String result = sendHttpRequest(connection, requestBody);
        JSONObject responseBody = new JSONObject(result);
        String instanceId = responseBody.getString("instanceId");
        userAliases.compute(owner.getName(), (key, value) -> {
            if (value == null) {
                Map<String, String> devices = new ConcurrentHashMap<>();
                devices.put(deviceName, instanceId);
                return devices;
            }
            value.put(deviceName, instanceId);
            return value;
        });
    }

    public void setAvailability(ClientUser owner, @Nullable String deviceName, String teamId, int availabilityType) {
        log.info("Availability status is only send to known devices. Make sure the user has send a message before.");
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/availability", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("teamId", teamId);
        requestBody.put("type", availabilityType);
        sendHttpRequest(connection, requestBody);
    }

    public String sendText(ClientUser owner, @Nullable String deviceName, String convoDomain, String convoId, Timedelta timeout,
                           boolean expectsReadConfirmation, String text, int legalHoldStatus) {
        return sendTextBase(owner, deviceName, convoDomain, convoId, timeout, expectsReadConfirmation, text, null, legalHoldStatus);
    }

    public String sendCompositeText(ClientUser owner, @Nullable String deviceName, String convoDomain, String convoId, Timedelta timeout,
                                    boolean expectsReadConfirmation, String text, JSONArray buttons, int legalHoldStatus) {
        return sendTextBase(owner, deviceName, convoDomain, convoId, timeout, expectsReadConfirmation, text, buttons, legalHoldStatus);
    }

    private String sendTextBase(ClientUser owner, @Nullable String deviceName, String convoDomain, String convoId, Timedelta timeout,
                               boolean expectsReadConfirmation, String text, @Nullable JSONArray buttons, int legalHoldStatus) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendText", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("conversationId", convoId);
        if (timeout.asMillis() > 0) {
            requestBody.put("messageTimer", timeout.asMillis());
        }
        if (expectsReadConfirmation) {
            requestBody.put("expectsReadConfirmation", expectsReadConfirmation);
        }
        requestBody.put("text", text);
        // composite message attributes
        if (buttons != null) {
            requestBody.put("buttons", buttons);
        }
        requestBody.put("legalHoldStatus", legalHoldStatus);
        String result = sendHttpRequest(connection, requestBody);
        JSONObject response = new JSONObject(result);
        return response.getString("messageId");
    }

    public void updateText(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, String messageId, String text) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/updateText", instanceId),"POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("firstMessageId", messageId);
        requestBody.put("text", text);
        sendHttpRequest(connection, requestBody);
    }

    public void updateTextWithLinkPreview(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain,
                                          String messageId, String text, String summary, String title, String url,
                                          int urlOffset, String permUrl, String filePath) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/updateText", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("firstMessageId", messageId);
        requestBody.put("text", text);
        JSONObject linkPreview = new JSONObject();

        // image
        if (filePath != null) {
            File imageFile = new File(filePath);
            JSONObject requestImage = new JSONObject();
            requestImage.put("data", fileToBase64String(imageFile));
            BufferedImage image;
            try {
                image = ImageIO.read(imageFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            requestImage.put("width", image.getWidth());
            requestImage.put("height", image.getHeight());
            requestImage.put("type", "image/" + filePath.substring(filePath.lastIndexOf('.') + 1));
            linkPreview.put("image", requestImage);
        }

        linkPreview.put("summary", summary);
        linkPreview.put("title", title);
        linkPreview.put("url", url);
        linkPreview.put("urlOffset", urlOffset);
        linkPreview.put("permanentUrl", permUrl);
        requestBody.put("linkPreview", linkPreview);
        sendHttpRequest(connection, requestBody);
    }

    public void sendTyping(ClientUser owner, @Nullable String deviceName, String convoId, TypingStatus status) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendTyping", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        requestBody.put("status", status.name().toLowerCase());
        sendHttpRequest(connection, requestBody);
    }

    public void clearConversation(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/clear", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        sendHttpRequest(connection, requestBody);
    }

    public void archiveConversation(ClientUser owner, @Nullable String deviceName, String convoId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/archive", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("archive", true);
        requestBody.put("conversationId", convoId);
        sendHttpRequest(connection, requestBody);
    }

    public void unarchiveConversation(ClientUser owner, @Nullable String deviceName, String convoId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/archive", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("archive", false);
        requestBody.put("conversationId", convoId);
        sendHttpRequest(connection, requestBody);
    }

    public void deleteForMe(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, String messageId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/delete", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("messageId", messageId);
        sendHttpRequest(connection, requestBody);
    }

    public void deleteEverywhere(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, String messageId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/deleteEverywhere", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("messageId", messageId);
        sendHttpRequest(connection, requestBody);
    }

    public void sendFile(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta timeout,
                         String filePath, String type) {
        sendFile(owner, deviceName, convoId, convoDomain, timeout, filePath, type, false, false, false);
    }

    public void sendFile(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta timeout,
                         String filePath, String type, boolean otherAlgorithm, boolean otherHash, boolean invalidHash) {
        File file = new File(filePath);
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendFile", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("data", fileToBase64String(file));
        requestBody.put("fileName", file.getName());
        if (timeout.asMillis() > 0) {
            requestBody.put("messageTimer", timeout.asMillis());
        }
        requestBody.put("type", type);
        requestBody.put("otherAlgorithm", otherAlgorithm);
        requestBody.put("otherHash", otherHash);
        requestBody.put("invalidHash", invalidHash);
        sendHttpRequest(connection, requestBody);
    }

    public void sendAudioFile(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain,
                              Timedelta timeout, Timedelta duration, int[] normalizedLoudness, String filePath, String type) {
        File file = new File(filePath);
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendFile", instanceId), "POST");
        JSONObject metadata = new JSONObject();
        metadata.put("durationInMillis", java.lang.Math.toIntExact(duration.asMillis()));
        metadata.put("normalizedLoudness", normalizedLoudness);
        JSONObject requestBody = new JSONObject();
        requestBody.put("audio", metadata);
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("data", fileToBase64String(file));
        requestBody.put("fileName", file.getName());
        if (timeout.asMillis() > 0) {
            requestBody.put("messageTimer", timeout.asMillis());
        }
        requestBody.put("type", type);
        sendHttpRequest(connection, requestBody);
    }

    public void sendVideoFile(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain,
                              Timedelta timeout, Timedelta duration, int[] dimensions, String filePath, String type) {
        File file = new File(filePath);
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendFile", instanceId), "POST");
        JSONObject metadata = new JSONObject();
        metadata.put("durationInMillis", java.lang.Math.toIntExact(duration.asMillis()));
        metadata.put("height", dimensions[0]);
        metadata.put("width", dimensions[1]);
        JSONObject requestBody = new JSONObject();
        requestBody.put("video", metadata);
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("data", fileToBase64String(file));
        requestBody.put("fileName", file.getName());
        if (timeout.asMillis() > 0) {
            requestBody.put("messageTimer", timeout.asMillis());
        }
        requestBody.put("type", type);
        sendHttpRequest(connection, requestBody);
    }

    public void sendImage(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta timeout,
                          String filePath) {
        File imageFile = new File(filePath);
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendImage", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("data", fileToBase64String(imageFile));
        BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        requestBody.put("width", image.getWidth());
        requestBody.put("height", image.getHeight());
        if (timeout.asMillis() > 0) {
            requestBody.put("messageTimer", timeout.asMillis());
        }
        requestBody.put("type", "image/" + filePath.substring(filePath.lastIndexOf('.') + 1));
        sendHttpRequest(connection, requestBody);
    }

    public void sendImage(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta timeout,
                          byte[] imageAsBytes, String type, int width, int height) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendImage", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("data", Base64.encodeBase64String(imageAsBytes));
        requestBody.put("width", width);
        requestBody.put("height", height);
        if (timeout.asMillis() > 0) {
            requestBody.put("messageTimer", timeout.asMillis());
        }
        requestBody.put("type", "image/" + type);
        sendHttpRequest(connection, requestBody);
    }

    public void sendLinkPreview(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain,
                                Timedelta messageTimer, String text, String summary, String title, String url,
                                int urlOffset, String permUrl, @Nullable String imagePath) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendText", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("text", text);
        if (messageTimer.asMillis() > 0) {
            requestBody.put("messageTimer", messageTimer.asMillis());
        }
        JSONObject linkPreview = new JSONObject();
        // image
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            JSONObject requestImage = new JSONObject();
            requestImage.put("data", fileToBase64String(imageFile));
            BufferedImage image;
            try {
                image = ImageIO.read(imageFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            requestImage.put("width", image.getWidth());
            requestImage.put("height", image.getHeight());
            requestImage.put("type", "image/" + imagePath.substring(imagePath.lastIndexOf('.') + 1));
            linkPreview.put("image", requestImage);
        }
        linkPreview.put("summary", summary);
        linkPreview.put("title", title);
        linkPreview.put("url", url);
        linkPreview.put("urlOffset", urlOffset);
        linkPreview.put("permanentUrl", permUrl);
        requestBody.put("linkPreview", linkPreview);
        sendHttpRequest(connection, requestBody);
    }
    public void sendTextWithMentions(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta messageTimer,
                                     String text, List<Mention> listOfMentions) {
        sendTextWithMentionsBase(owner, deviceName, convoId, convoDomain, messageTimer, text, listOfMentions, null);
    }

    public void sendCompositeTextWithMentions(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta messageTimer,
                                              String text, List<Mention> listOfMentions, JSONArray buttons) {
        sendTextWithMentionsBase(owner, deviceName, convoId, convoDomain, messageTimer, text, listOfMentions, buttons);
    }

    private void sendTextWithMentionsBase(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta messageTimer,
                                         String text, List<Mention> listOfMentions, JSONArray buttons) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendText", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("text", text);
        // composite message attributes
        if (buttons != null) {
            requestBody.put("buttons", buttons);
        }
        if (messageTimer.asMillis() > 0) {
            requestBody.put("messageTimer", messageTimer.asMillis());
        }
        JSONArray mentions = new JSONArray();
        for (Mention mention : listOfMentions) {
            JSONObject object = new JSONObject();
            object.put("length", mention.getLength());
            object.put("start", mention.getStart());
            object.put("userId", mention.getUserId());
            if (!convoDomain.equals("staging.zinfra.io")) {
                object.put("userDomain", mention.getUserDomain());
            }
            mentions.put(object);
        }
        requestBody.put("mentions", mentions);
        sendHttpRequest(connection, requestBody);
    }

    public void sendReply(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta messageTimer,
                          String text, String messageId, String hash) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendText", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("text", text);
        if (messageTimer.asMillis() > 0) {
            requestBody.put("messageTimer", messageTimer.asMillis());
        }
        JSONObject quote = new JSONObject();
        quote.put("quotedMessageId", messageId);
        quote.put("quotedMessageSha256", hash);
        requestBody.put("quote", quote);
        sendHttpRequest(connection, requestBody);
    }

    public void sendLocation(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain,
                             Timedelta timeout, float longitude, float latitude, String locationName, int zoom) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendLocation", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("latitude", latitude);
        requestBody.put("locationName", locationName);
        requestBody.put("longitude", longitude);
        if (timeout.asMillis() > 0) {
            requestBody.put("messageTimer", timeout.asMillis());
        }
        requestBody.put("zoom", zoom);
        sendHttpRequest(connection, requestBody);
    }

    public void sendPing(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, Timedelta timeout) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendPing", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        if (timeout.asMillis() > 0) {
            requestBody.put("messageTimer", timeout.asMillis());
        }
        sendHttpRequest(connection, requestBody);
    }

    public String getDeviceId(ClientUser user, String deviceName) {
        // caution: the returned device ids are not zero-padded which means they sometime lack a leading 0
        String instanceId = getInstanceId(user, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s", instanceId), "GET");
        String response = sendHttpRequest(connection, null);
        JSONObject instance = new JSONObject(response);
        return instance.getString("clientId");
    }

    public String getDeviceFingerprint(ClientUser user, String deviceName) {
        String instanceId = getInstanceId(user, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/fingerprint", instanceId), "GET");
        String response = sendHttpRequest(connection, null);
        JSONObject instance = new JSONObject(response);
        return instance.getString("fingerprint");
    }

    public JSONArray getMessages(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/getMessages", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        String response = sendHttpRequest(connection, requestBody);
        return new JSONArray(response);
    }

    public JSONArray getMessageReadReceipts(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, String messageId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/getMessageReadReceipts", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("messageId", messageId);
        String response = sendHttpRequest(connection, requestBody);
        return new JSONArray(response);
    }

    public List<String> getMessageIds(ClientUser user, @Nullable String deviceName, String convoId, String convoDomain) {
        JSONArray messages = getMessages(user, deviceName, convoId, convoDomain);
        List<String> messageIds = new ArrayList<>();
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.getJSONObject(i);
            messageIds.add(message.getString("id"));
        }
        return messageIds;
    }

    public String getMessageIdByText(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, String text) {
        JSONArray messages = getMessages(owner, deviceName, convoId, convoDomain);
        for (Object messageObject : messages) {
            JSONObject message = (JSONObject) messageObject;
            if (message.has("content")) {
                if (text.equals(message.getJSONObject("content").getString("text"))) {
                    return message.getString("id");
                }
            }
        }
        throw new IllegalStateException(String.format("Could not find message with '%s' in messages on Testservice", text));
    }

    public JSONObject getMessage(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, String messageId) {
        JSONArray messages = getMessages(owner, deviceName, convoId, convoDomain);
        for (Object messageObject : messages) {
            JSONObject message = (JSONObject) messageObject;
            if (messageId.equals(message.getString("id"))) {
                return message;
            }
        }
        throw new IllegalStateException(String.format("Could not find message with id '%s' in messages on Testservice", messageId));
    }

    public void sendConfirmationDelivered(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, String messageId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendConfirmationDelivered",
                instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("firstMessageId", messageId);
        sendHttpRequest(connection, requestBody);
    }

    public void sendConfirmationRead(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain, String messageId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendConfirmationRead",
                instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("firstMessageId", messageId);
        sendHttpRequest(connection, requestBody);
    }

    public void sendButtonAction(ClientUser sender, String receiverId, @Nullable String deviceName, String convoId,
                                 String referenceMessageId, String buttonId) {
        String instanceId = getInstanceId(sender, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendButtonAction", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("buttonId", buttonId);
        requestBody.put("referenceMessageId", referenceMessageId);
        requestBody.put("userIds", new String[]{ receiverId });
        requestBody.put("conversationId", convoId);
        sendHttpRequest(connection, requestBody);
    }

    public void sendButtonActionConfirmation(ClientUser owner, String receiverId, @Nullable String deviceName, String convoId,
                                             String referenceMessageId, String buttonId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendButtonActionConfirmation", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("buttonId", buttonId);
        requestBody.put("referenceMessageId", referenceMessageId);
        requestBody.put("userIds", new String[]{ receiverId });
        requestBody.put("conversationId", convoId);
        sendHttpRequest(connection, requestBody);
    }

    public void toggleReaction(ClientUser owner, @Nullable String deviceName, String convoId, String convoDomain,
                             String originalMessageId, String reaction) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendReaction", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("originalMessageId", originalMessageId);
        requestBody.put("type", reaction);
        sendHttpRequest(connection, requestBody);
    }

    public void sendEphemeralConfirmationDelivered(ClientUser owner, @Nullable String deviceName, String convoId,
                                                   String convoDomain, String messageId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s" +
                "/sendEphemeralConfirmationDelivered", instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        if (!convoDomain.equals("staging.zinfra.io")) {
            requestBody.put("conversationDomain", convoDomain);
        }
        requestBody.put("firstMessageId", messageId);
        sendHttpRequest(connection, requestBody);
    }

    public void breakSession(ClientUser owner, String deviceName, ClientUser user, String userDomain, String deviceId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/breakSession",
                instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("userId", user.getId());
        if (!userDomain.equals("staging.zinfra.io")) {
            requestBody.put("userDomain", userDomain);
        }
        requestBody.put("clientId", deviceId);
        sendHttpRequest(connection, requestBody);
    }

    public void resetSession(ClientUser owner, String deviceName, String convoId) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/sendSessionReset",
                instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversationId", convoId);
        sendHttpRequest(connection, requestBody);
    }

    public void createConversation(ClientUser owner, List<ClientUser> participants, String chatName,
                                   String deviceName) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s/conversation",
                instanceId), "POST");
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", chatName);
        List<String> userIds = participants
                .stream()
                .map(user -> String.format(
                        "%s@%s",
                        user.getId(),
                        BackendConnections.get(user.getBackendName()).getDomain())
                )
                .collect(Collectors.toList());
        requestBody.put("userIds", new JSONArray(userIds));
        sendHttpRequest(connection, requestBody);
    }

    public boolean isKaliumTestservice() {
        final HttpURLConnection c = buildRequest("api/v1/", "GET");
        String response = "";
        int status = -1;
        try {
            log.info(String.format("%s: %s", c.getRequestMethod(), c.getURL()));
            status = c.getResponseCode();
            return (status == HttpStatus.SC_NOT_FOUND);
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            String error = String.format("%s (%s): %s", e.getMessage(), status, response);
            log.severe(error);
            throw new HttpRequestException(error);
        } finally {
            c.disconnect();
        }
    }

    public void cleanUp(ClientUser owner, @Nullable String deviceName) {
        String instanceId = getInstanceId(owner, deviceName);
        final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s", instanceId), "DELETE");
        sendHttpRequest(connection, null);
    }

    public void cleanUp() {
        final List<String> allInstanceIds = new ArrayList<>();
        userAliases.values().forEach(x -> allInstanceIds.addAll(x.values()));
        userAliases.clear();
        for (String instanceId : allInstanceIds) {
            final HttpURLConnection connection = buildRequest(String.format("api/v1/instance/%s", instanceId), "DELETE");
            sendHttpRequest(connection, null);
        }
    }

    public List<String> getUserDevices(ClientUser owner) {
        return new ArrayList<>(userAliases.getOrDefault(owner.getName(), Collections.emptyMap()).keySet());
    }

    private String getInstanceId(ClientUser user, @Nullable String deviceName) {
        log.info("Looking for device:" + deviceName);
        if (deviceName == null) {
            Map<String, String> devices = userAliases.get(user.getName());
            if (devices == null) {
                deviceName = "Device1";
                log.info("No device found yet. Creation new one called " + deviceName);
                Backend backend = BackendConnections.get(user);
                boolean developmentApiEnabled = backend.isDevelopmentApiEnabled(user);
                String verificationCode = null;
                if (backend.getBackendName().contains("bund")) {
                    verificationCode = backend.getVerificationCode(user);
                }
                login(user, verificationCode, deviceName, developmentApiEnabled);
            } else {
                deviceName = devices.keySet().iterator().next();
            }
        }
        String instanceId = userAliases.computeIfAbsent(user.getName(), (key) -> new ConcurrentHashMap<>()).get(deviceName);
        if (instanceId == null) {
            throw new RuntimeException(String.format("No pre-created device %s for user %s found. Maybe you forgot to add the" +
                    " step to explicitly add this device?", deviceName, user.getName()));
        }
        return instanceId;
    }

    private void printAll() {
        userAliases.forEach(
                (userAlias, devices) -> devices.forEach(
                        (deviceName, instanceId) -> log.info(String.format("%s: %s -> %s", userAlias, deviceName, instanceId))
                )
        );
    }

    private static String fileToBase64String(File srcFile) {
        if (!srcFile.exists()) {
            throw new IllegalArgumentException(
                    String.format("The file at path '%s' is not accessible or does not exist",
                            srcFile.getAbsolutePath())
            );
        }
        final byte[] asBytes;
        try {
            asBytes = Files.readAllBytes(srcFile.toPath());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return Base64.encodeBase64String(asBytes);
    }
}
