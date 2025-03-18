package com.wearezeta.auto.common.backend.models;

import org.json.JSONObject;

public class Connection {
    private String to;
    private ConnectionStatus status;
    private String domain;

    public Connection() {}

    public static Connection fromJSON(JSONObject connection) {
        final Connection result = new Connection();
        result.setStatus(ConnectionStatus.fromString(connection.getString("status")));
        result.setTo(connection.getString("to"));
        if (connection.has("qualified_to")) {
            result.setDomain(connection.getJSONObject("qualified_to").getString("domain"));
        }
        return result;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }
}
