package com.wearezeta.auto.common.email;

import com.wearezeta.auto.common.backend.Backend;
import com.wearezeta.auto.common.email.handlers.EmailListenerMailbox;
import com.wearezeta.auto.common.email.handlers.ISupportsMessagesPolling;
import com.wearezeta.auto.common.email.handlers.InbucketMailbox;
import com.wearezeta.auto.common.usrmgmt.ClientUser;

public class MailboxProvider {

    public static ISupportsMessagesPolling getInstance(Backend backend, String email) throws Exception {
        if (backend.getInbucketUrl() == null) {
            return new EmailListenerMailbox(email);
        } else {
            return new InbucketMailbox(backend, email);
        }
    }

}
