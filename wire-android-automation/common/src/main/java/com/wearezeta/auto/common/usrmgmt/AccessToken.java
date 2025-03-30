package com.wearezeta.auto.common.usrmgmt;

import java.time.LocalDateTime;

import com.wearezeta.auto.common.log.ZetaLogger;

import java.util.logging.Logger;

public class AccessToken {

    private static final Logger log = ZetaLogger.getLog(AccessToken.class.getSimpleName());

    private final String value;
    private String type;
    private LocalDateTime expiresOnDate;

    public AccessToken(String value, String type, long expiresIn) {
        this.type = type;
        this.value = value;
        this.expiresOnDate = LocalDateTime.now().plusSeconds(expiresIn - 15);
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean isInvalid() {
        return (value == null || type == null);
    }

    public boolean isExpired() {
        boolean isExpired = LocalDateTime.now().isAfter(expiresOnDate);
        if (isExpired) {
            log.fine("Access token is expired");
        }
        return isExpired;
    }
}
