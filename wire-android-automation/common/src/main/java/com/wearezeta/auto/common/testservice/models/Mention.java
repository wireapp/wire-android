package com.wearezeta.auto.common.testservice.models;

public class Mention {

    private int length;
    private int start;
    private String userId;
    private String userDomain;

    public Mention(int length, int start, String userId) {
        this.length = length;
        this.start = start;
        this.userId = userId;
        this.userDomain = "";
    }

    public Mention(int length, int start, String userId, String userDomain) {
        this.length = length;
        this.start = start;
        this.userId = userId;
        this.userDomain = userDomain;
    }

    public int getLength() {
        return length;
    }

    public int getStart() {
        return start;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserDomain() {
        return userDomain;
    }
}
