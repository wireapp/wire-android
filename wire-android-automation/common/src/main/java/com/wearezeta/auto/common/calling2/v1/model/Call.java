package com.wearezeta.auto.common.calling2.v1.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Call {

    private String id;
    private CallStatus status;
    private String conversationId;
    private Long timeout;
    private Long creationTime;
    private Metrics metrics;

    @JsonCreator
    public Call(
            @JsonProperty("id") String id, 
            @JsonProperty("status") CallStatus status, 
            @JsonProperty("conversationId") String conversationId, 
            @JsonProperty("timeout") Long timeout, 
            @JsonProperty("creationTime") Long creationTime, 
            @JsonProperty("metrics") Metrics metrics) {
        this.id = id;
        this.status = status;
        this.conversationId = conversationId;
        this.timeout = timeout;
        this.creationTime = creationTime;
        this.metrics = metrics;
    }
    
    

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Long creationTime) {
        this.creationTime = creationTime;
    }

    @Nullable
    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return "Call{id=" + id + ", status=" + status + ", conversationId=" + conversationId + ", timeout=" + timeout + ", creationTime=" + creationTime + ", metrics=" + metrics + '}';
    }

}
