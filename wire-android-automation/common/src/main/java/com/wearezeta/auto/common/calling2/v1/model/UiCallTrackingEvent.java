package com.wearezeta.auto.common.calling2.v1.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class UiCallTrackingEvent {
    private UUID callId;
    private LocalDateTime timestamp;
    private NativeCallingEvent event;

    private static final String stringFormat = "[%s](%s) <%s>";

    public UiCallTrackingEvent(UUID callId, NativeCallingEvent event) {
        this.callId = callId;
        timestamp = LocalDateTime.now();
        this.event = event;
    }

    public UUID getCallId() {
        return callId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public NativeCallingEvent getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return String.format(stringFormat, timestamp, callId, event);
    }
}
