package com.wearezeta.auto.common.calling2.v1.model;

import java.util.Arrays;

public enum InstanceStatus {
	NON_EXISTENT, STARTING, STARTED, STOPPING, STOPPED, LOGIN_FAILED, DESTROYED, ERROR;

	public static boolean isContainedInSubset(InstanceStatus[] subSet,
			InstanceStatus item) {
		for (InstanceStatus status : subSet) {
			if (item == status) {
				return true;
			}
		}
		return false;
	}

	public static InstanceStatus fromString(String s) {
		return Arrays.stream(InstanceStatus.values())
				.filter(x -> x.name().equalsIgnoreCase(s))
				.findFirst()
				.orElseThrow(
						() -> new IllegalArgumentException(String.format("Instance status '%s' is unknown", s))
				);
	}
}
