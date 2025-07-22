/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
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
        if (e::class.qualifiedName == "kotlinx.coroutines.JobCancellationException") {
            // ignore
        } else {
            throw e
        }
    }
