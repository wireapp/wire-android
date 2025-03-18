package com.wearezeta.auto.common.service;

import com.wearezeta.auto.common.backend.models.Team;

public class ServiceInfo {
    private final Team team;
    private final String name;
    private boolean enabled;
    private final String providerId;
    private final String serviceId;

    public String getTeamId() {
        return team.getId();
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public ServiceInfo(Team team, String name, boolean enabled) {
        this.team = team;
        this.name = name;
        this.enabled = enabled;
        switch (name) {
            case "Echo":
                this.providerId = Services.ECHO_SERVICE.getProviderId();
                this.serviceId = Services.ECHO_SERVICE.getServiceId();
                break;
            case "Poll Bot":
                this.providerId = Services.POLL_SERVICE.getProviderId();
                this.serviceId = Services.POLL_SERVICE.getServiceId();
                break;
            case "Tracker":
                this.providerId = Services.TRACKER_SERVICE.getProviderId();
                this.serviceId = Services.TRACKER_SERVICE.getServiceId();
                break;
            default:
                throw new IllegalArgumentException("No such service");
        }
    }
}
