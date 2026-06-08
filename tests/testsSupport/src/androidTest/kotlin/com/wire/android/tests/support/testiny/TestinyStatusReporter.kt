/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.tests.support.testiny

import android.app.Instrumentation
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry

/** Emits Testiny diagnostics into the instrumentation stream shown in CI logs. */
object TestinyStatusReporter {
    fun info(message: String) {
        emit("TESTINY: $message")
    }

    fun warning(message: String) {
        emit("TESTINY WARNING: $message")
    }

    fun error(message: String, error: Throwable? = null) {
        emit("TESTINY ERROR: $message")
        error?.printStackTrace()
    }

    private fun emit(message: String) {
        println(message)

        runCatching {
            InstrumentationRegistry.getInstrumentation().sendStatus(
                0,
                Bundle().apply {
                    putString(Instrumentation.REPORT_KEY_STREAMRESULT, "$message\n")
                }
            )
        }
    }
}
