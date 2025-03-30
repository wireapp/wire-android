package com.wearezeta.auto.common.service;

enum Services {
    POLL_SERVICE ("d1e52fa0-46bc-46fa-acc1-95bd91735de1", "40085205-4499-4cd7-a093-ca7d3c1d8b21"),
    ECHO_SERVICE ("d64af9ae-e0c5-4ce6-b38a-02fd9363b54c", "d693bd64-79ae-4970-ad12-4df49cfe4038"),
    TRACKER_SERVICE ("d64af9ae-e0c5-4ce6-b38a-02fd9363b54c", "7ba4aac9-1bbb-41bd-b782-b57157665157");

    private final String providerId;
    private final String serviceId;

    Services(String providerId, String serviceId) {
        this.providerId = providerId;
        this.serviceId = serviceId;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public String getServiceId() {
        return this.serviceId;
    }
}
