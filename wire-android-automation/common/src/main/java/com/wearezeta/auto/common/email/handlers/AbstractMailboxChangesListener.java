package com.wearezeta.auto.common.email.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import com.wearezeta.auto.common.misc.Timedelta;
import java.util.logging.Logger;

import com.wearezeta.auto.common.log.ZetaLogger;

public abstract class AbstractMailboxChangesListener implements Callable<String> {
    protected Map<String, String> expectedHeaders = new HashMap<>();
    protected Object parentMBox;
    protected Timedelta timeout;
    protected Timedelta rejectMessagesBefore;

    protected Object getParentMbox() {
        return this.parentMBox;
    }

    protected final Logger log = ZetaLogger.getLog(this.getClass().getSimpleName());

    public AbstractMailboxChangesListener(Object parentMBox, Map<String, String> expectedHeaders, Timedelta timeout,
                                          Timedelta rejectMessagesBefore) {
        // clone map
        this.expectedHeaders.putAll(expectedHeaders);
        this.parentMBox = parentMBox;
        this.timeout = timeout;
        this.rejectMessagesBefore = rejectMessagesBefore;
    }

}
