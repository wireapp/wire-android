package com.wearezeta.auto.common.testiny;

import com.wearezeta.auto.common.rest.RESTError;

public class TestinyRequestException extends RESTError {
	private static final long serialVersionUID = -5694123643050116766L;

	public TestinyRequestException(String message, int returnCode) {
		super(message, returnCode);
	}
}
