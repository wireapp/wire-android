package com.wearezeta.auto.common.email;

import com.wearezeta.auto.common.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

public class MessagingUtils {

    public static String getDefaultAccountName() {
        return Config.current().getDefaultEmail(MessagingUtils.class);
    }

    public static String getSpecialAccountName() {
        return Config.current().getSpecialEmail(MessagingUtils.class);
    }

    public static String getSpecialAccountPassword() {
        return Config.current().getSpecialEmailPassword(MessagingUtils.class);
    }

    public static String generateEmail(String basemail, String suffix) {
        // FIXME: This is only a hack. Maybe remove MessagingUtils class completely and find a better solution
        if (basemail == null || basemail.isEmpty() || basemail.equals("true")) {
            return suffix + "@wire.engineering";
        }
        return basemail.split("@")[0].concat("+").concat(suffix)
                .concat("@").concat(basemail.split("@")[1]);
    }

    public static String get2FAVerificationCode(String emailContent) {
        String code = null;
        Pattern p = Pattern.compile("([0-9]{6})", Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = p.matcher(emailContent);
        while (urlMatcher.find()) {
            code = urlMatcher.group(0);
        }
        assertThat("Could not find code in mail", code, not(emptyOrNullString()));
        return code;
    }

    public static String getRecipientEmailFromHeader(String emailContent) {
        Pattern p = Pattern.compile("To: <?(.*@wire.engineering)>?", Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = p.matcher(emailContent);
        while (urlMatcher.find()) {
            return urlMatcher.group(1);
        }
        throw new RuntimeException("Could not find recipient email address in mail:" + emailContent);
    }
}
