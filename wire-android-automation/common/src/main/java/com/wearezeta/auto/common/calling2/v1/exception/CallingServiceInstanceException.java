package com.wearezeta.auto.common.calling2.v1.exception;

import com.wearezeta.auto.common.rest.RESTError;

public class CallingServiceInstanceException extends RESTError {

	private static final long serialVersionUID = 8661851766035908935L;

	public CallingServiceInstanceException(String message, int returnCode) {
		super(message, returnCode);
	}

	public CallingServiceInstanceException(RESTError exception) {
		super(exception.getMessage(), exception.getReturnCode());
	}

}
