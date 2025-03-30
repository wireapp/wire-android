package com.wearezeta.auto.common.backend.models;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Label {

    private String id;
    private String name;
    private List<String> conversations;
    private Integer type;

    protected Label() {
    }

    public Label(String id, String name, List<String> conversations, Integer type) {
        this.id = id;
        this.name = name;
        this.conversations = conversations;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getConversations() {
        return conversations;
    }

    public void setConversations(List<String> conversations) {
        this.conversations = conversations;
    }

    public int getType() {
        return type;
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        object.put("id", this.id);
        object.put("name", this.name);
        JSONArray array = new JSONArray();
        for (String conversation : this.conversations) {
            array.put(conversation);
        }
        object.put("conversations", array);
        object.put("type", this.type);
        return object;
    }

    public static Label fromJSON(JSONObject object) {
        List<String> conversations = new ArrayList<>();
        JSONArray array = object.getJSONArray("conversations");
        for (int i = 0; i < array.length(); i++) {
            conversations.add(array.getString(i));
        }
        //Check what type (type 1 == favorites
        if (object.getInt("type") == 1) {
            //favorites doesn't have a name according to spec
            return new Label(object.getString("id"), "", conversations, object.getInt("type"));
        } else {
            return new Label(object.getString("id"), object.getString("name"), conversations, object.getInt("type"));
        }
    }

}
