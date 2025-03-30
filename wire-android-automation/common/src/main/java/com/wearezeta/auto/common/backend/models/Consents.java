package com.wearezeta.auto.common.backend.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Consents {
    public enum Type {
        MARKETING(2);

        private final int type;

        Type(int type) {
            this.type = type;
        }

        public int getInt() {
            return type;
        }
    }

    private Map<Type, Boolean> values = new HashMap<>();

    public static Consents fromJson(String jsonString) {
        Consents consents = new Consents();
        putValuesFromJson(consents, jsonString);
        return consents;
    }

    private static void putValuesFromJson(Consents consents, String jsonString) {
        parseJson(jsonString).ifPresent(jsonObject -> {
            if (jsonObject.has("results")) {
                JSONArray results = (JSONArray) jsonObject.get("results");
                putValuesFromResults(consents, results);
            }
        });
    }

    private static void putValuesFromResults(Consents consents, JSONArray results) {
        for (int i = 0; i < results.length(); i++) {
            JSONObject resultObject = results.getJSONObject(i);
            int type = resultObject.getInt("type");
            boolean value = resultObject.getInt("value") == 1;
            try {
                consents.addValue(getTypeByInt(type), value);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private static Type getTypeByInt(int typeId) {
        for (Type type : Type.values()) {
            if (type.getInt() == typeId) {
                return type;
            }
        }
        throw new IllegalStateException(String.format("No Type with int: %s", typeId));
    }

    private static Optional<JSONObject> parseJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return Optional.empty();
        }
    }

    private void addValue(Type type, Boolean value) {
        values.put(type, value);
    }

    public boolean hasConsent(Type type) {
        if (values == null) {
            return false;
        }
        if (!values.containsKey(type)) {
            return false;
        }
        return values.get(type);
    }

}
