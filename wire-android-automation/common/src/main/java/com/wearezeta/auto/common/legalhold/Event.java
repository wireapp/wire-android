package com.wearezeta.auto.common.legalhold;

import java.util.Date;

public class Event {
    private String id;
    private String type;
    private Date dateTime;
    private String payload;

    public Event(String id, String type, Date dateTime, String payload) {
        this.id = id;
        this.type = type;
        this.dateTime = dateTime;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public String getPayload() {
        return payload;
    }
}

