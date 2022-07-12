package com.wire.android.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.platformClassName

/**
 * this workaround solves kotlinx.coroutines.test.UncompletedCoroutinesError in tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runTestWithCancellation(body: suspend TestScope.() -> Unit): TestResult =
    try {
        runTest {
            body()
            cancel()
        }
    } catch (e: Exception) {
        // we can't just catch only JobCancellationException as it's internal
        if (e.platformClassName() == "kotlinx.coroutines.JobCancellationException") {
            // ignore
        } else {
            throw e
        }
    }
