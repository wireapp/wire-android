package com.wearezeta.auto.common.backend;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;

import com.wearezeta.auto.common.ImageUtil;
import com.wearezeta.auto.common.backend.models.*;
import com.wearezeta.auto.common.backend.models.AssetV3;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.email.handlers.InbucketMailbox;
import com.wearezeta.auto.common.email.inbucket.models.Message;
import com.wearezeta.auto.common.email.messages.ActivationMessage;
import com.wearezeta.auto.common.legalhold.LegalHoldServiceSettings;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.service.ServiceInfo;
import com.wearezeta.auto.common.usrmgmt.AccessCookie;
import com.wearezeta.auto.common.usrmgmt.AccessToken;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.wearezeta.auto.common.CommonUtils.uriEncode;
import static java.net.HttpURLConnection.*;

public class Backend {

    private static final Logger log = Logger.getLogger(Backend.class.getSimpleName());

    public static final String PROFILE_PICTURE_JSON_ATTRIBUTE = "complete";
    public static final String PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE = "preview";

    private final String name;
    private final String backendUrl;
    private final String webappUrl;
    private final String backendWebsocket;
    private final BasicAuth basicAuth;
    private final BasicAuth inbucketAuth;
    private final String domain;
    private final String deeplink;
    private final String inbucketUrl;
    private final String keycloakUrl;
    private final String acmeDiscoveryUrl;
    private final String k8sNamespace;
    private final String socksProxy;
    private final Proxy proxy;

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

    public Backend(String name, String backendUrl, @Nullable String webappUrl, @Nullable String domain,
                   String backendWebsocket, @Nullable String deeplink, String inbucketUrl, String keycloakUrl,
                   String acmeDiscoveryUrl, String k8sNamespace, BasicAuth basicAuth, BasicAuth inbucketAuth,
                   boolean insecure, String socksProxy) {
        this.name = name;
        this.backendUrl = backendUrl;
        this.webappUrl = webappUrl;
        this.backendWebsocket = backendWebsocket;
        this.basicAuth = basicAuth;
        this.inbucketAuth = inbucketAuth;
        this.domain = domain;
        this.deeplink = deeplink;
        this.inbucketUrl = inbucketUrl;
        this.keycloakUrl = keycloakUrl;
        this.acmeDiscoveryUrl = acmeDiscoveryUrl;
        this.k8sNamespace = k8sNamespace;
        this.socksProxy = socksProxy;

        if (socksProxy != null && !socksProxy.isEmpty()) {
            Authenticator authenticator = new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication("qa",
                            Credentials.get("SOCKS_PROXY_PASSWORD").toCharArray()));
                }
            };
            this.proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.wire.link", 1080));
            Authenticator.setDefault(authenticator);
        } else {
            this.proxy = null;
        }

        if (insecure) {
            // Install the all-trusting trust manager
            try {
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (Exception e) {
                log.severe("Could not install all-trusting trust manager: " + e.getMessage());
            }
        }
    }

    public String getBackendName() {
        return this.name;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public String getWebappUrl() {
        return webappUrl;
    }

    public String getDomain() {
        return domain;
    }

    public String getDeeplinkForAndroid() {
        if (deeplink == null) {
            throw new RuntimeException("No known deeplink URL for " + getBackendName());
        }
        return String.format("wire://access/?config=%s", deeplink);
    }

    public String getDeeplinkForiOS(String protocolHandler) {
        if (deeplink == null) {
            throw new RuntimeException("No known deeplink URL for " + getBackendName());
        }
        return String.format(protocolHandler + "://access/?config=%s", deeplink);
    }

    public String getDeeplinkUrl() {
        if (deeplink == null) {
            throw new RuntimeException("No known deeplink URL for " + getBackendName());
        }
        return deeplink;
    }

    public String getBackendWebsocket() {
        return backendWebsocket;
    }

    private URL getBackendUrl(String path) {
        try {
            return new URL(backendUrl + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasInbucketSetup() {
        return inbucketUrl != null;
    }

    public String getInbucketUrl() {
        return inbucketUrl;
    }

    public String getKeycloakUrl() {
        return keycloakUrl;
    }

    public String getAcmeDiscoveryUrl() {
        return acmeDiscoveryUrl;
    }

    public String getK8sNamespace() {
        if (k8sNamespace == null) {
            throw new RuntimeException("Backend is missing its namespace. Use 'kubectl get namespaces' to find out!");
        }
        return k8sNamespace;
    }

    public String getBasicAuthUser() {
        return basicAuth.getUser();
    }

    public String getBasicAuthPassword() {
        return basicAuth.getPassword();
    }

    public String getInbucketUsername() {
        return inbucketAuth.getUser();
    }

    public String getInbucketPassword() {
        return inbucketAuth.getPassword();
    }

    public boolean useProxy() {
        return socksProxy != null;
    }

    // region Backend features

    public boolean isFeatureSFTEnabled() {
        if (this.name.equals("qa-column-1")) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isFeatureEncryptionAtRestEnabled() {
        if (this.name.equals("qa-column-1") || this.name.equals("qa-column-3")) {
            return true;
        } else {
            return false;
        }
    }

    // endregion

    // region HTTP connection logic

    private HttpURLConnection buildDefaultRequestOnBackdoor(String path, String mediaType) {
        URL url = getBackendUrl(path);
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) (socksProxy != null ? url.openConnection(proxy) : url.openConnection());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        c.setRequestProperty("Content-Type", mediaType);
        c.setRequestProperty("Accept", mediaType);
        c.setRequestProperty("Authorization", basicAuth.getEncoded());
        return c;
    }

    private HttpRequest.Builder buildNewDefaultRequestOnBackdoor(String path, String mediaType) {
        URL url = getBackendUrl(path);
        try {
            return HttpRequest.newBuilder()
                    .uri(url.toURI())
                    .header("Content-Type", mediaType)
                    .header("Accept", mediaType)
                    .header("Authorization", basicAuth.getEncoded());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    private HttpURLConnection buildDefaultRequest(String path, String mediaType) {
        URL url = getBackendUrl(path);
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) (socksProxy != null ? url.openConnection(proxy) : url.openConnection());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        c.setRequestProperty("Content-Type", mediaType);
        c.setRequestProperty("Accept", mediaType);
        return c;
    }

    private HttpURLConnection buildDefaultRequestWithAuth(String path, AccessToken token) {
        return buildDefaultRequestWithAuth(path, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, token, null);
    }

    private HttpURLConnection buildDefaultRequestWithAuth(String path, AccessToken token, AccessCookie cookie) {
        return buildDefaultRequestWithAuth(path, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, token, cookie);
    }

    private HttpURLConnection buildDefaultRequestWithAuth(String path, String contentType, String acceptType,
                                                          AccessToken token, AccessCookie cookie) {
        URL url = getBackendUrl(path);
        HttpURLConnection c;
        try {
            c = (HttpURLConnection) (socksProxy != null ? url.openConnection(proxy) : url.openConnection());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        c.setRequestProperty("Content-Type", contentType);
        c.setRequestProperty("Accept", Objects.requireNonNullElse(acceptType, "*/*"));
        String header = String.format("%s %s", token.getType(), token.getValue());
        log.fine("Authorization: " + header);
        c.setRequestProperty("Authorization", header);
        if (cookie != null) {
            log.fine("Cookie set");
            c.setRequestProperty("Cookie", cookie.getName() + "=" + cookie.getValue());
        }
        return c;
    }

    // TODO: The request code needs to be put into separate class

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

    private int getResponseCodeForHttpGet(HttpURLConnection c) {
        String response = "";
        int status = -1;
        try {
            log.info("GET " + c.getURL());
            c.setRequestMethod("GET");
            logHttpRequestProperties(c);
            status = c.getResponseCode();
            response = readStream(c.getInputStream());
            logResponseAndStatusCode(response, status);
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            logResponseAndStatusCode(response, status);
        } finally {
            c.disconnect();
        }
        return status;
    }

    private BufferedImage httpGetImage(HttpURLConnection c, int[] acceptableResponseCodes) {
        int status = -1;
        try {
            log.info("GET " + c.getURL());
            c.setRequestMethod("GET");
            logHttpRequestProperties(c);
            status = c.getResponseCode();
            return ImageIO.read(c.getInputStream());
        } catch (IOException e) {
            try {
                String response = readStream(c.getErrorStream());
                String error = String.format("%s (%s): %s", e.getMessage(), status, response);
                log.severe(error);
                throw new HttpRequestException(error, status);
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
                return null;
            }
        } finally {
            c.disconnect();
        }
    }

    private int getResponseCodeForHttpPost(HttpURLConnection c, String requestBody) {
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
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            logResponseAndStatusCode(response, status);
        } finally {
            c.disconnect();
        }
        return status;
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
                String error = String.format("%s (%s): %s", e.getMessage(), status, response);
                log.severe(error);
                throw new HttpRequestException(error, status);
            }
        } finally {
            c.disconnect();
        }
    }

    private String httpPost(HttpURLConnection c, byte[] requestBody, int[] acceptableResponseCodes) {
        String response = "";
        int status = -1;
        try {
            log.info("POST " + c.getURL());
            c.setRequestMethod("POST");
            logHttpRequestProperties(c);
            log.fine(" >>> Request: byte[]");
            c.setDoOutput(true);
            DataOutputStream out = new DataOutputStream(c.getOutputStream());
            out.write(requestBody);
            out.flush();
            out.close();
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

    private int getResponseCodeForHttpPut(HttpURLConnection c, String requestBody) {
        String response = "";
        int status = -1;
        try {
            log.info("POST " + c.getURL());
            c.setRequestMethod("PUT");
            logHttpRequestProperties(c);
            logRequest(requestBody);
            c.setDoOutput(true);
            writeStream(requestBody, c.getOutputStream());
            status = c.getResponseCode();
            response = readStream(c.getInputStream());
            logResponseAndStatusCode(response, status);
        } catch (IOException e) {
            try {
                response = readStream(c.getErrorStream());
            } catch (IOException ex) {
                log.fine("Could not read error stream: " + e.getMessage());
            }
            logResponseAndStatusCode(response, status);
        } finally {
            c.disconnect();
        }
        return status;
    }

    private String httpPut(HttpURLConnection c, String requestBody, int[] acceptableResponseCodes) {
        String response = "";
        int status = -1;
        try {
            log.info("PUT " + c.getURL());
            c.setRequestMethod("PUT");
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
            String error = String.format("%s (%s): %s", e.getMessage(), status, response);
            log.severe(error);
            throw new HttpRequestException(error, status);
        } finally {
            c.disconnect();
        }
    }

    private String httpPatch(String path, String mediaType, String requestBody, int[] acceptableResponseCodes) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient().newBuilder();

        if (socksProxy != null && !socksProxy.isEmpty()) {
            Authenticator authenticator = new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication("qa",
                            com.wearezeta.auto.common.credentials.Credentials.get("SOCKS_PROXY_PASSWORD").toCharArray()));
                }
            };
            Authenticator.setDefault(authenticator);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.wire.link", 1080));
            clientBuilder.proxy(proxy);
        }

        OkHttpClient client = clientBuilder.build();
        URL url = getBackendUrl(path);
        try {
            log.info("PATCH " + url);
            RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json"), requestBody);

            // Build the PATCH request
            Request request = new Request.Builder()
                    .url(url)  // Replace with your URL
                    .patch(body)
                    .addHeader("Content-Type", mediaType)
                    .addHeader("Accept", mediaType)
                    .addHeader("Authorization", basicAuth.getEncoded())
                    .build();
            logRequest(requestBody);
            Response response = client.newCall(request).execute();
            int status = response.code();
            String responseBody = response.body().string();
            logResponseAndStatusCode(responseBody, status);
            assertResponseCode(status, acceptableResponseCodes);
            return responseBody;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String httpDelete(HttpURLConnection c, String requestBody, int[] acceptableResponseCodes) {
        String response = "";
        int status = -1;
        try {
            log.info("DELETE " + c.getURL());
            c.setRequestMethod("DELETE");
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

    private String httpDelete(HttpURLConnection c, int[] acceptableResponseCodes) {
        String response = "";
        int status = -1;
        try {
            log.info("DELETE " + c.getURL());
            c.setRequestMethod("DELETE");
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

    // endregion HTTP connection logic

    // region Retries

    private <T> T retryOnBackendFailure(Supplier<T> r) {
        int ntry = 1;
        HttpRequestException savedException = null;
        while (ntry <= 2) {
            try {
                return r.get();
            } catch (HttpRequestException e) {
                savedException = e;
                Timedelta.ofMillis(2000 * ntry).sleep();
            }
            ntry++;
        }
        throw savedException;
    }

    private static <T> T retryOnBackendFailure(Timedelta timeout, Timedelta interval, Supplier<T> r) {
        final Timedelta started = Timedelta.now();
        int ntry = 1;
        HttpRequestException savedException = null;
        do {
            try {
                return r.get();
            } catch (HttpRequestException e) {
                savedException = e;
                Timedelta.ofMillis(interval.asMillis() * ntry).sleep();
            }
            ntry++;
        } while (Timedelta.now().isDiffLessOrEqual(started, timeout));
        throw savedException;
    }

    // endregion Retries

    /*
     * Business logic
     */

    // region User creation and registration

    public ClientUser createPersonalUserViaBackdoor(ClientUser user) {
        HttpURLConnection c = buildDefaultRequest("register", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", user.getEmail());
        requestBody.put("name", user.getName());
        requestBody.put("password", user.getPassword());
        String response = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        JSONObject object = new JSONObject(response);
        user.setId(object.getString("id"));
        String cookiesHeader = c.getHeaderField("Set-Cookie");
        List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
        AccessCookie cookie = new AccessCookie("zuid", cookies);
        user.setAccessCredentials(new AccessCredentials(null, cookie));
        String activationCode = getActivationCodeForEmail(user.getEmail());
        activateRegisteredEmailByBackdoorCode(user.getEmail(), activationCode);
        return user;
    }

    public ClientUser createWirelessUserViaBackdoor(ClientUser user) {
        HttpURLConnection c = buildDefaultRequest("register", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", user.getName());
        if (user.getExpiresIn() != null) {
            requestBody.put("expires_in", user.getExpiresIn().getSeconds());
        }
        String response = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        JSONObject object = new JSONObject(response);
        user.setId(object.getString("id"));
        String cookiesHeader = c.getHeaderField("Set-Cookie");
        List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
        AccessCookie cookie = new AccessCookie("zuid", cookies);
        user.setAccessCredentials(new AccessCredentials(null, cookie));
        return user;
    }

    public ClientUser createTeamOwnerViaBackdoor(ClientUser user, String teamName, String locale,
                                                 boolean updateHandle) {
        bookEmail(user.getEmail());
        final String activationCode = getActivationCodeForEmail(user.getEmail());
        HttpURLConnection c = buildDefaultRequest("register", MediaType.APPLICATION_JSON);
        final JSONObject requestBody = new JSONObject();
        requestBody.put("email", user.getEmail());
        requestBody.put("name", user.getName());
        requestBody.put("locale", locale);
        requestBody.put("password", user.getPassword());
        Optional.ofNullable(activationCode).map(x -> requestBody.put("email_code", x));
        final JSONObject team = new JSONObject();
        team.put("name", teamName);
        team.put("icon", "default");
        team.put("binding", true);
        requestBody.put("team", team);
        final String response = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        JSONObject object = new JSONObject(response);
        user.setId(object.getString("id"));
        user.setTeamId(object.getString("team"));
        String cookiesHeader = c.getHeaderField("Set-Cookie");
        List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
        AccessCookie cookie = new AccessCookie("zuid", cookies);
        user.setAccessCredentials(new AccessCredentials(null, cookie));
        updateUserPicture(user);
        if (updateHandle) {
            updateUniqueUsername(user, user.getUniqueUsername());
        }
        return user;
    }

    public ClientUser createTeamUserViaBackdoor(ClientUser teamOwner, String teamId, ClientUser member,
                                                final boolean uploadPicture, final boolean hasHandle, String role) {
        final String invitationId = inviteNewUserToTeam(receiveAuthToken(teamOwner), teamId,
                member.getEmail(), teamOwner.getName(), role);
        final String invitationCode = getTeamCodeViaBackdoor(teamId, invitationId);
        HttpURLConnection c = buildDefaultRequest("register", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", member.getEmail());
        requestBody.put("name", member.getName());
        requestBody.put("password", member.getPassword());
        requestBody.put("team_code", invitationCode);
        final String response = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        JSONObject object = new JSONObject(response);
        member.setId(object.getString("id"));
        member.setTeamId(teamId);
        String cookiesHeader = c.getHeaderField("Set-Cookie");
        List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
        AccessCookie cookie = new AccessCookie("zuid", cookies);
        member.setAccessCredentials(new AccessCredentials(null, cookie));
        if (uploadPicture) {
            updateUserPicture(member);
        }
        if (hasHandle) {
            updateUniqueUsername(member, member.getUniqueUsername());
        }
        return member;
    }

    public ClientUser acceptInviteViaBackdoor(String teamId, ClientUser member) {
        final String invitationCode = getTeamCodeViaBackdoor(teamId, member.getId());
        HttpURLConnection c = buildDefaultRequest("register", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", member.getEmail());
        requestBody.put("name", member.getName());
        requestBody.put("password", member.getPassword());
        requestBody.put("team_code", invitationCode);
        final String response = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        JSONObject object = new JSONObject(response);
        member.setId(object.getString("id"));
        member.setTeamId(teamId);
        String cookiesHeader = c.getHeaderField("Set-Cookie");
        List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
        AccessCookie cookie = new AccessCookie("zuid", cookies);
        member.setAccessCredentials(new AccessCredentials(null, cookie));
        return member;
    }

    private String inviteNewUserToTeam(AccessToken token, String teamId, String dstEmail, String inviterName, String role) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/invitations", teamId), token);
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", dstEmail);
        requestBody.put("role", role);
        requestBody.put("inviter_name", inviterName);
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_CREATED});
        return new JSONObject(output).getString("id");
    }

    private String getTeamCodeViaBackdoor(String teamId, String invitationId) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/invitation-code?team=%s&invitation_id=%s", uriEncode(teamId), uriEncode(invitationId)),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output).getString("code");
    }

    // endregion User creation and registration

    // region Authentication and user token

    private AccessToken receiveAuthToken(ClientUser user) {
        return getTokenIfExpired(user).getAccessToken();
    }

    private AccessCredentials getTokenIfExpired(ClientUser user) {
        if (user.getAccessCredentialsWithoutRefresh() == null) {
            AccessCredentials newCredentials = login(user);
            user.setAccessCredentials(newCredentials);
            return newCredentials;
        }
        if (user.getAccessCredentialsWithoutRefresh().getAccessToken() == null
                || user.getAccessCredentialsWithoutRefresh().getAccessToken().isInvalid()
                || user.getAccessCredentialsWithoutRefresh().getAccessToken().isExpired()) {
            AccessCredentials newCredentials = access(user.getAccessCredentialsWithoutRefresh());
            user.setAccessCredentials(newCredentials);
            return newCredentials;
        }
        return user.getAccessCredentialsWithoutRefresh();
    }

    public AccessCredentials login(ClientUser user) {
        HttpURLConnection c = buildDefaultRequest("login", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", user.getEmail());
        requestBody.put("password", user.getPassword());
        requestBody.put("label", "");
        String response = httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_FORBIDDEN});
        try {
            if (c.getResponseCode() == 403) {
                if(this.getInbucketUrl()  == null) {
                    throw new IOException("Received 403 for 2FA but no inbucket url present - check your backend settings");
                }
                // This is probably a user with 2FA
                trigger2FA(user.getEmail());
                InbucketMailbox inbucket = new InbucketMailbox(this, user.getEmail());
                List<Message> recentMessages = inbucket.getRecentMessages(user.getEmail());

                Message lastMessage = recentMessages.get(recentMessages.size()-1);
                String verificationCode = lastMessage.getHeader().get(ActivationMessage.ZETA_CODE_HEADER_NAME).get(0);

                HttpURLConnection c2 = buildDefaultRequest("login", MediaType.APPLICATION_JSON);
                JSONObject requestBodyLogin = new JSONObject();
                requestBodyLogin.put("email", user.getEmail());
                requestBodyLogin.put("password", user.getPassword());
                requestBodyLogin.put("verification_code", verificationCode);
                response = httpPost(c2, requestBodyLogin.toString(), new int[]{HTTP_OK});
                JSONObject object = new JSONObject(response);
                String cookiesHeader = c2.getHeaderField("Set-Cookie");
                List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
                AccessCookie cookie = new AccessCookie("zuid", cookies);
                AccessToken token = new AccessToken(object.getString("access_token"), object.getString("token_type"),
                        object.getLong("expires_in"));
                return new AccessCredentials(token, cookie);
            } else {
                // get access credentials from response
                JSONObject object = new JSONObject(response);
                String cookiesHeader = c.getHeaderField("Set-Cookie");
                List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
                AccessCookie cookie = new AccessCookie("zuid", cookies);
                AccessToken token = new AccessToken(object.getString("access_token"), object.getString("token_type"),
                        object.getLong("expires_in"));
                return new AccessCredentials(token, cookie);
            }
        } catch (IOException e) {
            throw new HttpRequestException("Login failed: " + e.getMessage());
        }
    }

    public boolean isLoginPossible(String email, String password) {
        HttpURLConnection c = buildDefaultRequest("login", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);
        requestBody.put("password", password);
        requestBody.put("label", "");
        return getResponseCodeForHttpPost(c, requestBody.toString()) == 200;
    }

    public AccessCredentials access(AccessCredentials accessCredentials) {
        HttpURLConnection c;
        if (accessCredentials.getAccessToken() != null) {
            c = buildDefaultRequestWithAuth("access", accessCredentials.getAccessToken());
        } else {
            c = buildDefaultRequest("access", MediaType.APPLICATION_JSON);
        }
        c.setRequestProperty("Cookie", String.format("zuid=%s", accessCredentials.getAccessCookie().getValue()));
        JSONObject requestBody = new JSONObject();
        requestBody.put("withCredentials", true);
        String response = httpPost(c, requestBody.toString(), new int[]{HTTP_OK});
        JSONObject object = new JSONObject(response);
        String cookiesHeader = c.getHeaderField("Set-Cookie");
        AccessCookie cookie = null;
        if (cookiesHeader != null) {
            List<HttpCookie> cookies = HttpCookie.parse(cookiesHeader);
            if (cookies.stream().anyMatch(x -> x.getName().equals("zuid"))) {
                cookie = new AccessCookie("zuid", cookies);
            }
        }
        if (cookie == null) {
            cookie = accessCredentials.getAccessCookie();
        }
        AccessToken token = new AccessToken(object.getString("access_token"), object.getString("token_type"),
                object.getLong("expires_in"));
        return new AccessCredentials(token, cookie);
    }

    public void logout(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("access/logout", receiveAuthToken(user),
                user.getAccessCredentialsWithoutRefresh().getAccessCookie());
        JSONObject requestBody = new JSONObject();
        httpPost(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Authentication and user token

    // region User deletion

    public void deleteUser(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self", receiveAuthToken(user));
        JSONObject requestBody = new JSONObject();
        requestBody.put("password", user.getPassword());
        httpDelete(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void triggerDeleteEmail(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self", receiveAuthToken(user));
        JSONObject requestBody = new JSONObject();
        httpDelete(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_ACCEPTED});
    }

    // endregion User deletion

    // region Email + Activation

    private void activateRegisteredEmailByBackdoorCode(String email, String code) {
        HttpURLConnection c = buildDefaultRequest("activate", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);
        requestBody.put("code", code);
        requestBody.put("dryrun", false);
        httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
        log.fine(String.format("User '%s' is successfully activated", email));
    }

    private void bookEmail(String email) {
        HttpURLConnection c = buildDefaultRequest("activate/send", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);
        httpPost(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public String getActivationCodeForEmail(String email) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/users/activation-code?email=%s", uriEncode(email)), MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output).getString("code");
    }

    public void activateEmailAfterEmailChange(String key, String code) {
        HttpURLConnection c = buildDefaultRequest(
                String.format("activate?code=%s&key=%s", code, key),
                MediaType.APPLICATION_JSON);
        httpGet(c, new int[]{HTTP_OK});
    }

    public void triggerUserEmailChange(ClientUser user, String newEmail) {
        updateSelfEmail(user, newEmail);
    }

    public String getVerificationCode(ClientUser user) {
        trigger2FA(user.getEmail());
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/users/%s/verification-code/login", uriEncode(user.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return output.replace("\"", "");
    }

    private void trigger2FA(String email) {
        HttpURLConnection c = buildDefaultRequest("v5/verification-code/send", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("action", "login");
        requestBody.put("email", email);
        httpPost(c, requestBody.toString(), new int[]{HTTP_OK, 429});
    }

    public void changeUserEmail(ClientUser user, String newEmail) {
        updateSelfEmail(user, newEmail);
        final String activationCode = getActivationCodeForEmail(newEmail);
        activateRegisteredEmailByBackdoorCode(newEmail, activationCode);
    }

    private void updateSelfEmail(ClientUser user, String newEmail) {
        HttpURLConnection c = buildDefaultRequestWithAuth("access/self/email", receiveAuthToken(user), user.getAccessCredentialsWithoutRefresh().getAccessCookie());
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", newEmail);
        httpPut(c, requestBody.toString(), new int[]{HTTP_ACCEPTED});
    }

    public void detachSelfEmail(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("access/self/email", receiveAuthToken(user), user.getAccessCredentialsWithoutRefresh().getAccessCookie());
        httpDelete(c, new int[]{HTTP_OK});
    }

    public Optional<String> getEmail(ClientUser user) {
        final JSONObject userInfo = getUserInfo(receiveAuthToken(user));
        if (userInfo.has("email")) {
            return Optional.of(userInfo.getString("email"));
        }
        return Optional.empty();
    }

    // endregion Email + Activation

    // region Password

    public void changeUserPassword(ClientUser user, String oldPassword, String newPassword) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self/password", receiveAuthToken(user));
        JSONObject requestBody = new JSONObject();
        if (oldPassword != null) {
            requestBody.put("old_password", oldPassword);
        }
        requestBody.put("new_password", newPassword);
        httpPut(c, requestBody.toString(), new int[]{HTTP_ACCEPTED, HTTP_OK});
        user.setPassword(newPassword);
    }

    // endregion

    // region Id

    public String getUserId(ClientUser user) {
        JSONObject object = getUserInfo(receiveAuthToken(user));
        return object.getString("id");
    }

    // endregion Id

    // region Name

    public String getName(ClientUser user) {
        final JSONObject userInfo = getUserInfo(receiveAuthToken(user));
        return userInfo.getString("name");
    }

    public String getUserNameByID(String domain, String id, ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("users/%s/%s/", domain, id), receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output).getString("name");
    }

    public void updateName(ClientUser user, String newName) {
        updateSelfInfo(receiveAuthToken(user), Optional.empty(), Optional.of(newName));
        user.setName(newName);
    }

    // endregion Name

    // region Email

    public String getUserEmailByID(String id, ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("users/%s", id), receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK});
        try {
            return new JSONObject(output).getString("email");
        } catch (JSONException ex) {
            log.warning("User email was not found");
            return null;
        }
    }

    // endregion Email

    // region Unique Username (@ handle)

    public Optional<String> getUniqueUsername(ClientUser user) {
        final JSONObject userInfo = getUserInfo(receiveAuthToken(user));
        if (userInfo.has("handle")) {
            return Optional.of(userInfo.getString("handle"));
        }
        return Optional.empty();
    }

    public Optional<String> getUniqueUsernameByID(String id, ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("users/" + id, receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK});
        final JSONObject userInfo = new JSONObject(output);
        if (userInfo.has("handle")) {
            return Optional.of(userInfo.getString("handle"));
        }
        return Optional.empty();
    }

    public void updateUniqueUsername(ClientUser user, String newUniqueUsername) {
        final int USERNAME_ALREADY_REGISTERED_ERROR = 409;
        final boolean tryAvoidDuplicates = newUniqueUsername.equalsIgnoreCase(user.getUniqueUsername());
        int ntry = 0;
        while (true) {
            try {
                updateSelfHandle(receiveAuthToken(user), newUniqueUsername);
                user.setUniqueUsername(newUniqueUsername);
                return;
            } catch (HttpRequestException e) {
                if (tryAvoidDuplicates && e.getReturnCode() == USERNAME_ALREADY_REGISTERED_ERROR && ntry < 5) {
                    // Try to generate another handle if this one already exists
                    newUniqueUsername = ClientUser.sanitizedRandomizedHandle(user.getFirstName());
                } else {
                    throw e;
                }
            }
            ntry++;
        }
    }

    private void updateSelfHandle(AccessToken token, String handle) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self/handle", token);
        JSONObject requestBody = new JSONObject();
        requestBody.put("handle", handle);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Unique Username (@ handle)

    // region User Profile info

    private JSONObject getUserInfo(AccessToken token) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self", token);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    private void updateSelfInfo(AccessToken token,
                                Optional<Integer> accentId,
                                Optional<String> name) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self", token);
        JSONObject requestBody = new JSONObject();
        accentId.ifPresent(x -> requestBody.put("accent_id", x));
        name.ifPresent(x -> requestBody.put("name", x));
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void changeUserLocale(ClientUser forUser, String newLocale) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self/locale", receiveAuthToken(forUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("locale", newLocale);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public Optional<JSONObject> getPropertyValue(ClientUser user, String pathKey) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("properties/%s", pathKey), receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK, HTTP_NOT_FOUND});
        if (!output.isEmpty()) {
            return Optional.of(new JSONObject(output));
        } else {
            return Optional.empty();
        }
    }

    public String getStringPropertyValue(ClientUser user, String pathKey) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("properties/%s", pathKey), receiveAuthToken(user));
        return httpGet(c, new int[]{HTTP_OK, HTTP_NOT_FOUND});
    }

    public JSONObject getPropertyValues(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("properties-values", receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK, HTTP_NOT_FOUND});
        if (!output.isEmpty()) {
            return new JSONObject(output);
        } else {
            return new JSONObject();
        }
    }

    public void setPropertyValue(ClientUser user, String propertyKey, JSONObject properties) {
        String propertyValue = properties.toString();
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("properties/%s", propertyKey),
                receiveAuthToken(user));
        httpPut(c, propertyValue, new int[]{HTTP_OK});
    }

    public void setPropertyValue(ClientUser user, String propertyKey, String value) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("properties/%s", propertyKey),
                receiveAuthToken(user));
        httpPut(c, value, new int[]{HTTP_OK});
    }

    // endregion User Profile info

    // region GDPR

    public void disableConsentPopup(ClientUser user) {
        JSONObject privacyProperty = new JSONObject();
        privacyProperty.put("improve_wire", false);
        privacyProperty.put("marketing_consent", false);
        privacyProperty.put("telemetry_data_sharing", false);
        JSONObject settingsProperty = new JSONObject();
        settingsProperty.put("privacy", privacyProperty);
        JSONObject properties = new JSONObject();
        properties.put("settings", settingsProperty);
        setPropertyValue(user, "webapp", properties);
    }

    public Consents getConsents(ClientUser asUser) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self/consent", receiveAuthToken(asUser));
        return Consents.fromJson(httpGet(c,
                new int[]{HTTP_OK, HTTP_NOT_FOUND, HTTP_INTERNAL_ERROR}));
    }

    // endregion GDPR

    // region User Profile Picture

    private void updateUserPicture(ClientUser user) {
        final String DEFAULT_TEAM_AVATAR = "images/default_team_avatar.jpg";
        try (InputStream is = Backend.class.getClassLoader().getResourceAsStream(DEFAULT_TEAM_AVATAR)) {
            updateUserPictureWithIS(user, is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void updateUserPicture(ClientUser user, File picture) {
        if (!picture.exists()) {
            throw new IllegalArgumentException(String.format("The picture at %s does not exist or is not" +
                    " accessible", picture.getAbsolutePath()));
        }
        try (InputStream is = new FileInputStream(picture)) {
            updateUserPictureWithIS(user, is);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void updateUserPictureWithIS(ClientUser user, InputStream picture) {
        final BufferedImage image;
        try {
            image = ImageIO.read(picture);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        updateUserPicture(user, image);
    }

    public void updateUserPicture(ClientUser user, BufferedImage image) {
        final int PROFILE_PREVIEW_MAX_WIDTH = 280;
        final int PROFILE_PREVIEW_MAX_HEIGHT = 280;
        final String PROFILE_PICTURE_JSON_ATTRIBUTE = "complete";
        final String PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE = "preview";

        BufferedImage square = ImageUtil.cropToSquare(image);
        BufferedImage preview = ImageUtil.scaleTo(square, PROFILE_PREVIEW_MAX_WIDTH, PROFILE_PREVIEW_MAX_HEIGHT);
        String previewKey = retryOnBackendFailure(() -> {
            try {
                return uploadAssetV3(receiveAuthToken(user), true, "eternal", ImageUtil.asByteArray(preview));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        String completeKey = retryOnBackendFailure(() -> {
            try {
                return uploadAssetV3(receiveAuthToken(user), true, "eternal", ImageUtil.asByteArray(image));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        Set<AssetV3> assets = new HashSet<>();
        assets.add(new AssetV3(previewKey, "image", PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE));
        assets.add(new AssetV3(completeKey, "image", PROFILE_PICTURE_JSON_ATTRIBUTE));
        retryOnBackendFailure(() -> {
            updateSelfAssets(receiveAuthToken(user), assets);
            return null;
        });
    }

    public void updateSelfAssets(AccessToken token, Set<AssetV3> assets) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self", token);
        JSONObject requestBody = new JSONObject();
        JSONArray array = new JSONArray();
        for (AssetV3 asset : assets) {
            array.put(asset.toJSON());
        }
        requestBody.put("assets", array);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public BufferedImage getUserPicture(ClientUser user, String key) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("assets/v3/%s", key),
                receiveAuthToken(user));
        return httpGetImage(c, new int[]{HTTP_OK});
    }

    public String getUserAssetKey(ClientUser user, String size) {
        final JSONObject userInfo = getUserInfo(receiveAuthToken(user));
        final JSONArray assets = userInfo.getJSONArray("assets");
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (size.equals(asset.getString("size"))) {
                return asset.getString("key");
            }
        }
        throw new IllegalArgumentException("No user asset found with size: " + size + " in " + assets);
    }

    public void removeUserPicture(ClientUser user) {
        retryOnBackendFailure(() -> {
            // v3 assets
            updateSelfAssets(receiveAuthToken(user), new HashSet<>());
            return null;
        });
    }

    // endregion User Profile Picture

    // region Accent color

    public AccentColor getUserAccentColor(ClientUser user) {
        JSONObject response = getUserInfo(receiveAuthToken(user));
        return AccentColor.getById(response.getInt("accent_id"));
    }

    public void updateUserAccentColor(ClientUser user, AccentColor color) {
        updateSelfInfo(receiveAuthToken(user), Optional.of(color.getId()), Optional.empty());
        user.setAccentColor(color);
    }

    // endregion Accent color

    // region Rich Info

    private JSONObject getRichInfo(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("users/%s/rich-info", user.getId()),
                receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void updateRichInfo(ClientUser asUser, String key, String value) {
        JSONObject richInfo = getRichInfo(asUser);
        richInfo = updateRichInfo(richInfo, key, value);
        HttpURLConnection c = buildDefaultRequestOnBackdoor(String.format("i/users/%s/rich-info", asUser.getId()),
                MediaType.APPLICATION_JSON);
        JSONObject request = new JSONObject();
        request.put("rich_info", richInfo);
        httpPut(c, request.toString(), new int[]{HTTP_OK});
    }

    private JSONObject updateRichInfo(JSONObject richInfo, String key, String value) {
        JSONArray fields = richInfo.getJSONArray("fields");
        boolean replaced = false;

        for (int i = 0; i < fields.length(); i++) {
            if (fields.getJSONObject(i).has("type") && fields.getJSONObject(i).getString("type").equals(key)) {
                JSONObject richInfoEntry = new JSONObject();
                richInfoEntry.put("type", key);
                richInfoEntry.put("value", value);
                fields.put(i, richInfoEntry);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            JSONObject richInfoEntry = new JSONObject();
            richInfoEntry.put("type", key);
            richInfoEntry.put("value", value);
            fields.put(richInfoEntry);
        }
        richInfo.put("fields", fields);
        return richInfo;
    }

    public void removeKeyFromRichInfo(ClientUser asUser, String key) {
        JSONObject richInfo = getRichInfo(asUser);
        JSONArray fields = richInfo.getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            if (fields.getJSONObject(i).getString("type").equals(key)) {
                fields.remove(i);
                break;
            }
        }
        richInfo.put("fields", fields);
        HttpURLConnection c = buildDefaultRequestOnBackdoor(String.format("i/users/%s/rich-info", asUser.getId()),
                MediaType.APPLICATION_JSON);
        JSONObject request = new JSONObject();
        request.put("rich_info", richInfo);
        httpPut(c, request.toString(), new int[]{HTTP_OK});
    }

    // endregion Rich Info

    // region SCIM

    public String createSCIMAccessToken(ClientUser asUser, String description) {
        HttpURLConnection c = buildDefaultRequestWithAuth("scim/auth-tokens", receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("description", description);
        requestBody.put("password", asUser.getPassword());
        if (asUser.getVerificationCode() != null) {
            requestBody.put("verification_code", asUser.getVerificationCode());
        }
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_OK});
        return new JSONObject(output).getString("token");
    }

    // endregion SCIM

    // region User Connections

    public void sendConnectionRequest(ClientUser fromUser, ClientUser toUser) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("connections/%s/%s", BackendConnections.get(toUser).getDomain(), toUser.getId()), receiveAuthToken(fromUser));
        JSONObject requestBody = new JSONObject();
        int code = getResponseCodeForHttpPost(c, requestBody.toString());
        if (code == 404) {
            // Use old backend method to connect users if new endpoint is not available
            HttpURLConnection newc = buildDefaultRequestWithAuth("connections", receiveAuthToken(fromUser));
            requestBody = new JSONObject();
            requestBody.put("user", toUser.getId());
            requestBody.put("name", toUser.getName());
            requestBody.put("message", "This message is not shown anywhere anymore");
            httpPost(newc, requestBody.toString(), new int[]{HTTP_OK, HTTP_CREATED});
        } else {
            if (code != 200 && code != 201) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Sleep failed" + e);
                }
                // Retry after 1,5 second wait
                HttpURLConnection retryConnection = buildDefaultRequestWithAuth(String.format("connections/%s/%s", BackendConnections.get(toUser).getDomain(), toUser.getId()), receiveAuthToken(fromUser));
                code = getResponseCodeForHttpPost(retryConnection, requestBody.toString());
                if (code != 200 && code != 201) {
                    throw new RuntimeException("Connection request failed with status code " + code);
                }
            }
        }
        if (code == 422) {
            throw new IllegalStateException("HTTP 422 Invalid body - likely domains are not federated");
        }
    }

    public void acceptAllIncomingConnectionRequests(ClientUser asUser) {
        updateConnections(asUser, ConnectionStatus.Pending, ConnectionStatus.Accepted, Optional.empty());
    }

    public void acceptIncomingConnectionRequest(ClientUser asUser, ClientUser fromUser) {
        updateConnections(asUser, ConnectionStatus.Pending, ConnectionStatus.Accepted,
                Optional.of(Collections.singletonList(fromUser.getId())));
    }

    public void cancelAllOutgoingConnections(ClientUser asUser) {
        updateConnections(asUser, ConnectionStatus.Sent, ConnectionStatus.Cancelled, Optional.empty());
    }

    public void ignoreAllIncomingConnections(ClientUser asUser) {
        updateConnections(asUser, ConnectionStatus.Pending, ConnectionStatus.Ignored, Optional.empty());
    }

    private void updateConnections(ClientUser asUser, ConnectionStatus srcStatus, ConnectionStatus dstStatus,
                                   Optional<List<String>> forUserIds) {
        getAllConnections(asUser).stream().filter(
                        x -> x.getStatus() == srcStatus && (!forUserIds.isPresent() || forUserIds.get().contains(x.getTo())))
                .forEach(x -> {
                    try {
                        changeConnectRequestStatus(asUser, x.getTo(), x.getDomain(), dstStatus);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private List<Connection> getAllConnections(ClientUser user) {
        String pagingState = null;
        JSONObject connectionsInfo;
        final List<Connection> result = new ArrayList<>();
        do {
            connectionsInfo = getConnectionsInfo(receiveAuthToken(user), pagingState);
            final JSONArray connections = connectionsInfo.getJSONArray("connections");
            for (int i = 0; i < connections.length(); i++) {
                result.add(Connection.fromJSON(connections.getJSONObject(i)));
            }
            // Following if statement is needed for older backends
            if (connectionsInfo.has("paging_state")) {
                pagingState = connectionsInfo.getString("paging_state");
            }
        } while (connectionsInfo.getBoolean("has_more"));
        return result;
    }

    private JSONObject getConnectionsInfo(AccessToken token, @Nullable String pagingState) {
        HttpURLConnection c = buildDefaultRequestWithAuth("list-connections", token);

        JSONObject body = new JSONObject();
        body.put("paging_state", pagingState);

        String output = httpPost(c, body.toString(), new int[]{HTTP_OK, HTTP_NOT_FOUND});

        if (output.contains("404 Not Found")) {
            // Use old backend method to connect users if new endpoint is not available
            String requestUri = "connections";
            HttpURLConnection newc = buildDefaultRequestWithAuth(requestUri, token);
            output = httpGet(newc, new int[]{HTTP_OK});
        }
        return new JSONObject(output);
    }

    public void changeConnectRequestStatus(ClientUser asUser, String connectionId, String domain, ConnectionStatus newStatus) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("connections/%s/%s", domain, connectionId), receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", newStatus.toString());
        int code = getResponseCodeForHttpPut(c, requestBody.toString());
        if (code == 404) {
            // Use old backend method to connect users if new endpoint is not available
            HttpURLConnection newc = buildDefaultRequestWithAuth(String.format("connections/%s", connectionId), receiveAuthToken(asUser));
            httpPut(newc, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
        }
    }

    // endregion User Connections

    // region Search

    public void waitUntilContactsFound(ClientUser searchByUser,
                                       String query, int expectedCount, boolean orMore, Timedelta timeout) {
        final Timedelta started = Timedelta.now();
        int currentCount;
        while (Timedelta.now().isDiffLessOrEqual(started, timeout)) {
            JSONObject searchResult;
            try {
                // Changed this to make it look the same as in webapp
                HttpURLConnection c = buildDefaultRequestWithAuth(
                        String.format("search/contacts?q=%s&size=%s&l=3&d=1", uriEncode(query), 30),
                        receiveAuthToken(searchByUser));
                final String output = httpGet(c, new int[]{HTTP_OK});
                searchResult = new JSONObject(output);
            } catch (HttpRequestException e) {
                if (e.getReturnCode() == 500) {
                    Timedelta.ofSeconds(1).sleep();
                    continue;
                } else {
                    throw e;
                }
            }
            if (searchResult.has("documents") && (searchResult.get("documents") instanceof JSONArray)) {
                currentCount = searchResult.getJSONArray("documents").length();
            } else {
                currentCount = 0;
            }
            if (currentCount == expectedCount || (orMore && currentCount >= expectedCount)) {
                return;
            }
            Timedelta.ofSeconds(1).sleep();
        }
        throw new RuntimeException(String.format("%s contact(s) '%s' were not found within %s second(s) " +
                "timeout", expectedCount, query, timeout));
    }

    public boolean doesUserExistForUser(ClientUser searchByUser,
                                        String query) {
        final Timedelta started = Timedelta.now();
        int currentCount;
        while (Timedelta.now().isDiffLessOrEqual(started, Timedelta.ofSeconds(5))) {
            JSONObject searchResult;
            try {
                // Changed this to make it look the same as in webapp
                HttpURLConnection c = buildDefaultRequestWithAuth(
                        String.format("search/contacts?q=%s&size=%s&l=3&d=1", uriEncode(query), 30),
                        receiveAuthToken(searchByUser));
                final String output = httpGet(c, new int[]{HTTP_OK});
                searchResult = new JSONObject(output);
            } catch (HttpRequestException e) {
                if (e.getReturnCode() == 500) {
                    Timedelta.ofSeconds(1).sleep();
                    continue;
                } else {
                    throw e;
                }
            }
            currentCount = searchResult.getInt("found");
            if (currentCount == 1) {
                return true;
            } else if (currentCount > 1) {
                throw new RuntimeException(String.format("Given query is not specific enough, %s users found",
                        currentCount));
            }
            Timedelta.ofSeconds(1).sleep();
        }
        return false;
    }
    // endregion Search

    // region Assets

    public String uploadAssetV3(AccessToken token, boolean isPublic, String retention, byte[] content) {
        final String BOUNDARY = "frontier";
        Base64.Encoder base64 = Base64.getEncoder();

        // generate MD5 of content
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        m.update(content);
        String md5 = base64.encodeToString(m.digest());

        HttpURLConnection c = buildDefaultRequestWithAuth("assets/v3", token);
        JSONObject metadata = new JSONObject();
        metadata.put("public", isPublic);
        metadata.put("retention", retention);
        Formatter multipartBody = new Formatter();
        multipartBody.format("--%s\r\n", BOUNDARY);
        multipartBody.format("Content-Type: application/json; charset=utf-8\r\n");
        multipartBody.format("Content-length: %d\r\n", metadata.toString().length());
        multipartBody.format("\r\n%s\r\n", metadata.toString());
        multipartBody.format("--%s\r\n", BOUNDARY);
        multipartBody.format("Content-Type: application/octet-stream\r\n");
        multipartBody.format("Content-length: %d\r\n", content.length);
        multipartBody.format("Content-MD5: %s\r\n\r\n", md5);

        // footer
        String footer = "\r\n--" + BOUNDARY + "--\r\n";

        c.setRequestProperty("Content-Type", "multipart/mixed; boundary=" + BOUNDARY);

        try {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(multipartBody.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.write(content);
            outputStream.write(footer.getBytes(StandardCharsets.UTF_8));

            String output = httpPost(c, outputStream.toByteArray(), new int[]{HTTP_CREATED});
            final JSONObject jsonOutput = new JSONObject(output);
            return jsonOutput.getString("key");
        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage());
        } finally {
            multipartBody.close();
        }
    }

    // endregion Assets

    // region Conversations

    public JSONObject createGroupConversation(ClientUser user, List<ClientUser> contacts, String conversationName) {
        JSONArray ids = new JSONArray();
        JSONArray qids = new JSONArray();

        for (ClientUser contact : contacts) {
            if (contact.getBackendName().equals(user.getBackendName())) {
                ids.put(contact.getId());
            } else {
                qids.put(new QualifiedID(contact.getId(), BackendConnections.get(contact.getBackendName()).domain).toJSON());
            }
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("users", ids);
        requestBody.put("qualified_users", qids);
        requestBody.put("conversation_role", "wire_member");
        requestBody.put("name", conversationName);
        HttpURLConnection c = buildDefaultRequestWithAuth("conversations", receiveAuthToken(user));
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        return new JSONObject(output);
    }

    public String createTeamConversation(ClientUser user, List<ClientUser> contacts, Team team) {
        return createTeamConversation(user, contacts, null, team);
    }

    public String createTeamConversation(ClientUser user, @Nullable List<ClientUser> contacts, String conversationName, Team team) {
        JSONArray ids = new JSONArray();
        JSONArray qids = new JSONArray();

        if (contacts != null) {
            for (ClientUser contact : contacts) {
                if (contact.getBackendName().equals(user.getBackendName())) {
                    ids.put(contact.getId());
                } else {
                    qids.put(new QualifiedID(contact.getId(), BackendConnections.get(contact.getBackendName()).domain).toJSON());
                }
            }
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("users", ids);
        requestBody.put("qualified_users", qids);
        requestBody.put("conversation_role", "wire_member");
        if (conversationName != null) {
            requestBody.put("name", conversationName);
        }
        requestBody.put("conversation_role", "wire_member");
        JSONObject teaminfo = new JSONObject();
        teaminfo.put("teamid", team.getId());
        teaminfo.put("managed", false);
        requestBody.put("team", teaminfo);
        JSONArray access = new JSONArray();
        access.put("invite");
        access.put("code");
        requestBody.put("access", access);
        JSONArray access_role_v2 = new JSONArray();
        access_role_v2.put("team_member");
        access_role_v2.put("non_team_member");
        access_role_v2.put("guest");
        access_role_v2.put("service");
        requestBody.put("access_role_v2", access_role_v2);
        HttpURLConnection c = buildDefaultRequestWithAuth("conversations", receiveAuthToken(user));
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        return new JSONObject(output).getString("id");
    }

    public String createMLSTeamConversation(ClientUser user, List<ClientUser> contacts, String conversationName, Team team, String deviceID) {
        JSONArray ids = new JSONArray();
        JSONArray qids = new JSONArray();

        for (ClientUser contact : contacts) {
            if (contact.getBackendName().equals(user.getBackendName())) {
                ids.put(contact.getId());
            } else {
                qids.put(new QualifiedID(contact.getId(), BackendConnections.get(contact.getBackendName()).domain).toJSON());
            }
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("users", ids);
        requestBody.put("qualified_users", qids);
        requestBody.put("conversation_role", "wire_member");
        if (conversationName != null) {
            requestBody.put("name", conversationName);
        }
        requestBody.put("conversation_role", "wire_member");
        JSONObject teaminfo = new JSONObject();
        teaminfo.put("teamid", team.getId());
        teaminfo.put("managed", false);
        requestBody.put("team", teaminfo);
        JSONArray access = new JSONArray();
        access.put("invite");
        access.put("code");
        requestBody.put("access", access);
        JSONArray access_role_v2 = new JSONArray();
        access_role_v2.put("team_member");
        access_role_v2.put("non_team_member");
        access_role_v2.put("guest");
        access_role_v2.put("service");
        requestBody.put("access_role_v2", access_role_v2);
        requestBody.put("protocol", "mls");
        requestBody.put("creator_client", deviceID);
        HttpURLConnection c = buildDefaultRequestWithAuth("conversations", receiveAuthToken(user));
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        return new JSONObject(output).getString("id");
    }

    public JSONObject getConversationInfo(ClientUser user, Conversation conversation) {
        JSONArray conversationIDsArray = new JSONArray();
        conversationIDsArray.put(conversation.getQualifiedID().toJSON());
        return getConversationObjects(receiveAuthToken(user), conversationIDsArray);
    }

    private JSONObject getConversationsInfo(AccessToken token, @Nullable String startId) {
        HttpURLConnection c = buildDefaultRequestWithAuth((startId == null) ? "conversations" : String.format(
                "conversations/?start=%s", startId), token);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    private JSONObject getConversationIDs(AccessToken token, @Nullable String pagingState) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("paging_state", pagingState);
        HttpURLConnection c = buildDefaultRequestWithAuth("conversations/list-ids", token);
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    private JSONObject getConversationObjects(AccessToken token, JSONArray conversationIDs) {
        HttpURLConnection c = buildDefaultRequestWithAuth("v4/conversations/list", token);

        JSONObject requestBody = new JSONObject();
        requestBody.put("qualified_ids", conversationIDs);
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    private List<Conversation> getConversations(ClientUser user) {
        final List<Conversation> result = new ArrayList<>();

        List<QualifiedID> conversationIDs = getConversationIDs(user);
        // WORKAROUND: 1000 is a limit for 'qualified_ids' parameter size of 'conversations/list/v2' endpoint
        for (int i = 0; i < conversationIDs.size(); i += 1000) {
            JSONArray conversationIDsArray = new JSONArray();
            for (int j = i; j < Math.min(conversationIDs.size(), i + 1000); ++j) {
                conversationIDsArray.put(conversationIDs.get(j).toJSON());
            }
            JSONObject conversationObjects = getConversationObjects(receiveAuthToken(user), conversationIDsArray);
            JSONArray conversationObjectResponse = conversationObjects.getJSONArray("found");

            for (int k = 0; k < conversationObjectResponse.length(); k++) {
                result.add(Conversation.fromJSON(conversationObjectResponse.getJSONObject(k)));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private List<QualifiedID> getConversationIDs(ClientUser user) {
        final List<QualifiedID> result = new ArrayList<>();

        JSONObject conversationIDs;
        String pagingState = null;

        do {
            conversationIDs = getConversationIDs(receiveAuthToken(user), pagingState);
            pagingState = conversationIDs.getString("paging_state");

            JSONArray qualifiedConversations = conversationIDs.getJSONArray("qualified_conversations");
            for (int i = 0; i < qualifiedConversations.length(); i++) {
                result.add(QualifiedID.fromJSON(qualifiedConversations.getJSONObject(i)));
            }
        } while (conversationIDs.getBoolean("has_more"));

        return Collections.unmodifiableList(result);
    }

    public Conversation getConversationByName(ClientUser ownerUser, String conversationName) {
        final List<Conversation> allConversations = getConversations(ownerUser);
        return allConversations.stream()
                .filter(conversationItem -> {
                    // Team 1:1
                    if (!conversationItem.getName().isPresent() && conversationItem.getOtherIds().size() == 1) {
                        try {
                            final String contactName = getUserNameByID(conversationItem.getOtherIds().get(0).getDomain(), conversationItem.getOtherIds().get(0).getID(), ownerUser);
                            return contactName.equals(conversationName);
                        } catch (Exception e) {
                            //ignore
                            log.warning(e.getMessage());
                        }
                    }
                    // Personal 1:1 OR Group conversations
                    if (conversationItem.getType().isPresent()) {
                        // in the case of personal 1:1 conversations, type is actually set correctly (e.g. = 2)
                        // the name element is not to be trusted of Conversation in personal 1:1
                        if (conversationItem.getType().get() == 2) {
                            try {
                                final String contactName = getUserNameByID(conversationItem.getOtherIds().get(0).getDomain(), conversationItem.getOtherIds().get(0).getID(), ownerUser);
                                return contactName.equals(conversationName);
                            } catch (Exception e) {
                                //ignore
                                log.warning(e.getMessage());
                            }
                        } else {
                            // now we know it is not a personal 1:1 conversation and it's not a team 1:1 conv.
                            // so we can go ahead and check if the name we search for is equal to the conv. name
                            if (conversationItem.getName().isPresent() && conversationItem.getName().get().equals(conversationName)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }).findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException(String.format("Conversation '%s' does not exist for user '%s'",
                                conversationName, ownerUser.getName()))
                );
    }

    public Conversation getConversationByName(ClientUser ownerUser, ClientUser otherUser) {
        final List<Conversation> allConversations = getConversations(ownerUser);
        return allConversations.stream()
                .filter(x -> {
                    try {
                        if (x.getOtherIds().size() == 1) {
                            final String otherName = getUserNameByID(x.getQualifiedID().getDomain(), x.getOtherIds().get(0).getID(), ownerUser);
                            return otherName.equals(otherUser.getName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                }).findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException(String.format(
                                "1:1 conversation with '%s' does not exist for user '%s'",
                                otherUser.getName(), ownerUser.getName()))
                );
    }

    public List<Conversation> getConversationsByName(ClientUser ownerUser, String conversationName) {
        final List<Conversation> allConversations = getConversations(ownerUser);
        return allConversations.stream()
                .filter(x -> {
                    if (x.getName().isPresent() && x.getName().get().equals(conversationName)) {
                        return true;
                    }
                    if (x.getOtherIds().size() == 1) {
                        try {
                            final String contactName = getUserNameByID(x.getQualifiedID().getDomain(), x.getOtherIds().get(0).getID(), ownerUser);
                            return contactName.equals(conversationName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
    }

    public void setArchivedStateForConversation(ClientUser asUser, Conversation conversation, boolean isArchived) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("conversations/%s/%s/self", conversation.getQualifiedID().getDomain(), conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("otr_archived", isArchived);
        String ref = getNowAsISOString();
        log.info("Archive conversation on ISO date: " + ref);
        requestBody.put("otr_archived_ref", ref);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    private static String getNowAsISOString() {
        final TimeZone timezone = TimeZone.getTimeZone("UTC");
        DateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.999Z'");
        isoFormat.setTimeZone(timezone);
        return isoFormat.format(new Date());
    }

    public void addUsersToGroupConversation(ClientUser asUser,
                                            List<ClientUser> contacts, Conversation conversation) {
        JSONObject requestBody = new JSONObject();
        JSONArray userIds = new JSONArray();
        for (ClientUser contact : contacts) {
            userIds.put(new QualifiedID(contact.getId(), BackendConnections.get(contact.getBackendName()).domain).toJSON());
        }
        requestBody.put("qualified_users", userIds);
        requestBody.put("conversation_role", "wire_member");

        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/members/v2", conversation.getId()),
                receiveAuthToken(asUser));
        httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void removeUserIdFromGroupConversation(ClientUser asUser, String contactId, Conversation conversation) {
        ClientUser contact = new ClientUser();
        contact.setId(contactId);
        removeUserFromGroupConversation(asUser, contact, conversation);
    }

    public void removeUserFromGroupConversation(ClientUser asUser, ClientUser contact, Conversation conversation) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format(
                        "conversations/%s/%s/members/%s/%s",
                        conversation.getQualifiedID().getDomain(), conversation.getId(), BackendConnections.get(contact).getDomain(), contact.getId()),
                receiveAuthToken(asUser));
        httpDelete(c, new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void changeConversationName(ClientUser asUser, Conversation conversationToRename, String newConversationName) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/%s/name", conversationToRename.getQualifiedID().getDomain(), conversationToRename.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", newConversationName);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void setMuteStateForConversation(ClientUser asUser, Conversation conversation, MuteState muteState) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/%s/self", conversation.getQualifiedID().getDomain(), conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("otr_muted", muteState != MuteState.NONE);
        requestBody.put("otr_muted_ref", getNowAsISOString());
        requestBody.put("otr_muted_status", muteState.getBitmaskValue());
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void setConversationMessageTimer(ClientUser asUser, Conversation conversation, Timedelta msgTimer) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/%s/message-timer", conversation.getQualifiedID().getDomain(), conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        if (msgTimer.asMillis() == 0) {
            requestBody.put("message_timer", JSONObject.NULL);
        } else {
            requestBody.put("message_timer", msgTimer.asMillis());
        }
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void setReadReceiptToConversation(ClientUser asUser, Conversation conversation, boolean newState) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/%s/receipt-mode", conversation.getQualifiedID().getDomain(), conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("receipt_mode", newState ? 1 : 0);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void deleteTeamConversation(ClientUser asUser, String convoId) {
        String teamId = asUser.getTeamId();
        if (teamId == null) {
            teamId = getTeamId(asUser);
        }
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("teams/%s/conversations/%s", teamId, convoId),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        httpDelete(c, requestBody.toString(), new int[]{HTTP_NO_CONTENT, HTTP_OK});
    }

    // endregion Conversations

    // region Conversation roles

    public void userChangesRoleOtherInConversation(ClientUser asUser, Conversation convo, ClientUser subjectUser,
                                                   String conversationRole) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("conversations/%s/%s/members/%s/%s",
                        convo.getQualifiedID().getDomain(), convo.getId(), BackendConnections.get(subjectUser).getDomain(), subjectUser.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("conversation_role", String.format("wire_%s", conversationRole.toLowerCase()));
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Conversation roles

    // region E2E Device Management

    public List<OtrClient> getOtrClients(ClientUser forUser) {
        final List<OtrClient> result = new ArrayList<>();
        HttpURLConnection c = buildDefaultRequestWithAuth("clients", receiveAuthToken(forUser));
        final String output = httpGet(c, new int[]{HTTP_OK});
        final JSONArray responseList = new JSONArray(output);
        for (int clientIdx = 0; clientIdx < responseList.length(); clientIdx++) {
            result.add(new OtrClient(responseList.getJSONObject(clientIdx)));
        }
        return result;
    }

    public JSONArray getClients(ClientUser forUser) {
        HttpURLConnection c = buildDefaultRequestWithAuth("clients", receiveAuthToken(forUser));
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONArray(output);
    }

    public void removeOtrClient(ClientUser forUser, OtrClient otrClientInfo) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("clients/%s", otrClientInfo.getId()),
                receiveAuthToken(forUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("password", forUser.getPassword());
        httpDelete(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    // endregion E2E Device Management

    // region Teams

    public String getTeamId(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("self", receiveAuthToken(user));
        return new JSONObject(httpGet(c, new int[]{HTTP_OK})).getString("team");
    }

    public Team getTeamByName(ClientUser forUser, String teamName) {
        return getAllTeams(forUser).stream().filter(x -> x.getName().equalsIgnoreCase(teamName)).findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException(String.format("Can not find team with name '%s'", teamName)));
    }

    public List<Team> getAllTeams(ClientUser forUser) {
        HttpURLConnection c = buildDefaultRequestWithAuth("teams", receiveAuthToken(forUser));
        final JSONArray teams = new JSONObject(httpGet(c, new int[]{HTTP_OK})).getJSONArray("teams");
        final List<Team> result = new ArrayList<>();
        for (int i = 0; i < teams.length(); ++i) {
            result.add(Team.fromJSON(teams.getJSONObject(i)));
        }
        return result;
    }

    public void deleteTeam(ClientUser forUser, Team team) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("teams/%s", team.getId()),
                receiveAuthToken(forUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("password", forUser.getPassword());
        httpDelete(c, requestBody.toString(), new int[]{HTTP_ACCEPTED});
    }

    public void suspendTeam(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/suspend", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPost(c, "", new int[]{HTTP_OK});
    }

    public void renameTeam(ClientUser admin, Team team, String newName) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s", team.getId()), receiveAuthToken(admin));
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", newName);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void renameTeam(ClientUser admin, String newName) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s", getTeamId(admin)), receiveAuthToken(admin));
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", newName);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void updateTeamIcon(ClientUser user, Team team, BufferedImage image) {
        String assetKey = retryOnBackendFailure(
                () -> {
                    try {
                        return uploadAssetV3(receiveAuthToken(user), true, "eternal",
                                ImageUtil.asByteArray(image));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s", team.getId()), receiveAuthToken(user));
        JSONObject requestBody = new JSONObject();
        requestBody.put("icon", assetKey);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void resetTeamIcon(ClientUser user, Team team) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s", team.getId()), receiveAuthToken(user));
        JSONObject requestBody = new JSONObject();
        requestBody.put("icon", "default");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void onlySendEmailInvitationToMember(ClientUser teamOwner, String teamId, String inviteeMail, String role) {
        inviteNewUserToTeam(receiveAuthToken(teamOwner), teamId, inviteeMail, teamOwner.getName(), role);
    }

    public void revokeTeamInvitationForMember(ClientUser forUser, Team team, String email) {
        JSONArray invitations = getTeamInvitations(receiveAuthToken(forUser), team.getId());
        for (int i = 0; i < invitations.length(); ++i) {
            String emailFound = invitations.getJSONObject(i).getString("email");
            if (emailFound.equals(email)) {
                String invitationId = invitations.getJSONObject(i).getString("id");
                HttpURLConnection c = buildDefaultRequestWithAuth(
                        String.format("teams/%s/invitations/%s", team.getId(), invitationId),
                        receiveAuthToken(forUser));
                httpDelete(c, new int[]{HTTP_OK});
                return;
            }
        }
        throw new IllegalStateException("Could not find invite for this member.");
    }

    public void revokeTeamInvitationsForTeam(ClientUser forUser, Team team) {
        JSONArray invitations = getTeamInvitations(receiveAuthToken(forUser), team.getId());
        boolean hasMore = invitations.length() > 0;
        while (hasMore) {
            for (int i = 0; i < invitations.length(); ++i) {
                String invitationId = invitations.getJSONObject(i).getString("id");
                HttpURLConnection c = buildDefaultRequestWithAuth(
                        String.format("teams/%s/invitations/%s", team.getId(), invitationId),
                        receiveAuthToken(forUser));
                httpDelete(c, new int[]{HTTP_OK});
            }
            invitations = getTeamInvitations(receiveAuthToken(forUser), team.getId());
            hasMore = invitations.length() > 0;
        }
    }

    private JSONArray getTeamInvitations(AccessToken token, String teamId) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/invitations", teamId), token);
        JSONObject invitationChunk = new JSONObject(httpGet(c, new int[]{HTTP_OK}));
        return invitationChunk.getJSONArray("invitations");
    }

    private JSONObject getTeamInvitations(AccessToken token, String teamId, int returnSize, String start) {
        HttpURLConnection c;
        if (start != null) {
            c = buildDefaultRequestWithAuth(String.format("teams/%s/invitations?size=%s&start=%s", teamId, returnSize, start), token);
        } else {
            c = buildDefaultRequestWithAuth(String.format("teams/%s/invitations?size=%s", teamId, returnSize), token);
        }
        JSONObject invitationChunk = new JSONObject(httpGet(c, new int[]{HTTP_OK}));
        return invitationChunk;
    }

    public List<String> getPendingTeamInvitations(ClientUser asUser) {
        List<String> emails = new ArrayList<>();
        Team firstTeam = getAllTeams(asUser).get(0);
        JSONArray invitations = getTeamInvitations(receiveAuthToken(asUser), firstTeam.getId());
        for (int i = 0; i < invitations.length(); ++i) {
            emails.add(invitations.getJSONObject(i).getString("email"));
        }
        return emails;
    }

    public List<String> getAllPendingTeamInvitations(ClientUser asUser) {
        List<String> emails = new ArrayList<>();
        Team firstTeam = getAllTeams(asUser).get(0);
        boolean hasMore = true;
        String lastId = null;
        while (hasMore) {
            JSONObject invitationsReturn = getTeamInvitations(receiveAuthToken(asUser), firstTeam.getId(), 500, lastId);
            JSONArray invitations = invitationsReturn.getJSONArray("invitations");
            hasMore = invitationsReturn.getBoolean("has_more");
            if (invitations.length() > 0) {
                lastId = invitations.getJSONObject(invitations.length() - 1).getString("id");
                for (int i = 0; i < invitations.length(); ++i) {
                    emails.add(invitations.getJSONObject(i).getString("email"));
                }
            }
        }
        return emails;
    }

    public int getTeamSize(ClientUser asUser, String teamId) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/size", teamId), receiveAuthToken(asUser));
        JSONObject response = new JSONObject(httpGet(c, new int[]{HTTP_OK}));
        return response.getInt("teamSize");
    }

    public List<TeamMember> getTeamMembers(ClientUser asUser) {
        Team firstTeam = getAllTeams(asUser).get(0);
        return getTeamMembers(receiveAuthToken(asUser), firstTeam.getId());
    }

    private List<TeamMember> getTeamMembers(AccessToken token, String teamId) {
        List<TeamMember> teamMembers = new ArrayList<>();
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/members", teamId), token);
        JSONObject response = new JSONObject(httpGet(c, new int[]{HTTP_OK}));
        JSONArray members = response.getJSONArray("members");
        for (int i = 0; i < members.length(); i++) {
            JSONObject member = members.getJSONObject(i);
            String userId = member.getString("user");
            JSONObject permissions = member.getJSONObject("permissions");
            TeamRole role = TeamRole.getByPermissionBitMask(permissions.getInt("self"));
            teamMembers.add(new TeamMember(userId, role));
        }
        return teamMembers;
    }

    public TeamRole getTeamRole(ClientUser asUser) {
        Team firstTeam = getAllTeams(asUser).get(0);
        List<TeamMember> members = getTeamMembers(receiveAuthToken(asUser), firstTeam.getId());
        for (TeamMember member : members) {
            if (asUser.getId().equals(member.getUserId())) {
                return member.getRole();
            }
        }
        return null;
    }

    public void editTeamMember(ClientUser asUser, Team team, ClientUser userToEdit, TeamRole newRole) {
        retryOnBackendFailure(() -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/members", team.getId()),
                    receiveAuthToken(asUser));
            JSONObject requestBody = new JSONObject();
            JSONObject member = new JSONObject();
            member.put("user", userToEdit.getId());
            JSONObject permissions = new JSONObject();
            permissions.put("copy", newRole.getPermissionBitMask());
            permissions.put("self", newRole.getPermissionBitMask());
            member.put("permissions", permissions);
            requestBody.put("member", member);
            httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
            return null;
        });
    }

    public void deleteTeamMember(ClientUser asUser, String teamId, String userIdOfMemberToDelete) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("teams/%s/members/%s", teamId, userIdOfMemberToDelete), receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("password", asUser.getPassword());
        httpDelete(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_ACCEPTED});
    }

    // endregion Teams

    // region Billing

    public Boolean teamBillingInfoExists(ClientUser user, Team team) {
        JSONObject info = retryOnBackendFailure(Timedelta.ofMinutes(5), Timedelta.ofSeconds(5), () -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/billing/info", team.getId()),
                    receiveAuthToken(user));
            return new JSONObject(httpGet(c, new int[]{HTTP_OK}));
        });
        return info != null;
    }

    public void updateBillingInfo(ClientUser user, Team team, JSONObject billingInfo) {
        retryOnBackendFailure(() -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(
                    String.format("teams/%s/billing/info", team.getId()),
                    receiveAuthToken(user));
            httpPut(c, billingInfo.toString(), new int[]{HTTP_OK, HTTP_ACCEPTED});
            return null;
        });
    }

    public boolean hasBillingPlansEnabled(ClientUser user, Team team) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("teams/%s/billing/plan", team.getId()),
                receiveAuthToken(user));
        return getResponseCodeForHttpGet(c) == 200;
    }

    public void setBillingPlan(ClientUser user, Team team, String planId) {
        retryOnBackendFailure(() -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(
                    String.format("teams/%s/billing/plan", team.getId()),
                    receiveAuthToken(user));
            JSONObject requestBody = new JSONObject();
            requestBody.put("planId", planId);
            httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_ACCEPTED});
            return null;
        });
    }

    public void setLegacyBillingPlan(ClientUser user, Team team, String planId) {
        retryOnBackendFailure(() -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(
                    String.format("teams/%s/billing/plan", team.getId()),
                    receiveAuthToken(user));
            JSONObject requestBody = new JSONObject();
            requestBody.put("planId", planId);
            httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_ACCEPTED});
            return null;
        });
    }

    public List<String> getCurrenciesFromBillingPlans(ClientUser user, Team team) {
        List<String> currencies = new ArrayList<>();
        final JSONArray plans = retryOnBackendFailure(() -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/billing/plan/list", team.getId()),
                    receiveAuthToken(user));
            final String output = httpGet(c, new int[]{HTTP_OK});
            return new JSONArray(output);
        });
        for (int i = 0; i < plans.length(); ++i) {
            JSONObject plan = plans.getJSONObject(i);
            if (plan.has("currency")) {
                String currency = plan.getString("currency");
                if (!currencies.contains(currency)) {
                    currencies.add(currency);
                }
            }
        }
        return currencies;
    }

    public String getStripeCustomerIdForTeam(ClientUser owner, Team team) {
        return retryOnBackendFailure(() -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(
                    String.format("teams/%s/billing/team", team.getId()),
                    receiveAuthToken(owner));
            final JSONObject info = new JSONObject(httpGet(c, new int[]{HTTP_OK}));
            return info.getString("customerId");
        });
    }

    public int getNumberOfInvoices(ClientUser owner, String teamId) {
        return retryOnBackendFailure(() -> {
            HttpURLConnection c = buildDefaultRequestWithAuth(
                    String.format("teams/%s/billing/invoices?size=10", teamId),
                    receiveAuthToken(owner));
            final JSONObject invoices = new JSONObject(httpGet(c, new int[]{HTTP_OK}));
            return invoices.getJSONArray("invoices").length();
        });
    }

    // endregion Billing

    // region Guests & Guestrooms

    public String createInviteLink(ClientUser asUser, Conversation conversation) {
        JSONObject payload = getPayloadOfInvite(asUser, conversation);
        try {
            JSONObject data = payload.getJSONObject("data");
            return data.getString("uri");
        } catch (JSONException e) {
            return payload.getString("uri");
        }
    }

    public String createInviteLinkWithPassword(ClientUser asUser, Conversation conversation, String password) {
        JSONObject payload = getPayloadOfInviteWithPassword(asUser, conversation, password);
//      There are different responses depending on whether the code already exists or was created through the API call.
//      If it already existed the uri is directly in the payload, otherwise it is in the data object
        try {
            JSONObject data = payload.getJSONObject("data");
            return data.getString("uri");
        } catch (JSONException e) {
            return payload.getString("uri");
        }
    }

    public String getInviteLink(ClientUser asUser, Conversation conversation) {
        JSONObject payload = getInviteCodePayload(asUser, conversation);
        try {
            JSONObject data = payload.getJSONObject("data");
            return data.getString("uri");
        } catch (JSONException e) {
            return payload.getString("uri");
        }
    }

    public String getJoinConversationPath(ClientUser asUser, Conversation conversation) {
        final JSONObject payload = getPayloadOfInvite(asUser, conversation);
        JSONObject data = payload.getJSONObject("data");
        String key = data.getString("key");
        String code = data.getString("code");
        return String.format("auth/?join_key=%s&join_code=%s#join-conversation", key, code);
    }

    private JSONObject getInviteCodePayload(ClientUser asUser, Conversation conversation) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/code", conversation.getId()),
                receiveAuthToken(asUser));
        String response = httpGet(c, new int[]{HTTP_OK, HTTP_CREATED,
                HTTP_NO_CONTENT});
        return new JSONObject(response);
    }

    public void inviteUsersViaLink(ClientUser asUser, List<ClientUser> usersToInvite, Conversation conversation) {
        final JSONObject payload = getPayloadOfInvite(asUser, conversation);
        JSONObject data = payload.getJSONObject("data");
        String key = data.getString("key");
        String code = data.getString("code");
        for (ClientUser userToInvite : usersToInvite) {
            HttpURLConnection c = buildDefaultRequestWithAuth("conversations/join", receiveAuthToken(userToInvite));
            JSONObject requestBody = new JSONObject();
            requestBody.put("key", key);
            requestBody.put("code", code);
            httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_CREATED});
        }
    }

    public int getJoinConversationResponseCode(ClientUser asUser, String key, String code) throws IOException {
        HttpURLConnection c = buildDefaultRequestWithAuth("conversations/join", receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("key", key);
        requestBody.put("code", code);
        return getResponseCodeForHttpPost(c, requestBody.toString());
    }

    private JSONObject getPayloadOfInvite(ClientUser asUser, Conversation conversation) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/code", conversation.getId()),
                receiveAuthToken(asUser));
        String response = httpPost(c, "", new int[]{HTTP_OK, HTTP_CREATED,
                HTTP_NO_CONTENT});
        return new JSONObject(response);
    }

    private JSONObject getPayloadOfInviteWithPassword(ClientUser asUser, Conversation conversation, String password) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("v4/conversations/%s/code", conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("password", password);
        String response = httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_CREATED,
                HTTP_NO_CONTENT});
        return new JSONObject(response);
    }

    public int getWirelessLinkResponseCode(ClientUser asUser, Conversation conversation) throws IOException {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/code", conversation.getId()),
                receiveAuthToken(asUser));
        return c.getResponseCode();
    }

    public void revokeInviteLink(ClientUser asUser, Conversation conversation) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/code", conversation.getId()),
                receiveAuthToken(asUser));
        httpDelete(c, new int[]{HTTP_OK});
    }

    public void allowGuests(ClientUser asUser, Conversation conversation) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/access", conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        JSONArray access = new JSONArray();
        access.put("invite");
        access.put("code");
        requestBody.put("access", access);
        // Set access role
        HttpURLConnection c1 = buildDefaultRequestWithAuth(
                String.format("conversations/%s", conversation.getId()),
                receiveAuthToken(asUser));
        final String output = httpGet(c1, new int[]{HTTP_OK});
        final JSONArray access_role_v2 = new JSONObject(output).getJSONArray("access_role_v2");
        access_role_v2.put("non_team_member");
        access_role_v2.put("guest");
        requestBody.put("access_role_v2", access_role_v2);
        //
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void disallowGuest(ClientUser asUser, Conversation conversation) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/access", conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        JSONArray access = new JSONArray();
        access.put("invite");
        access.put("code");
        requestBody.put("access", access);
        // Set access role
        HttpURLConnection c1 = buildDefaultRequestWithAuth(
                String.format("conversations/%s", conversation.getId()),
                receiveAuthToken(asUser));
        final String output = httpGet(c1, new int[]{HTTP_OK});
        final JSONArray access_role_v2 = new JSONObject(output).getJSONArray("access_role_v2");
        for (int i = 0; i < access_role_v2.length(); ++i) {
            if (access_role_v2.getString(i).equals("non_team_member") || access_role_v2.getString(i).equals("guest")) {
                access_role_v2.remove(i);
            }
        }
        requestBody.put("access_role_v2", access_role_v2);
        //
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    // endregion Guests & Guestrooms

    // region Services / Bots

    public ClientUser createNewServiceProvider(ClientUser user, String providerName, String providerUrl,
                                               String providerDescription) {
        HttpURLConnection c = buildDefaultRequest("provider/register", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("name", providerName);
        requestBody.put("email", user.getEmail());
        requestBody.put("password", user.getPassword());
        requestBody.put("url", providerUrl);
        requestBody.put("description", providerDescription);
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_CREATED});
        JSONObject responseBody = new JSONObject(output);
        user.setServiceProviderId(responseBody.getString("id"));
        return user;
    }

    public void activateServiceProvider(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/provider/activation-code?email=%s", uriEncode(user.getEmail())), MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        JSONObject data = new JSONObject(output);
        HttpURLConnection c2 = buildDefaultRequest(
                String.format("provider/activate?key=%s&code=%s", data.getString("key"), data.getString("code")),
                MediaType.APPLICATION_JSON);
        httpGet(c2, new int[]{HTTP_OK, HTTP_OK});
    }

    public void switchServiceForTeam(ClientUser ownerOrAdminUser, ServiceInfo serviceInfo) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("teams/%s/services/whitelist", serviceInfo.getTeamId()),
                MediaType.APPLICATION_JSON, null,
                receiveAuthToken(ownerOrAdminUser),  null);
        JSONObject requestBody = new JSONObject();
        requestBody.put("id", serviceInfo.getServiceId());
        requestBody.put("provider", serviceInfo.getProviderId());
        requestBody.put("whitelisted", serviceInfo.isEnabled());
        httpPost(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    private Service getService(ClientUser asUser, String teamId, String name) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("teams/%s/services/whitelisted?prefix=%s", teamId, uriEncode(name)),
                receiveAuthToken(asUser));
        final String output = httpGet(c, new int[]{HTTP_OK});
        JSONObject result = new JSONObject(output);
        JSONArray services = result.getJSONArray("services");
        for (int i = 0; i < services.length(); i++) {
            JSONObject bot = services.getJSONObject(i);
            if (bot.has("name") && bot.getString("name").equals(name)) {
                return new Service(bot.getString("id"), bot.getString("provider"));
            }
        }
        return null;
    }

    public void addServiceToConversation(ClientUser asUser, String serviceName, Conversation convo) {
        String teamId = convo.getTeamId().orElseThrow(IllegalStateException::new);
        Service service = getService(asUser, teamId, serviceName);
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/bots", convo.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("service", service.getId());
        requestBody.put("provider", service.getProvider());
        httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
    }

    public void allowServices(ClientUser asUser, Conversation conversation) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/access", conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        JSONArray access = new JSONArray();
        access.put("invite");
        access.put("code");
        requestBody.put("access", access);
        // Set access role
        HttpURLConnection c1 = buildDefaultRequestWithAuth(
                String.format("conversations/%s", conversation.getId()),
                receiveAuthToken(asUser));
        final String output = httpGet(c1, new int[]{HTTP_OK});
        final JSONArray access_role_v2 = new JSONObject(output).getJSONArray("access_role_v2");
        access_role_v2.put("service");
        requestBody.put("access_role_v2", access_role_v2);
        //
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void disallowServices(ClientUser asUser, Conversation conversation) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("conversations/%s/access", conversation.getId()),
                receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        JSONArray access = new JSONArray();
        access.put("invite");
        access.put("code");
        requestBody.put("access", access);
        // Set access role
        HttpURLConnection c1 = buildDefaultRequestWithAuth(
                String.format("conversations/%s", conversation.getId()),
                receiveAuthToken(asUser));
        final String output = httpGet(c1, new int[]{HTTP_OK});
        final JSONArray access_role_v2 = new JSONObject(output).getJSONArray("access_role_v2");
        for (int i = 0; i < access_role_v2.length(); ++i) {
            if (access_role_v2.getString(i).equals("service")) {
                access_role_v2.remove(i);
            }
        }
        requestBody.put("access_role_v2", access_role_v2);
        //
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    // endregion Services / Bots

    // region Digital Signature

    public void enableDigitalSignatureFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/digitalSignatures", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableDigitalSignatureFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/digitalSignatures", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Digital Signature

    // region App Lock

    public JSONObject getAppLockFeatureSettings(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/appLock", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void enableForceAppLockFeature(Team team, int seconds) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/appLock", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");

        JSONObject config = new JSONObject();
        config.put("enforceAppLock", true);
        config.put("inactivityTimeoutSecs", seconds);
        requestBody.put("config", config);

        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableForceAppLockFeature(Team team, int seconds) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/appLock", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");

        JSONObject config = new JSONObject();
        config.put("enforceAppLock", false);
        config.put("inactivityTimeoutSecs", seconds);
        requestBody.put("config", config);

        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableAppLockFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/appLock", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");

        JSONObject config = new JSONObject();
        config.put("enforceAppLock", false);
        config.put("inactivityTimeoutSecs", 60);
        requestBody.put("config", config);

        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void enableAppLockFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/appLock", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");

        JSONObject config = new JSONObject();
        config.put("enforceAppLock", false);
        config.put("inactivityTimeoutSecs", 30);
        requestBody.put("config", config);

        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion App Lock

    // region Legal Hold

    public void whitelistTeamForLegalHold(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/legalhold/whitelisted-teams/%s", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, "", new int[]{HTTP_OK});
    }

    public void enableLegalHold(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/legalhold", uriEncode(team.getId())), MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public String registerLegalHoldService(ClientUser asUser, Team team) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/legalhold/settings",
                team.getId()), receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("auth_token", LegalHoldServiceSettings.SERVICE_AUTH_TOKEN);
        requestBody.put("base_url", LegalHoldServiceSettings.SERVICE_BASE_URL);
        requestBody.put("public_key", LegalHoldServiceSettings.SERVICE_PUBLIC_KEY);
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        return new JSONObject(output).getJSONObject("settings").getString("fingerprint");
    }

    public String breakLegalHoldService(ClientUser asUser, Team team) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/legalhold/settings",
                team.getId()), receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("auth_token", "broken");
        requestBody.put("base_url", LegalHoldServiceSettings.SERVICE_BASE_URL);
        requestBody.put("public_key", LegalHoldServiceSettings.SERVICE_PUBLIC_KEY);
        final String output = httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED});
        return new JSONObject(output).getJSONObject("settings").getString("fingerprint");
    }

    public void unregisterLegalHoldService(ClientUser asUser, String teamId) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/legalhold/settings",
                teamId), receiveAuthToken(asUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("password", asUser.getPassword());
        httpDelete(c, requestBody.toString(), new int[]{HTTP_NO_CONTENT});
    }

    public void setLegalHoldConsentForUser(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/legalhold/consent",
                user.getTeamId()), receiveAuthToken(user));
        JSONObject requestBody = new JSONObject();
        httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED, HTTP_NO_CONTENT});
    }

    public void adminSendsLegalHoldRequestForUser(ClientUser adminUser, ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/legalhold/%s",
                adminUser.getTeamId(), user.getId()), receiveAuthToken(adminUser));
        JSONObject requestBody = new JSONObject();
        //requestBody.put("team_id", teamId);
        //requestBody.put("user_id", userId);
        httpPost(c, requestBody.toString(), new int[]{HTTP_CREATED, HTTP_NO_CONTENT});
    }

    public void adminTurnsOffLegalHoldForUser(ClientUser adminUser, ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/legalhold/%s",
                adminUser.getTeamId(), user.getId()), receiveAuthToken(adminUser));
        JSONObject requestBody = new JSONObject();
        requestBody.put("password", adminUser.getPassword());
        httpDelete(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void userAcceptsLegalHoldRequest(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("teams/%s/legalhold/%s/approve",
                user.getTeamId(), user.getId()), receiveAuthToken(user));
        JSONObject requestBody = new JSONObject();
        requestBody.put("password", user.getPassword());
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Legal Hold

    // region Custom backend

    public void addCustomBackendDomain(String domain, String configURL, String webappURL) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/custom-backend/by-domain/%s", uriEncode(domain)), MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("config_json_url", configURL);
        requestBody.put("webapp_welcome_url", webappURL);
        httpPut(c, requestBody.toString(), new int[]{HTTP_CREATED});
    }

    public void deleteCustomBackendDomain(String domain) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/custom-backend/by-domain/%s", uriEncode(domain)), MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        httpDelete(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Custom backend

    // region SSO

    public JSONObject getSSOFeatureSettings(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/sso", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void enableSSOFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/sso", uriEncode(team.getId())), MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public String createIdentityProvider(ClientUser user, String metadata) {
        HttpURLConnection c = buildDefaultRequestWithAuth("identity-providers", MediaType.APPLICATION_XML,
                MediaType.APPLICATION_JSON, receiveAuthToken(user), null);
        final String output = httpPost(c, metadata, new int[]{HTTP_OK, HTTP_CREATED});
        JSONObject responseBody = new JSONObject(output);
        return responseBody.getString("id");
    }

    public String createIdentityProviderV2(ClientUser user, String metadata) {
        HttpURLConnection c = buildDefaultRequestWithAuth("v5/identity-providers?api_version=v2",
                MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, receiveAuthToken(user), null);
        final String output = httpPost(c, metadata, new int[]{HTTP_OK, HTTP_CREATED});
        JSONObject responseBody = new JSONObject(output);
        return responseBody.getString("id");
    }

    public void setFixedSSO(String ssoId) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor("i/sso/settings", MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("default_sso_code", ssoId);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    // endregion SSO

    // region Delegated Admins team

    public void enableDelegatedAdminsFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/feature_delegated_admins", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_NO_CONTENT});
    }

    // endregion Delegated Admins team

    // region File Sharing

    public JSONObject getFileSharingFeatureSettings(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/fileSharing", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void unlockFileSharingFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/fileSharing/unlocked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void lockFileSharingFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/fileSharing/locked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void enableFileSharingFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/fileSharing", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableFileSharingFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/fileSharing", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion File Sharing

    // region Self-Deleting Messages

    public JSONObject getSelfDeletingMessagesSettings(ClientUser teamMember) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/selfDeletingMessages", uriEncode(getTeamId(teamMember))),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void unlockSelfDeletingMessagesFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/selfDeletingMessages/unlocked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void lockSelfDeletingMessagesFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/selfDeletingMessages/locked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void enableForcedSelfDeletingMessages(Team team, long seconds) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/selfDeletingMessages", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");

        JSONObject config = new JSONObject();
        config.put("enforcedTimeoutSeconds", seconds);
        requestBody.put("config", config);

        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableForcedSelfDeletingMessages(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/selfDeletingMessages", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");

        JSONObject config = new JSONObject();
        config.put("enforcedTimeoutSeconds", 0);
        requestBody.put("config", config);

        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableSelfDeletingMessagesFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/selfDeletingMessages", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");

        JSONObject config = new JSONObject();
        config.put("enforcedTimeoutSeconds", 0);
        requestBody.put("config", config);

        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void enableSelfDeletingMessagesFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/selfDeletingMessages", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");

        JSONObject config = new JSONObject();
        config.put("enforcedTimeoutSeconds", 0);
        requestBody.put("config", config);

        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Self-Deleting Messages

    // region Backend Features

    public JSONObject getFeatureConfig(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("feature-configs", receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public String getFeatureConfig(String feature, ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("feature-configs/%s", feature), receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output).toString();
    }

    public boolean isDevelopmentApiEnabled(ClientUser user) {
        String response = getFeatureConfig("mls", user);
        return new JSONObject(response).get("status").equals("enabled");
    }

    public void enableConferenceCalling(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conferenceCalling", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableConferenceCalling(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conferenceCalling", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion

    // region Conference Calling feature backdoor

    public Boolean isConferenceCallingEnabled(Team team, ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth(String.format("feature-configs/conferenceCalling",
                uriEncode(team.getId())), receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output).getString("status").equals("enabled");
    }

    public void unlockConferenceCallingFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conferenceCalling/unlocked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void enableConferenceCallingBackdoorViaBackdoorTeam(Team team) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPatch(
                String.format("i/teams/%s/features/conferenceCalling", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableConferenceCallingBackdoorViaBackdoorTeam(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conferenceCalling", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void enableConferenceCallingViaBackdoorPersonalUser(ClientUser personalUsers) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/users/%s/features/conferenceCalling", uriEncode(personalUsers.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void upgradeToEnterprisePlanResult(Team team) {
        enableConferenceCallingBackdoorViaBackdoorTeam(team);
    }

    public JSONObject getCallConfig(ClientUser user) {
        HttpURLConnection c = buildDefaultRequestWithAuth("calls/config/v2",
                receiveAuthToken(user),
                user.getAccessCredentialsWithoutRefresh().getAccessCookie());
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    // endregion Conference Calling feature backdoor

    // region Team Sign up - Marketo integration

    public JSONObject getMarketoCustomer(String email) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/marketo/emails/%s", uriEncode(email)),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK, HTTP_NOT_FOUND});
        if (!output.isEmpty()) {
            return new JSONObject(output);
        } else {
            return new JSONObject();
        }
    }

    // endregion Team Sign up - Marketo integration

    // region Invite guest link feature backdoor

    public JSONObject getGuestLinksFeatureSettings(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conversationGuestLinks", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void unlockGuestLinksFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conversationGuestLinks/unlocked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void lockGuestLinksFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conversationGuestLinks/locked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void disableInviteGuestLinkFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conversationGuestLinks", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void enableInviteGuestLinkFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/conversationGuestLinks", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Invite guest link feature backdoor

    // region Invite inbound/outbound feature backdoor

    public JSONObject getSearchVisibilityInboundFeatureSettings(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/searchVisibilityInbound", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void enableSearchVisibilityInbound(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/searchVisibilityInbound", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableSearchVisibilityInbound(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/searchVisibilityInbound", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public JSONObject getTeamSearchVisibilityOutboundFeatureSettings(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/search-visibility", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void enableTeamSearchVisibilityOutbound(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/searchVisibility", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableTeamSearchVisibilityOutbound(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/searchVisibility", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void setTeamSearchVisibilityOutboundStandard(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/search-visibility", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("search_visibility", "standard");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    public void setTeamSearchVisibilityOutboundNoNameOutsideTeam(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/search-visibility", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("search_visibility", "no-name-outside-team");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK, HTTP_NO_CONTENT});
    }

    // endregion search inbound/outbound feature backdoor

    // region 2F Authentication

    public JSONObject get2FAuthenticationFeatureSettings(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/sndFactorPasswordChallenge", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void lock2FAuthenticationFeature(String teamID) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/sndFactorPasswordChallenge/locked", uriEncode(teamID)),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void unlock2FAuthenticationFeature(String teamID) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/sndFactorPasswordChallenge/unlocked", uriEncode(teamID)),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void disable2FAuthenticationFeature(String teamID) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/sndFactorPasswordChallenge", uriEncode(teamID)),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void enable2FAuthenticationFeature(String teamID) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/sndFactorPasswordChallenge", uriEncode(teamID)),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion 2F Authentication

    // region MLS

    public void migrateTeamToMLS(Team team, String startDate, String finaliseDate) {
        JSONObject requestBody = new JSONObject();
        JSONObject config = new JSONObject();

        requestBody.put("config", config);
        config.put("finaliseRegardlessAfter", finaliseDate);
        config.put("startTime", startDate);
        requestBody.put("status", "enabled");

        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/mlsMigration", uriEncode(team.getId())), MediaType.APPLICATION_JSON);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
        System.out.println("Request: " + c);
    }

    public void disableMLSFeature(Team team, List<ClientUser> members) {
        disableMLSMigration(team);

        JSONArray protocolToggleUsers = new JSONArray();
        JSONObject requestBody = new JSONObject();
        JSONObject config = new JSONObject();

        for (ClientUser member : members) {
            protocolToggleUsers.put(member.getId());
        }

        JSONArray allowedCipherSuites = new JSONArray();
        allowedCipherSuites.put(1);
        config.put("allowedCipherSuites", allowedCipherSuites);
        config.put("defaultCipherSuite", 1);
        config.put("defaultProtocol", "proteus");
        config.put("protocolToggleUsers", protocolToggleUsers);
        JSONArray supportedProtocols = new JSONArray();
        supportedProtocols.put("proteus");
        supportedProtocols.put("mls");
        config.put("supportedProtocols", supportedProtocols);

        requestBody.put("config", config);
        requestBody.put("status", "disabled");

        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/mls", uriEncode(team.getId())), MediaType.APPLICATION_JSON);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void enableMLSFeatureTeam(Team team, Integer defaultCipherSuite,
                                     List<Integer> allowedCipherSuites, String defaultProtocol,
                                     List<String> supportedProtocols) {
        JSONObject config = new JSONObject();
        JSONObject requestBody = new JSONObject();
        JSONArray protocolToggleUsers = new JSONArray();
        JSONArray cipherSuites = new JSONArray();
        config.put("defaultCipherSuite", defaultCipherSuite);
        for (Integer cipherSuite: allowedCipherSuites) {
            cipherSuites.put(cipherSuite);
        }
        config.put("allowedCipherSuites", cipherSuites);
        config.put("defaultProtocol", defaultProtocol);
        config.put("protocolToggleUsers", protocolToggleUsers);
        JSONArray protocols = new JSONArray();
        for (String protocol: supportedProtocols) {
            protocols.put(protocol);
        }
        config.put("supportedProtocols", protocols);
        requestBody.put("config", config);
        requestBody.put("status", "enabled");

        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/mls", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void lockMLSFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/mls/locked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK, 409});
    }

    public void disableMLSFeatureTeam(Team team) {
        disableMLSMigration(team);

        JSONObject config = new JSONObject();
        JSONObject requestBody = new JSONObject();
        JSONArray protocolToggleUsers = new JSONArray();

        JSONArray allowedCipherSuites = new JSONArray();
        allowedCipherSuites.put(1);
        config.put("allowedCipherSuites", allowedCipherSuites);
        config.put("defaultCipherSuite", 1);
        config.put("defaultProtocol", "proteus");
        config.put("protocolToggleUsers", protocolToggleUsers);
        config.put("supportedProtocols", new JSONArray().put("proteus").put("mls"));

        requestBody.put("config", config);
        requestBody.put("status", "disabled");

        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/mls", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void disableMLSMigration(Team team) {
        JSONObject requestBody = new JSONObject();
        JSONObject config = new JSONObject();

        requestBody.put("config", config);
        requestBody.put("status", "disabled");

        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/mlsMigration", uriEncode(team.getId())), MediaType.APPLICATION_JSON);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void claimKeyPackages(ClientUser user) {
        JSONObject requestBody = new JSONObject();
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("v5/mls/key-packages/claim/%s/%s?ciphersuite=2", BackendConnections.get(user).getDomain(), user.getId()),
                receiveAuthToken(user));
        httpPost(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public JSONObject getRemainingKeyPackagesCount(ClientUser user, String deviceID) {
        HttpURLConnection c = buildDefaultRequestWithAuth(
                String.format("v5/mls/key-packages/self/%s/count?ciphersuite=2", deviceID),
                receiveAuthToken(user));
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    // endregion MLS

    // region E2EI

    public void enableE2EIFeatureTeam(Team team, String acmeDiscoveryUrl) {
        JSONObject config = new JSONObject();
        JSONObject requestBody = new JSONObject();

        config.put("acmeDiscoveryUrl", acmeDiscoveryUrl);
        config.put("useProxyOnMobile", true);
        config.put("verificationExpiration", 3600);
        requestBody.put("config", config);
        requestBody.put("status", "enabled");

        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/mlsE2EId", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void enableE2EIFeatureTeam(Team team, String acmeDiscoveryUrl, int verificationExpiration) {
        JSONObject config = new JSONObject();
        JSONObject requestBody = new JSONObject();

        config.put("acmeDiscoveryUrl", acmeDiscoveryUrl);
        config.put("useProxyOnMobile", true);
        config.put("verificationExpiration", verificationExpiration);
        requestBody.put("config", config);
        requestBody.put("status", "enabled");

        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/mlsE2EId", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion

    // region Outlook Calendar Integration

    public JSONObject getOutlookCalendarIntegrationFeatureSettings(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/outlookCalIntegration", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        final String output = httpGet(c, new int[]{HTTP_OK});
        return new JSONObject(output);
    }

    public void unlockOutlookCalendarIntegrationFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/outlookCalIntegration/unlocked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void lockOutlookCalendarIntegrationFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/outlookCalIntegration/locked", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        httpPut(c, new JSONObject().toString(), new int[]{HTTP_OK});
    }

    public void disableOutlookCalendarIntegrationFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/outlookCalIntegration", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "disabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    public void enableOutlookCalendarIntegrationFeature(Team team) {
        HttpURLConnection c = buildDefaultRequestOnBackdoor(
                String.format("i/teams/%s/features/outlookCalIntegration", uriEncode(team.getId())),
                MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("status", "enabled");
        httpPut(c, requestBody.toString(), new int[]{HTTP_OK});
    }

    // endregion Outlook Calendar Integration

}
