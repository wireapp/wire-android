package com.wearezeta.auto.common.testrail;

import com.wearezeta.auto.common.rest.RESTError;

public class TestrailRequestException extends RESTError {
	private static final long serialVersionUID = -5694123643050116766L;

	public TestrailRequestException(String message, int returnCode) {
		super(message, returnCode);
	}
}
