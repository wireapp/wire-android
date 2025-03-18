package com.wearezeta.auto.common.email.messages;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProviderPasswordResetMessage extends WireMessage {

	public ProviderPasswordResetMessage(String msg) throws Exception {
		super(msg);
	}

	public String extractPasswordResetLink() {
		ArrayList<String> links = new ArrayList<String>();

		String regex = "<a href=\"([^\"]*)\"[^>]*>Reset password</a>";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher urlMatcher = p.matcher(this.getContent());
		while (urlMatcher.find()) {
			links.add(urlMatcher.group(1));
		}
		return links.get(0).replaceAll("&amp;", "&");
	}

	public static final String MESSAGE_PURPOSE = "ProviderPasswordReset";

	@Override
	protected String getExpectedPurposeValue() {
		return MESSAGE_PURPOSE;
	}
}
