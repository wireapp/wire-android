package com.wearezeta.auto.common.calling2.v1.model;

public enum CallStatus {

	NON_EXISTENT, WAITING, CONNECTING, ACTIVE, SPEAKING, MUTED, DESTROYED, ERROR;

	public static boolean isContainedInSubset(CallStatus[] subSet,
			CallStatus item) {
		for (CallStatus status : subSet) {
			if (item == status) {
				return true;
			}
		}
		return false;
	}
}
