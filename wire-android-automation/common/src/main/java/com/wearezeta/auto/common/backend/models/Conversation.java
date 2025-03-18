package com.wearezeta.auto.common.backend.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Conversation {
    private String id;
    private QualifiedID qualifiedID;
    private Optional<String> name = Optional.empty();
    /*
        RegularConv = 0 (= any group conversation, = any team conversation)
        SelfConv = 1
        One2OneConv = 2
        ConnectConv = 3
     */
    private Optional<Integer> type = Optional.empty();
    private Optional<String> teamId = Optional.empty();
    private String selfId;
    private List<QualifiedID> otherIds = new ArrayList<>();
    private String creatorId;
    private Integer receiptMode;
    private Integer messageTimerInMilliseconds;

    public Conversation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setQualifiedId(QualifiedID qualifiedID) {
        this.qualifiedID = qualifiedID;
    }

    public QualifiedID getQualifiedID() {
        return qualifiedID;
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Optional.of(name);
    }

    public Optional<Integer> getType() {
        return type;
    }

    public void setType(int type) {
        this.type = Optional.of(type);
    }

    public Optional<String> getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = Optional.of(teamId);
    }

    public List<QualifiedID> getOtherIds() {
        return otherIds;
    }

    public void setOtherIds(List<QualifiedID> otherIds) {
        this.otherIds = new ArrayList<>(otherIds);
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getSelfId() {
        return selfId;
    }

    public void setMessageTimerInMilliseconds(int timeoutInSeconds) {
        this.messageTimerInMilliseconds = timeoutInSeconds;
    }

    public int getMessageTimerInMilliseconds() {
        return messageTimerInMilliseconds;
    }

    public void setSelfId(String selfId) {
        this.selfId = selfId;
    }

    public boolean isReceiptModeEnabled() {
        return receiptMode != null && receiptMode > 0;
    }

    public void setReceiptMode(Integer receiptMode) {
        this.receiptMode = receiptMode;
    }

    public static Conversation fromJSON(JSONObject conversation) {
        final Conversation result = new Conversation();
        result.setId(conversation.getString("id"));

        result.setQualifiedId(QualifiedID.fromJSON(conversation.getJSONObject("qualified_id")));

        if (conversation.has("name") && !conversation.isNull("name")) {
            result.setName(conversation.getString("name").replaceAll("\uFFFC", "").trim());
        }

        if (!conversation.isNull("message_timer")) {
            result.setMessageTimerInMilliseconds(conversation.getInt("message_timer"));
        } else {
            result.setMessageTimerInMilliseconds(0);
        }

        if (conversation.has("type") && !conversation.isNull("type")) {
            result.setType(conversation.getInt("type"));
        }
        if (conversation.has("team") && !conversation.isNull("team")) {
            result.setTeamId(conversation.getString("team"));
        }

        if (conversation.has("qualified_id")) {
            JSONObject qualifiedID = conversation.getJSONObject("qualified_id");

            result.setQualifiedId(QualifiedID.fromJSON(qualifiedID));
        }

        // each 1:1 conversation is created with proteus, but has an "invisible" copy with mls protocol (if mls is enabled)
        // they have no creator, they simply exist between connected users
        // this is intended behavior
        if (conversation.isNull("creator")) {
            result.setCreatorId("null");
        } else {
            result.setCreatorId(conversation.getString("creator"));
        }

        result.setSelfId(conversation.getJSONObject("members").getJSONObject("self").getString("id"));

        final JSONArray others = conversation.getJSONObject("members").getJSONArray("others");

        List<QualifiedID> otherIds = new ArrayList<>();
        for (int i = 0; i < others.length(); ++i) {
            QualifiedID othersQualifiedID = QualifiedID.fromJSON(others.getJSONObject(i).getJSONObject("qualified_id"));
            otherIds.add(othersQualifiedID);
        }
        result.setOtherIds(otherIds);

        if (conversation.has("receipt_mode") && !conversation.isNull("receipt_mode")) {
            result.setReceiptMode((conversation.getInt("receipt_mode")));
        }
        return result;
    }
}