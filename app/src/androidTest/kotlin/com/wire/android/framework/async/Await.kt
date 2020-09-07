package com.wire.android.framework.async

import org.assertj.core.api.Assertions.fail
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @param timeout number of seconds to wait for the countdown
 */
fun CountDownLatch.awaitResult(timeout: Long) {
    if (!await(timeout, TimeUnit.SECONDS)) {
        fail<Unit>("Did not receive event within $timeout seconds")
    }
}
