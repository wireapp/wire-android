package com.wearezeta.auto.common.email.handlers;

import com.wearezeta.auto.common.email.inbucket.models.Message;
import com.wearezeta.auto.common.misc.Timedelta;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class InbucketChangesListener extends AbstractMailboxChangesListener {

    private final InbucketMailbox mailbox;
    // Example date: "2022-07-13T14:59:38.314900766Z"
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'");

    public InbucketChangesListener(InbucketMailbox mailbox, Map<String, String> expectedHeaders,
                                   Timedelta timeout, Timedelta rejectMessagesBefore) {
        super(mailbox, expectedHeaders, timeout, rejectMessagesBefore);
        this.mailbox = mailbox;
    }

    @Override
    public String call() throws Exception {
        final String deliveredTo = mailbox.getEmailAddress();
        final Timedelta started = Timedelta.now();
        do {
            final List<Message> messages = this.mailbox.getRecentMessages(deliveredTo);
            log.info(String.format("Got %s incoming message(s) for %s", messages.size(), deliveredTo));
            for (Message message: messages)  {
                if (this.areAllHeadersInMessage(message)) {
                    Instant messageDate = Instant.parse(message.getDate());
                    Instant listeningStartDate = Instant.ofEpochSecond(rejectMessagesBefore.asSeconds());
                    if (messageDate.isAfter(listeningStartDate)) {
                        log.info(String.format(
                                "Message accepted because message date (%s) is after start of listening (%s)",
                                messageDate,
                                listeningStartDate));
                        return this.mailbox.getMessageSource(deliveredTo, message.getId());
                    }
                    log.severe(String.format(
                            "Message rejected because message date (%s) is before start of listening (%s)",
                            messageDate,
                            listeningStartDate));
                }
            }
            Timedelta.ofSeconds(2).sleep();
        } while (Timedelta.now().isDiffLessOrEqual(started, timeout));
        throw new RuntimeException(String.format("Email message containing headers %s has not been found within %s",
                this.expectedHeaders.toString(), this.timeout));
    }

    protected boolean areAllHeadersInMessage(Message message) {
        for (Map.Entry<String, String> expectedHeader : this.expectedHeaders.entrySet()) {
            boolean isHeaderFound = false;
            final String expectedHeaderName = expectedHeader.getKey();
            final String expectedHeaderValue = expectedHeader.getValue();
                List<String> headerValues = message.getHeader().get(expectedHeaderName);
                log.info(String.format("Checking if the email message contains %s: %s header",
                        expectedHeaderName, expectedHeaderValue));
                if (headerValues != null) {
                    for (String headerValue : headerValues) {
                        log.info(String.format("%s: %s -> %s", expectedHeaderName, headerValue, expectedHeaderValue));
                        if (headerValue.equals(expectedHeaderValue)) {
                            log.info(String.format("The expected header value '%s' is found in the email",
                                    expectedHeaderValue));
                            isHeaderFound = true;
                            break;
                        }
                    }
                }
                log.info(String.format("Header %s with value %s found: %s", expectedHeaderName, expectedHeaderValue,
                        isHeaderFound));
            if (!isHeaderFound) {
                return false;
            }
        }
        return true;
    }

}
