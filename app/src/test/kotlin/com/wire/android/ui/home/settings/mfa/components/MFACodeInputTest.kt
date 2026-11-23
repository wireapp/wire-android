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

package com.wire.android.ui.home.settings.mfa.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.wire.android.ui.common.mfa.observeMFACodeCompletion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MFACodeInputTest {
    @Test
    fun `typing submits only when six digits have been entered`() = runTest {
        val code = TextFieldState()
        val submitted = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeMFACodeCompletion(code, canSubmit = { true }) { submitted += code.text.toString() }
        }

        for (length in 1..5) {
            Snapshot.withMutableSnapshot { code.setTextAndPlaceCursorAtEnd("123456".take(length)) }
            runCurrent()
            assertEquals(emptyList<String>(), submitted)
        }

        Snapshot.withMutableSnapshot { code.setTextAndPlaceCursorAtEnd("123456") }
        runCurrent()
        assertEquals(listOf("123456"), submitted)
    }

    @Test
    fun `pasting a complete code submits once even when submission readiness changes`() = runTest {
        val code = TextFieldState()
        val enabled = mutableStateOf(true)
        val submitted = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeMFACodeCompletion(code, canSubmit = { enabled.value }) { submitted += code.text.toString() }
        }

        Snapshot.withMutableSnapshot { code.setTextAndPlaceCursorAtEnd("123456") }
        runCurrent()
        Snapshot.withMutableSnapshot { enabled.value = false }
        runCurrent()
        Snapshot.withMutableSnapshot { enabled.value = true }
        runCurrent()

        assertEquals(listOf("123456"), submitted)
    }

    @Test
    fun `non-digit and overlong values are not submitted`() = runTest {
        val code = TextFieldState()
        val submitted = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeMFACodeCompletion(code, canSubmit = { true }) { submitted += code.text.toString() }
        }

        for (value in listOf("12345x", "1234567", "１２３４５６")) {
            Snapshot.withMutableSnapshot { code.setTextAndPlaceCursorAtEnd(value) }
            runCurrent()
        }

        assertEquals(emptyList<String>(), submitted)
    }

    @Test
    fun `code entered first waits for a password and an available action`() = runTest {
        val code = TextFieldState()
        val password = TextFieldState()
        val actionAvailable = mutableStateOf(false)
        val submitted = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeMFACodeCompletion(code, canSubmit = { actionAvailable.value && password.text.isNotBlank() }) {
                submitted += code.text.toString()
            }
        }

        Snapshot.withMutableSnapshot { code.setTextAndPlaceCursorAtEnd("123456") }
        runCurrent()
        Snapshot.withMutableSnapshot { actionAvailable.value = true }
        runCurrent()
        assertEquals(emptyList<String>(), submitted)
        Snapshot.withMutableSnapshot {
            actionAvailable.value = false
            password.setTextAndPlaceCursorAtEnd("password")
        }
        runCurrent()
        assertEquals(emptyList<String>(), submitted)
        Snapshot.withMutableSnapshot { actionAvailable.value = true }
        runCurrent()

        assertEquals(listOf("123456"), submitted)
    }

    @Test
    fun `busy state blocks submission and clearing allows the same code to be retried`() = runTest {
        val code = TextFieldState()
        val busy = mutableStateOf(true)
        val submitted = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeMFACodeCompletion(code, canSubmit = { !busy.value }) {
                submitted += code.text.toString()
                Snapshot.withMutableSnapshot {
                    code.clearText()
                    busy.value = true
                }
            }
        }

        Snapshot.withMutableSnapshot { code.setTextAndPlaceCursorAtEnd("123456") }
        runCurrent()
        assertEquals(emptyList<String>(), submitted)
        Snapshot.withMutableSnapshot { busy.value = false }
        runCurrent()
        assertEquals(listOf("123456"), submitted)

        Snapshot.withMutableSnapshot { busy.value = false }
        runCurrent()
        assertEquals(listOf("123456"), submitted)
        Snapshot.withMutableSnapshot { code.setTextAndPlaceCursorAtEnd("123456") }
        runCurrent()
        assertEquals(listOf("123456", "123456"), submitted)
    }

    @Test
    fun `leaving the code input cancels automatic submission`() = runTest {
        val code = TextFieldState()
        val submitted = mutableListOf<String>()
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeMFACodeCompletion(code, canSubmit = { true }) { submitted += code.text.toString() }
        }

        observer.cancel()
        observer.join()
        Snapshot.withMutableSnapshot { code.setTextAndPlaceCursorAtEnd("123456") }
        runCurrent()

        assertEquals(emptyList<String>(), submitted)
    }
}
