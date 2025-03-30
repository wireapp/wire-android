package com.wearezeta.auto.common.backend.models;

public class TeamMember {

    private String userId;
    private TeamRole role;

    public TeamMember(String userId, TeamRole role) {
        this.userId = userId;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public TeamRole getRole() {
        return role;
    }
}
