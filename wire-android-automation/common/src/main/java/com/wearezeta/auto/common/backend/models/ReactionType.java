package com.wearezeta.auto.common.backend.models;

public enum ReactionType {
    LIKE("❤️"), UNLIKE("");

    private final String asString;

    ReactionType(String asString) {
        this.asString = asString;
    }

    @Override
    public String toString() {
        return asString;
    }
}