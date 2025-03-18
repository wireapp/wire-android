package com.wearezeta.auto.common.calling2.v1.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionedInstanceType {

	private final String name;
	private final String version;

	public VersionedInstanceType(@JsonProperty("name") String name,
			@JsonProperty("version") String version) {
		this.name = name;
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getPath() {
		if (isOSX()) {
			if (name.toLowerCase().contains("firefox")) {
				return "/" + name + "/Contents/MacOS/firefox";
			} else if (name.toLowerCase().contains("chrome")) {
				return "/" + name + "/Contents/MacOS/Google Chrome";
			} else {
				return "/" + name + "-" + version + "/zcall";
			}
		} else {
			switch (name) {
			case "chrome":
				return "/" + name + "-" + version + "/google-chrome";
			default:
				return "/" + name + "-" + version + "/" + name;
			}
		}
	}

	public static VersionedInstanceType parse(String text) {
		String[] parts = text.split("-", 2);
		if (parts.length == 2) {
			String name = parts[0];
			String version = parts[1];
			return new VersionedInstanceType(name, version);
		}
		return new VersionedInstanceType("", "");
	}

	public boolean isFirefox() {
		return this.getName().toLowerCase().contains("firefox");
	}

	public boolean isChrome() {
		return this.getName().toLowerCase().contains("chrome");
	}

	private boolean isOSX() {
		return System.getProperty("os.name").contains("OS X");
	}

	public boolean isZCall() {
		return this.getName().toLowerCase().contains("zcall");
	}

	@Override
	public String toString() {
		return "VersionedInstanceType{name=" + name + ", version="
				+ version + '}';
	}

}
