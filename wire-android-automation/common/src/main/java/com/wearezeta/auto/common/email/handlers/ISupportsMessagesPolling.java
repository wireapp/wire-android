package com.wearezeta.auto.common.email.handlers;

import com.wearezeta.auto.common.misc.Timedelta;

import java.util.Map;
import java.util.concurrent.Future;

public interface ISupportsMessagesPolling {
    boolean waitUntilMessagesCountReaches(String deliveredTo, int expectedMsgsCount, Timedelta timeout) throws Exception;

    Future<String> getMessage(Map<String, String> expectedHeaders, Timedelta timeout) throws Exception;

    Future<String> getMessage(Map<String, String> expectedHeaders, Timedelta timeout,
                              Timedelta rejectMessagesBefore) throws Exception;

    boolean isAlive();
}
