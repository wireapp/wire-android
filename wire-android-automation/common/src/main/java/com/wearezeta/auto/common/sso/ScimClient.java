package com.wearezeta.auto.common.sso;

import com.wearezeta.auto.common.ImageUtil;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import org.apache.commons.lang3.StringUtils;
import java.util.logging.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;

public class ScimClient {

    private static final Logger log = ZetaLogger.getLog(ScimClient.class.getSimpleName());

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(240);

    private static String backendUrl = null;
    private Client client;
    private String scimAuthToken = null;
    private String backendName;

    public ScimClient(String backendName) {
        final ClientConfig configuration = new ClientConfig();
        configuration.property(ClientProperties.CONNECT_TIMEOUT, (int) CONNECT_TIMEOUT.toMillis());
        configuration.property(ClientProperties.READ_TIMEOUT, (int) READ_TIMEOUT.toMillis());
        this.client = ClientBuilder.newClient(configuration);
        this.backendName = backendName;
    }

    private URI getBaseURI() {
        String backend = BackendConnections.get(backendName).getBackendUrl();
        return UriBuilder.fromUri(backend).build();
    }

    private WebTarget getSCIMTarget(String path) {
        final String target = String.format("%s%s", getBaseURI(), path);
        log.fine(String.format("Making request to %s...", target));
        return client.target(target);
    }

    private static String bearer(String scimToken) {
        return "Bearer " + scimToken;
    }

    private static void checkResponse(Response res) {
        int status = res.getStatus();

        if (status == 404)
            throw new IllegalStateException("404 User not provisioned yet");

        if (status == 409)
            throw new IllegalStateException("409 User already provisioned");

        if (status == 401)
            throw new IllegalStateException("401 Not authorized for SCIM");

        if (status >= 400)
            throw new IllegalStateException(String.format("Failed with status %s to provision SCIM user: %s", status, res.readEntity(String.class)));
    }

    private String getScimAuthToken() {
        if (scimAuthToken == null) {
            throw new IllegalStateException("SCIM Auth Token is missing. Maybe you forgot to add the step to create the user " +
                    "via SCIM?");
        }
        return scimAuthToken;
    }

    public String insert(ClientUser asUser, ClientUser userToCreate, @Nullable BufferedImage image) {
        JSONObject profile = new JSONObject();
        profile.put("externalId", userToCreate.getEmail());
        profile.put("userName", userToCreate.getUniqueUsername());
        profile.put("displayName", userToCreate.getName());

        if (image != null) {
            try {
                JSONArray photos = new JSONArray();
                JSONObject photo = new JSONObject();
                photo.put("type", "profile-large"); // or profile-small
                photo.put("value", uploadAsset(asUser, image));
                photos.put(photo);
                profile.put("photos", photos);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to upload profile picture: " + e.getMessage(), e);
            }
        }

        return createProfile(asUser, profile);
    }

    public JSONObject claim(String userId) {
        // Not implemented on backend yet
        return new JSONObject();
    }

    public void changeName(String userId, String name) {
        JSONObject profile = getProfile(userId);
        profile.put("displayName", name);
        updateProfile(userId, profile);
    }

    public void changeUniqueUsername(String userId, String name) {
        JSONObject profile = getProfile(userId);
        profile.put("userName", name);
        updateProfile(userId, profile);
    }

    public void changeProfilePicture(ClientUser asUser, String userId, BufferedImage image) {
        JSONObject profile = getProfile(userId);

        try {
            BufferedImage previewImage = ImageUtil.scaleTo(image, 200, 200);

            JSONArray photos = new JSONArray();
            JSONObject photo = new JSONObject();
            photo.put("type", "profile-large"); // or profile-small
            photo.put("value", uploadAsset(asUser, image));
            photos.put(photo);
            JSONObject preview = new JSONObject();
            preview.put("type", "profile-small"); // or profile-small
            preview.put("value", uploadAsset(asUser, previewImage));
            photos.put(preview);
            profile.put("photos", photos);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to upload profile picture: " + e.getMessage(), e);
        }

        updateProfile(userId, profile);
    }

    public void changeAccentColor(String userId, int color) {
        JSONObject profile = getProfile(userId);
        profile.put("color", color);
        updateProfile(userId, profile);
    }

    public void changeEmail(String userId, String email) {
        JSONObject profile = getProfile(userId);
        profile.put("externalId", email);
        updateProfile(userId, profile);
    }

    private JSONArray getOldSchemaFields(JSONObject profile) {
        if (profile.has("urn:wire:scim:schemas:profile:1.0")) {
            JSONObject schema = profile.getJSONObject("urn:wire:scim:schemas:profile:1.0");
            if (schema.has("richInfo")) {
                JSONObject richInfo = schema.getJSONObject("richInfo");
                if (richInfo.has("fields")) {
                    return richInfo.getJSONArray("fields");
                }
            }
        }
        return new JSONArray();
    }

    private JSONObject getUserSchemaFields(JSONObject profile) {
        if (profile.has("urn:ietf:params:scim:schemas:extension:wire:1.0:User")) {
            return profile.getJSONObject("urn:ietf:params:scim:schemas:extension:wire:1.0:User");
        }
        return new JSONObject();
    }

    private JSONObject updateProfileWithRichInfo(JSONObject profile, JSONArray fields) {
        JSONObject schema = new JSONObject();
        JSONObject richInfo = new JSONObject();
        richInfo.put("fields", fields);
        richInfo.put("version", 0);
        schema.put("richInfo", richInfo);
        profile.put("urn:wire:scim:schemas:profile:1.0", schema);
        return profile;
    }

    private JSONObject updateProfileWithUserSchema(JSONObject profile, JSONObject schema) {
        profile.put("urn:ietf:params:scim:schemas:extension:wire:1.0:User", schema);
        return profile;
    }

    public void updateRichInfo(String userId, String key, String value) {
        JSONObject profile = getProfile(userId);
        JSONObject schema = getUserSchemaFields(profile);
        schema.put(key, value);
        updateProfile(userId, updateProfileWithUserSchema(profile, schema));
    }

    public void removeKeyFromRichInfo(String userId, String key) {
        JSONObject profile = getProfile(userId);
        JSONArray fields = getOldSchemaFields(profile);
        JSONObject schema = getUserSchemaFields(profile);
        for (int i = 0; i < fields.length(); i++) {
            if (fields.getJSONObject(i).getString("type").equals(key)) {
                fields.remove(i);
                break;
            }
        }
        schema.remove(key);
        updateProfileWithRichInfo(profile, fields);
        updateProfileWithUserSchema(profile, schema);
        updateProfile(userId, profile);
    }

    private String createProfile(ClientUser asUser, JSONObject profile) {
        if (scimAuthToken == null) {
            scimAuthToken = createSCIMAuthToken(asUser);
        }

        log.info(String.format(" >>> POST: %s", formatLogRecord(profile)));

        Response res = getSCIMTarget("scim/v2/Users")
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(scimAuthToken))
                .header(HttpHeaders.CONTENT_TYPE, "application/scim+json")
                .post(Entity.entity(profile.toString(), MediaType.APPLICATION_JSON));

        checkResponse(res);

        String response = res.readEntity(String.class);
        log.info(String.format(" >>> Response: %s", formatLogRecord(response)));
        return new JSONObject(response).getString("id");
    }

    private JSONObject getProfile(String userId) {
        String token = getScimAuthToken();

        log.fine(" >>> GET ");

        Response res = getSCIMTarget("scim/v2/Users")
                .path(userId)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .get();

        checkResponse(res);

        String response = res.readEntity(String.class);
        log.fine(String.format(" >>> Response: %s", formatLogRecord(response)));
        return new JSONObject(response);
    }

    private void updateProfile(String userId, JSONObject profile) {
        String token = getScimAuthToken();

        log.fine(String.format(" >>> PUT: %s", formatLogRecord(profile)));

        Response res = getSCIMTarget("scim/v2/Users")
                .path(userId)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .header(HttpHeaders.CONTENT_TYPE, "application/scim+json")
                .put(Entity.entity(profile.toString(), MediaType.APPLICATION_JSON));

        checkResponse(res);

        String response = res.readEntity(String.class);
        log.fine(String.format(" >>> Response: %s", formatLogRecord(response)));
    }

    private String uploadAsset(ClientUser asUser, BufferedImage image) throws IOException {
        String key = BackendConnections.get(asUser).uploadAssetV3(
                asUser.getAccessCredentialsWithoutRefresh().getAccessToken(),
                true, "eternal", ImageUtil.asByteArray(image));
        return String.format("wire-asset://%s", key);
    }

    private static String formatLogRecord(Object entity) {
        String result = "EMPTY";
        if (entity != null) {
            if (entity instanceof String) {
                result = ((String) entity);
            } else {
                result = entity.toString();
            }
            result = result.isEmpty() ? "EMPTY" : StringUtils.abbreviate(result, 800);
        }
        return result;
    }

    private String createSCIMAuthToken(ClientUser asUser) {
        return BackendConnections.get(asUser).createSCIMAccessToken(asUser, "SCIM");
    }
}

