package com.wearezeta.auto.common;

import java.util.NoSuchElementException;

public enum Platform {
    Mac("Mac"), Windows("win"), Android("Android"), iOS("iOS"), Web("ANY");

    private final String name;

    public String getName() {
        return this.name;
    }

    Platform(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public static Platform getByName(String name) {
        for (Platform p : Platform.values()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new NoSuchElementException(String.format(
                "Platform '%s' is unknown", name));
    }
}
