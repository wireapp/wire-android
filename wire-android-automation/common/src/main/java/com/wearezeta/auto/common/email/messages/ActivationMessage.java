package com.wearezeta.auto.common.email.messages;

import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wearezeta.auto.common.misc.Timedelta;

public class ActivationMessage extends WireMessage {

	public static final Timedelta ACTIVATION_TIMEOUT = Timedelta.ofSeconds(120); // seconds

	public ActivationMessage(String msg) throws Exception {
		super(msg);
	}

    public String getXZetaKey() {
        return this.getHeaderValue(ZETA_KEY_HEADER_NAME);
    }

    public String getXZetaCode() {
        return this.getHeaderValue(ZETA_CODE_HEADER_NAME);
    }

	public String getActivationLink() throws Exception {
		String regex = "https://[a-zA-Z_0-9.=-]+/verify/\\?key=([a-zA-Z_0-9=-]+)&code=([0-9]+)";
		Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher urlMatcher = p.matcher(this.getContent());
		if (urlMatcher.find()) {
			return urlMatcher.group(0);
		} else {
			throw new Exception("Unable to extract URL from mail: " + getContent());
		}
	}

	public static String getMessageContent(Future<String> activationMessage) throws Exception {
		ActivationMessage sentence = new ActivationMessage(activationMessage.get());
		return sentence.getContent();
	}

	public static final String MESSAGE_PURPOSE = "Activation";

	@Override
	protected String getExpectedPurposeValue() {
		return MESSAGE_PURPOSE;
	}
}
