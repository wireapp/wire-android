package com.wearezeta.auto.common.backend.models;

public class Service {

    private String id;
    private String provider;

    public Service(String id, String provider) {
        this.id = id;
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }
}
