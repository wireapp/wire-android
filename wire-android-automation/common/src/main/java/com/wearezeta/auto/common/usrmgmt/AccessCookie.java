package com.wearezeta.auto.common.usrmgmt;

import javax.ws.rs.core.NewCookie;

import java.net.HttpCookie;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AccessCookie {

    private final String name;
    private final Date expirationDate;
    private final String value;

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean isExpired() {
        return new Date().after(expirationDate);
    }

    public AccessCookie(String cookieName, Map<String, NewCookie> cookies) {
        if (!cookies.containsKey(cookieName)) {
            throw new RuntimeException(String.format("No cookie found with name '%s'", cookieName));
        }
        NewCookie newCookie = cookies.get(cookieName);
        this.name = cookieName;
        this.value = newCookie.getValue();
        this.expirationDate = newCookie.getExpiry();
    }

    public AccessCookie(String cookieName, List<HttpCookie> cookies) {
        HttpCookie newCookie = cookies.stream().filter(x -> x.getName().equals(cookieName)).findFirst().get();
        this.name = cookieName;
        this.value = newCookie.getValue();
        Date now = new Date();
        this.expirationDate = new Date(now.getTime() + (newCookie.getMaxAge() * 1000));
    }
}
