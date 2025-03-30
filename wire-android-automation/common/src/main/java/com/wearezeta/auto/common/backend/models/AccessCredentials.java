package com.wearezeta.auto.common.backend.models;

import com.wearezeta.auto.common.usrmgmt.AccessToken;
import com.wearezeta.auto.common.usrmgmt.AccessCookie;

public class AccessCredentials {

    private AccessToken accessToken;
    private AccessCookie accessCookie;

    public AccessCredentials(AccessToken token, AccessCookie cookie) {
        this.accessToken = token;
        this.accessCookie = cookie;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public AccessCookie getAccessCookie() {
        return accessCookie;
    }
}
