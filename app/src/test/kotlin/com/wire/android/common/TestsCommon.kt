package com.wire.android.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.platformClassName
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * this workaround solves kotlinx.coroutines.test.UncompletedCoroutinesError in tests
 *
 * FIXME: This shouldn't exist.
 *        We should handle and test the fact that coroutines are cancelled properly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runTestWithCancellation(
    context: CoroutineContext = EmptyCoroutineContext,
    body: suspend TestScope.() -> Unit
): TestResult =
    try {
        runTest(context) {
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
