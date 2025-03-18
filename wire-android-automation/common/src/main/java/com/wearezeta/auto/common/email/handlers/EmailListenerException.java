package com.wearezeta.auto.common.email.handlers;

import com.wearezeta.auto.common.rest.RESTError;

public class EmailListenerException extends RESTError {

	public EmailListenerException(String message, int returnCode) {
		super(message, returnCode);
	}

	private static final long serialVersionUID = -77484643126202998L;
}
