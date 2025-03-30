package com.wearezeta.auto.common.testservice.models;

public class LegalHoldStatus {

    public static final int UNKNOWN = 0;

    public static final int DISABLED = 1;

    public static final int ENABLED = 2;

    public static int valueOf(String value) {
        switch (value.toUpperCase()) {
            case "UNKNOWN":
                return 0;
            case "DISABLED":
                return 1;
            case "ENABLED":
                return 2;
            default:
                return 0;
        }
    }

}
