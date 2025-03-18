package com.wearezeta.auto.common.email.messages;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wearezeta.auto.common.misc.Timedelta;
import java.util.logging.Logger;

import com.wearezeta.auto.common.log.ZetaLogger;

public class InvitationMessage extends WireMessage {

	public static final Logger log = ZetaLogger.getLog(InvitationMessage.class.getSimpleName());

	public static final Timedelta INVITATION_RECEIVING_TIMEOUT = Timedelta.ofSeconds(120);

	public InvitationMessage(String msg) throws Exception {
		super(msg);
	}

	public String extractInvitationLink() {
		ArrayList<String> links = new ArrayList<>();
		String regex = "(https://.*/i/[^\"<\\s\\[\\]]+)";
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

	public static final String MESSAGE_PURPOSE = "Invitation";
}
