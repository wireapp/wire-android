package com.wearezeta.auto.common.calling2.v1.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Instance {

	private final String id;
	private final InstanceStatus instanceStatus;

	private final String email;
	private final String password;
	private final Call currentCall;
	private final String backend;
	private final VersionedInstanceType instanceType;
	private final String screenshot;
	private final String name;
	private final long timeout;
	private final long created;
	private String message;

	@JsonCreator
	public Instance(@JsonProperty("id") String id,
			@JsonProperty("instanceStatus") InstanceStatus instanceStatus,
			@JsonProperty("currentCall") Call currentCall,
			@JsonProperty("email") String email,
			@JsonProperty("password") String password,
			@JsonProperty("backend") String backend,
			@JsonProperty("screenshot") String screenshot,
			@JsonProperty("instanceType") VersionedInstanceType instanceType,
			@JsonProperty("name") String name,
			@JsonProperty("timeout") long timeout,
			@JsonProperty("created") long created,
			@JsonProperty("message") String message) {
		this.id = id;
		this.instanceStatus = instanceStatus;
		this.currentCall = currentCall;
		this.email = email;
		this.password = password;
		this.backend = backend;
		this.screenshot = screenshot;
		this.instanceType = instanceType;
		this.name = name;
		this.timeout = timeout;
		this.created = created;
		this.message = message;
	}

	public String getId() {
		return id;
	}

	public InstanceStatus getStatus() {
		return instanceStatus;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public VersionedInstanceType getInstanceType() {
		return instanceType;
	}

	public long getTimeout() {
		return timeout;
	}

	public long getCreated() {
		return created;
	}

	public Call getCurrentCall() {
		return currentCall;
	}

	public String getLivePreview() {
		return screenshot;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		if (id == null && instanceStatus == null && message != null) return message;
		return "Instance{id=" + id + ", instanceStatus=" + instanceStatus
				+ ", email=" + email + ", password=" + password
				+ ", currentCall=" + currentCall + ", backend=" + backend
				+ ", instanceType=" + instanceType
				+ ", screenshot=<suppressed>, name=" + name + ", timeout="
				+ timeout + ", created=" + created + '}';
	}

}
