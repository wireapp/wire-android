package com.wearezeta.auto.common.email.handlers;

import java.util.List;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.wearezeta.auto.common.email.messages.BackendMessage;
import com.wearezeta.auto.common.misc.Timedelta;

class EmailListenerChangesListener extends AbstractMailboxChangesListener {

    @Override
    protected EmailListenerMailbox getParentMbox() {
        return (EmailListenerMailbox) this.parentMBox;
    }

    public EmailListenerChangesListener(EmailListenerMailbox parentMBox, Map<String, String> expectedHeaders,
                                        Timedelta timeout, Timedelta rejectMessagesBefore) {
        super(parentMBox, expectedHeaders, timeout, rejectMessagesBefore);
    }

    @Override
    public String call() throws Exception {
        final Timedelta started = Timedelta.now();
        do {
            final List<String> deliveredRawMessages = this.getParentMbox().getRecentMessages(0, -1, Timedelta.ofMillis(this.timeout.asMillis() / 3));
            log.info(String.format("Got %s incoming message(s) in %s", deliveredRawMessages.size(),
                    getParentMbox().getMailboxName()));
            for (String deliveredRawMessage : deliveredRawMessages) {
                final Message foundMsg = BackendMessage.stringToMsg(deliveredRawMessage);
                if (this.areAllHeadersInMessage(foundMsg)) {
                    log.info("Message found at: " + Timedelta.ofMillis(foundMsg.getSentDate().getTime()).toString());
                    // Remove milliseconds because mails don't contain the date that precise
                    long before = rejectMessagesBefore.asMillis() / 1000 * 1000;
                    log.info("Looking for mails before " + before);
                    if (foundMsg.getSentDate().getTime() >= before) {
                        log.info("\tMessage accepted by timestamp");
                        return deliveredRawMessage;
                    }
                    log.severe("\t!!! Message rejected because it is outdated");
                }
            }
            Timedelta.ofSeconds(2).sleep();
        } while (Timedelta.now().isDiffLessOrEqual(started, timeout));
        throw new RuntimeException(String.format("Email message containing headers %s has not been found within %s",
                this.expectedHeaders.toString(), this.timeout));
    }

    private boolean areAllHeadersInMessage(Message msg) {
        for (Map.Entry<String, String> expectedHeader : this.expectedHeaders.entrySet()) {
            boolean isHeaderFound = false;
            final String expectedHeaderName = expectedHeader.getKey();
            final String expectedHeaderValue = expectedHeader.getValue();
            try {
                String[] headerValues = null;
                try {
                    headerValues = msg.getHeader(expectedHeaderName);
                } catch (NullPointerException e) {
                    log.fine("Ignore NPE bug in java mail lib");
                }
                log.fine(String.format("Checking if the email message contains %s: %s header",
                        expectedHeaderName, expectedHeaderValue));
                if (headerValues != null) {
                    for (String headerValue : headerValues) {
                        log.fine(String.format("%s: %s -> %s", expectedHeaderName, headerValue, expectedHeaderValue));
                        if (headerValue.equals(expectedHeaderValue)) {
                            log.fine(String.format("The expected header value '%s' is found in the email",
                                    expectedHeaderValue));
                            isHeaderFound = true;
                            break;
                        }
                    }
                }
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            if (!isHeaderFound) {
                return false;
            }
        }
        return true;
    }
}
