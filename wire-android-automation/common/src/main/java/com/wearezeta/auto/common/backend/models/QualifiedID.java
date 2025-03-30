package com.wearezeta.auto.common.backend.models;

import org.json.JSONObject;

public class QualifiedID {

    private String domain;
    private String id;

    public QualifiedID() {}

    public QualifiedID(String id, String domain) {
        this.id = id;
        this.domain = domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getID() {
        return id;
    }

    public String getDomain() {
        return domain;
    }

    public static QualifiedID fromJSON(JSONObject qualifiedIDObject) {
        final QualifiedID result = new QualifiedID();
        result.setID(qualifiedIDObject.getString("id"));
        result.setDomain(qualifiedIDObject.getString("domain"));

        return result;
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("id", this.id);
        object.put("domain", this.domain);
        return object;
    }
}
