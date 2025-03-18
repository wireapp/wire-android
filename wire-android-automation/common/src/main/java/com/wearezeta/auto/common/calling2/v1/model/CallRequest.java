package com.wearezeta.auto.common.calling2.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CallRequest {
	private final static long DEFAULT_CALL_TIMEOUT = 1000L * 60 * 60 * 2; // milliseconds

	private final String conversationId;
	private final Long timeout;

	public CallRequest(String conversationId) {
		this.conversationId = conversationId;
		this.timeout = DEFAULT_CALL_TIMEOUT;
	}

	public CallRequest() {
		this.conversationId = "";
		this.timeout = DEFAULT_CALL_TIMEOUT;
	}

	public String getConversationId() {
		return conversationId;
	}

	public Long getTimeout() {
		return timeout;
	}

	@Override
	public String toString() {
		return "CallRequest{conversationId=" + conversationId
				+ ", timeout=" + timeout + '}';
	}

}
