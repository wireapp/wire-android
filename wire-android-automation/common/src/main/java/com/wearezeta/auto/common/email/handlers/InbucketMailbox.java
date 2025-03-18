package com.wearezeta.auto.common.email.handlers;

import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.email.inbucket.InbucketClient;
import com.wearezeta.auto.common.email.inbucket.models.Message;
import com.wearezeta.auto.common.email.inbucket.models.MessageInfo;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.misc.Timedelta;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class InbucketMailbox implements ISupportsMessagesPolling {

    private static final Logger log = ZetaLogger.getLog(InbucketMailbox.class.getSimpleName());
    private final int MAX_PARALLEL_EMAIL_LISTENER_TASKS = 4;

    private String emailAddress;
    private InbucketClient client;

    public InbucketMailbox(Backend backend, String emailAddress) {
        this.emailAddress = emailAddress;
        this.client = new InbucketClient(
                backend.getInbucketUrl(),
                backend.getInbucketUsername(),
                backend.getInbucketPassword(),
                backend.useProxy()
        );
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    @Override
    public boolean waitUntilMessagesCountReaches(String deliveredTo, int expectedMsgsCount, Timedelta timeout) throws Exception {
        List<Message> recentMessages = getRecentMessages(deliveredTo);
        long start = System.currentTimeMillis();
        long end = start + timeout.asMillis();
        while (System.currentTimeMillis() < end && recentMessages.size() < expectedMsgsCount) {
            Thread.sleep(1000);
            recentMessages = getRecentMessages(deliveredTo);
        }
        return getRecentMessages(deliveredTo).size() >= expectedMsgsCount;
    }

    private final ExecutorService pool = Executors.newFixedThreadPool(MAX_PARALLEL_EMAIL_LISTENER_TASKS);

    @Override
    public Future<String> getMessage(Map<String, String> expectedHeaders, Timedelta timeout) throws Exception {
        Timedelta rejectMessagesBefore = Timedelta.ofMillis(0);
        return getMessage(expectedHeaders, timeout, rejectMessagesBefore);
    }

    @Override
    public Future<String> getMessage(Map<String, String> expectedHeaders, Timedelta timeout,
                                     Timedelta rejectMessagesBefore) throws Exception {
        InbucketChangesListener listener = new InbucketChangesListener(this, expectedHeaders, timeout,
                rejectMessagesBefore);
        log.fine(String.format("Started email listener for message containing headers %s...",
                expectedHeaders.toString()));
        return pool.submit(listener);
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    public List<Message> getRecentMessages(String deliveredTo) throws IOException {
        List<MessageInfo> messageInfos = client.getMailbox(deliveredTo);
        return messageInfos.stream().map(mi -> {
            try {
                return client.getMessage(deliveredTo, mi.getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public String getMessageSource(String deliveredTo, String messageId) throws IOException {
        return client.getMessageSource(deliveredTo, messageId);
    }
}
