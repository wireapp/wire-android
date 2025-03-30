package com.wearezeta.auto.common.calling2.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class InstanceRequest {

	private final String email;
	private final String password;
	private final String verificationCode;
	private final String backend;
	private final BackendDTO customBackend;
	private final VersionedInstanceType instanceType;
	private final String name;
	private final long timeout;
	private final boolean beta;

	public InstanceRequest(String email, String password, String verificationCode, String backend,
						   BackendDTO customBackend, VersionedInstanceType instanceType, String name, boolean beta,
						   long timeout) {
		this.email = email;
		this.password = password;
		this.verificationCode = verificationCode;
		this.backend = backend;
		this.customBackend = customBackend;
		this.instanceType = instanceType;
		this.name = name;
		this.beta = beta;
		this.timeout = timeout;
	}

	public String getEmail() {
		return email;
	}

	public String getPassword() {
		return password;
	}

	public String getVerificationCode() {
		return verificationCode;
	}

	public String getBackend() {
		return backend;
	}

	public BackendDTO getCustomBackend() {
		return customBackend;
	}

	public VersionedInstanceType getInstanceType() {
		return instanceType;
	}

	public long getTimeout() {
		return timeout;
	}

	public boolean getBeta() {
		return beta;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "InstanceRequest{email=" + email + ", password=" + password
				+ ", environment=" + backend + ", customBackend=" + customBackend.getBackendUrl() + " | " + customBackend.getWebappUrl() + ", instanceType=" + instanceType
				+ ", name=" + name + ", timeout=" + timeout + ", beta=" + beta +'}';
	}

}
