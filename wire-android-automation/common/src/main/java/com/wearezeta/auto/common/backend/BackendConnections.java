package com.wearezeta.auto.common.backend;

import java.util.*;
import java.util.logging.Logger;

import com.wearezeta.auto.common.Config;
import com.wearezeta.auto.common.backend.models.BasicAuth;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import org.json.JSONArray;
import org.json.JSONObject;

public class BackendConnections {

    private static final Logger log = ZetaLogger.getLog(BackendConnections.class.getSimpleName());
    private final static Map<String, Backend> backendConnections;
    private final static String defaultBackendName;

    static {
        backendConnections = getBackendConnections();
        defaultBackendName = Config.common().getBackendType(BackendConnections.class);
    }

    private static Map<String, Backend> getBackendConnections() {
        Map<String, Backend> backendConnections = new HashMap<>();
        JSONArray json = BackendConnectionsReader.read();
        for (int i = 0; i < json.length(); i++) {
            JSONObject entry = json.getJSONObject(i);
            if (entry.has("fields")) {
                Map<String, String> fields = mapFields(entry.getJSONArray("fields"));
                String name = fields.get("name");
                log.info("1Password item for " + name + " with " + fields.size() + " entries found.");
                BasicAuth basicAuth;
                if (fields.get("basicAuthUsername") != null && fields.get("basicAuthPassword") != null) {
                    basicAuth = new BasicAuth(fields.get("basicAuthUsername"), fields.get("basicAuthPassword"));
                } else {
                    basicAuth = new BasicAuth(fields.get("basicAuth"));
                }
                BasicAuth inbucketAuth;
                if (fields.get("inbucketUsername") != null && fields.get("inbucketPassword") != null) {
                    inbucketAuth = new BasicAuth(fields.get("inbucketUsername"), fields.get("inbucketPassword"));
                } else {
                    inbucketAuth = basicAuth;
                }
                backendConnections.put(name, new Backend(
                        name,
                        fields.get("backendUrl"),
                        fields.get("webappUrl"),
                        fields.get("domain"),
                        fields.get("backendWebsocket"),
                        fields.get("deeplink"),
                        fields.get("inbucketUrl"),
                        fields.get("keycloakUrl"),
                        fields.get("acmeDiscoveryUrl"),
                        fields.get("k8sNamespace"),
                        basicAuth,
                        inbucketAuth,
                        false,
                        fields.get("socksProxy")));
            }
        }
        return backendConnections;
    }

    private static Map<String, String> mapFields(JSONArray fields) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field.has("label") && field.has("value")) {
                String label = field.getString("label");
                String value = field.getString("value");
                map.put(label, value);
            }
        }
        return map;
    }

    public static Backend getDefault() {
        return backendConnections.get(defaultBackendName);
    }

    public static Backend get(String backendName) {
        if(backendName == null) {
            backendName = getDefault().getBackendName();
        }
        // Possibility to use bund-qa and bund-next environments by just using the prefix "column-x" as backend name
        if (backendName.startsWith("column") || backendName.startsWith("external") && defaultBackendName.contains("column")) {
            String prefix = defaultBackendName.substring(0, defaultBackendName.indexOf("column"));
            log.info(String.format("Replace %s with %s", backendName, prefix + backendName));
            backendName = prefix + backendName;
        }
        return backendConnections.getOrDefault(backendName, getDefault());
    }

    public static Backend get(ClientUser user) {
        return backendConnections.getOrDefault(user.getBackendName(), getDefault());
    }

}
