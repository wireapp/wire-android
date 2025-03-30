package com.wearezeta.auto.common.backend.models;

public enum MuteState {
    NONE(0),
    MENTIONS_ONLY(0b01),
    MUTE_ALL(0b11);

    private final int bitmaskValue;

    MuteState(int bitmaskValue) {
        this.bitmaskValue = bitmaskValue;
    }

    public int getBitmaskValue() {
        return bitmaskValue;
    }
}
