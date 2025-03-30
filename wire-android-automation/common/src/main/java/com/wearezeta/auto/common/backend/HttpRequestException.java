package com.wearezeta.auto.common.backend;

public class HttpRequestException extends RuntimeException {
	private static final long serialVersionUID = -5694123643050116766L;
	private int returnCode = -1;

	public int getReturnCode() {
		return this.returnCode;
	}

	public HttpRequestException(String message) {
		super(message);
	}

	public HttpRequestException(String message, int returnCode) {
		super(String.format("%s (%s)", message, returnCode));
		this.returnCode = returnCode;
	}
}
