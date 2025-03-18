package com.wearezeta.auto.common.email.handlers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.rest.CommonRESTHandlers;

import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.wearezeta.auto.common.log.ZetaLogger;


public class EmailListenerMailbox implements ISupportsMessagesPolling {
    private static final Logger log = ZetaLogger.getLog(EmailListenerMailbox.class.getSimpleName());
    private final int MAX_PARALLEL_EMAIL_LISTENER_TASKS = 4;

    private String mailboxName;
    private String email;

    public String getMailboxName() {
        return mailboxName;
    }

    public EmailListenerMailbox(String email) {
        this.email = email;
        this.mailboxName = normalizeEmail(email);
    }

    protected List<String> getRecentMessages(int minCount, int maxCount, Timedelta timeout) {
        List<String> result = new ArrayList<>();
        final JSONArray recentEmails = EmailListenerAPI.getRecentEmailsForUser(this.email, minCount, maxCount, timeout);
        for (int i = 0; i < recentEmails.length(); i++) {
            final JSONObject recentEmailInfo = recentEmails.getJSONObject(i);
            result.add(recentEmailInfo.getString("raw_text"));
        }
        return result;
    }

    private final ExecutorService pool = Executors.newFixedThreadPool(MAX_PARALLEL_EMAIL_LISTENER_TASKS);

    @Override
    public Future<String> getMessage(Map<String, String> expectedHeaders, Timedelta timeout) {
        Timedelta rejectMessagesBefore = Timedelta.ofMillis(0);
        return getMessage(expectedHeaders, timeout, rejectMessagesBefore);
    }

    @Override
    public Future<String> getMessage(Map<String, String> expectedHeaders, Timedelta timeout, Timedelta rejectMessagesBefore) {
        EmailListenerChangesListener listener = new EmailListenerChangesListener(this, expectedHeaders, timeout,
                rejectMessagesBefore);
        log.fine(String.format("Started email listener for message containing headers %s...",
                expectedHeaders.toString()));
        return pool.submit(listener);
    }

    @Override
    public boolean waitUntilMessagesCountReaches(String deliveredTo, int expectedMsgsCount, Timedelta timeout) {
        return getRecentMessages(expectedMsgsCount, expectedMsgsCount, timeout).size() >= expectedMsgsCount;
    }

    @Override
    public boolean isAlive() {
        try {
            return CommonRESTHandlers.isAlive(
                    new URL(String.format("%s/recent_emails/%s/0/0", EmailListenerAPI.getApiRoot(), this.mailboxName))
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String normalizeEmail(String originalEmail) {
        final int plusPos = originalEmail.indexOf("+");
        if (plusPos > 0) {
            return originalEmail.substring(0, plusPos) + originalEmail.substring(
                    originalEmail.indexOf("@")
            );
        }
        return originalEmail;
    }
}
