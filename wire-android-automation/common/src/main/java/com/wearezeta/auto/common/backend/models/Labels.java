package com.wearezeta.auto.common.backend.models;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

public class Labels {

    public static List<Label> fromJSON(JSONArray array) {
        List<Label> labels = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            labels.add(Label.fromJSON(array.getJSONObject(i)));
        }
        return labels;
    }

    public static JSONArray toJSON(List<Label> labels) {
        JSONArray array = new JSONArray();
        for (Label label : labels) {
            array.put(label.toJSON());
        }
        return array;
    }

}
