package com.wearezeta.auto.common.email.messages;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TeamInvitationMessage extends WireMessage {
	public TeamInvitationMessage(String msg) throws Exception {
		super(msg);
	}

	public String getXZetaCode() {
		return this.getHeaderValue(ZETA_CODE_HEADER_NAME);
	}

	public String extractInvitationLink() {
		ArrayList<String> links = new ArrayList<>();
		String regex = "(https://.*/join/[^\"<\\s\\[\\]]+)";
		Pattern p = Pattern.compile(regex);
		Matcher urlMatcher = p.matcher(this.getContent());
		while (urlMatcher.find()) {
			links.add(urlMatcher.group(1));
		}
		return links.get(0);
	}

	@Override
	protected String getExpectedPurposeValue() {
		return MESSAGE_PURPOSE;
	}

	public static final String MESSAGE_PURPOSE = "TeamInvitation";
}
