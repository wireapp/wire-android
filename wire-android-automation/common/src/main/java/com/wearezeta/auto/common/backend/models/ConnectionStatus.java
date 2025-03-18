package com.wearezeta.auto.common.backend.models;

import java.util.Arrays;

public enum ConnectionStatus {
	Accepted, Blocked, Pending, Ignored, Sent, Cancelled;

	@Override
    public String toString() {
	    return this.name().toLowerCase();
    }

	public static ConnectionStatus fromString(String s) {
		return Arrays.stream(ConnectionStatus.values())
				.filter(x -> x.name().equalsIgnoreCase(s))
				.findFirst()
				.orElseThrow(
						() -> new IllegalArgumentException(String.format("Connection status '%s' is unknown", s))
				);
	}
}
