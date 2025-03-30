package com.wearezeta.auto.common.calling2.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BackendDTO {

    private String name;
    private final String webappUrl;
    private final String backendUrl;
    private final String websocketUrl;

    public BackendDTO(@JsonProperty("name") String name,
                      @JsonProperty("webappUrl") String webappUrl,
                      @JsonProperty("backendUrl") String backendUrl,
                      @JsonProperty("websocketUrl") String websocketUrl) {
        this.name = name;
        this.webappUrl = webappUrl;
        this.backendUrl = backendUrl;
        this.websocketUrl = websocketUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getWebappUrl() {
        return webappUrl;
    }

    public String getBackendUrl() {
        return this.backendUrl;
    }

    public String getWebsocketUrl() {
        return this.websocketUrl;
    }

    public static BackendDTO fromBackend(com.wearezeta.auto.common.backend.Backend backend) {
        return new BackendDTO(backend.getBackendName(), backend.getWebappUrl(), backend.getBackendUrl(),
                backend.getBackendWebsocket());
    }

}