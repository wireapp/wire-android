package com.wearezeta.auto.common.backend.models;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class BasicAuth {
    String user;
    String password;
    String encoded;

    public BasicAuth(String user, String password) {
        this.user = user;
        this.password = password;

        try {
            this.encoded = Base64.getEncoder().encodeToString((user + ":" + password).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not compile basic auth credentials for backend", e);
        }
    }

    public BasicAuth(String encoded) {
        this.encoded = encoded;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getEncoded() {
        return "Basic " + encoded;
    }
}
