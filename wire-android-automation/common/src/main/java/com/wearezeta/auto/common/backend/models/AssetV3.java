package com.wearezeta.auto.common.backend.models;

import org.json.JSONObject;

public class AssetV3 {

    private String key;
    private String type;
    private String size;

    public AssetV3(String key, String type, String size) {
        this.key = key;
        this.type = type;
        this.size = size;
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("key", key);
        object.put("type", type);
        object.put("size", size);
        return object;
    }
}
