package com.wearezeta.auto.common.calling2.v1.exception;

import com.wearezeta.auto.common.rest.RESTError;

public class CallingServiceCallException extends RESTError {

	private static final long serialVersionUID = -4831305426342617547L;

	public CallingServiceCallException(String message, int returnCode) {
		super(message, returnCode);
	}

	public CallingServiceCallException(RESTError exception) {
		super(exception.getMessage(), exception.getReturnCode());
	}

}
