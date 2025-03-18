package com.wearezeta.auto.common.backend.models;

import org.json.JSONObject;

public class Team {
    private String id;
    private String name;

    // TODO: Probably, it makes sense to use automatic serialization here
    public Team() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Team fromJSON(JSONObject team) {
        final Team result = new Team();
        result.setId(team.getString("id"));
        result.setName(team.getString("name"));
        return result;
    }
}
