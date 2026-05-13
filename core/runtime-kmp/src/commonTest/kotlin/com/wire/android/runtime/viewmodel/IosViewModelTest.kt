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
package com.wire.android.runtime.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosViewModelTest {

    @Test
    fun givenIntentIsSent_whenSendIntentIsCalled_thenDelegateReceivesIntent() {
        var receivedIntent: TestIntent? = null
        val viewModel = IosViewModel<TestState, TestEffect, TestIntent>(
            state = MutableStateFlow(TestState),
            effects = emptyFlow(),
            onIntent = { receivedIntent = it },
        )

        viewModel.sendIntent(TestIntent)

        assertEquals(TestIntent, receivedIntent)
    }

    @Test
    fun givenCloseCallback_whenCloseIsCalled_thenDelegateIsCalled() {
        var closed = false
        val viewModel = IosViewModel<TestState, TestEffect, TestIntent>(
            state = MutableStateFlow(TestState),
            effects = emptyFlow(),
            onIntent = {},
            onClose = { closed = true },
        )

        viewModel.close()

        assertTrue(closed)
    }

    private data object TestState
    private data object TestEffect
    private data object TestIntent
}
