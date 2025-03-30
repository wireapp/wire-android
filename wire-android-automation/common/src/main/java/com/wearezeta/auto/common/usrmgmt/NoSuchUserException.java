package com.wearezeta.auto.common.usrmgmt;

public class NoSuchUserException extends RuntimeException {
	private static final long serialVersionUID = 8400419658588574129L;

	public NoSuchUserException(String message) {
		super(message);
	}
}
