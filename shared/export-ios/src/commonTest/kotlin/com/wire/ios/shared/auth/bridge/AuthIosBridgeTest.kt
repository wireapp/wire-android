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
package com.wire.ios.shared.auth.bridge

import com.wire.ios.shared.NoEffect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class AuthIosBridgeTest {

    @Test
    fun givenStateAndEffects_whenAuthIosViewModelIsCreated_thenExposesSameFlows() {
        val state = MutableStateFlow(TestState)
        val effects = MutableSharedFlow<NoEffect>()

        val viewModel = authIosViewModel<TestState, NoEffect, TestIntent>(
            state = state,
            effects = effects,
            onIntent = {},
        )

        assertSame(state, viewModel.state)
        assertSame(effects, viewModel.effects)
    }

    @Test
    fun givenIntentAndCloseHandlers_whenBridgeIsUsed_thenDelegatesCalls() {
        var handledIntent: TestIntent? = null
        var closed = false
        val viewModel = authIosViewModel<TestState, NoEffect, TestIntent>(
            state = MutableStateFlow(TestState),
            effects = MutableSharedFlow<NoEffect>(),
            onIntent = { handledIntent = it },
            onClose = { closed = true },
        )

        viewModel.sendIntent(TestIntent.Submit)
        viewModel.close()

        assertEquals(TestIntent.Submit, handledIntent)
        assertTrue(closed)
    }

    @Test
    fun givenAuthBridgeViewModel_whenConverted_thenDelegatesIntentAndClose() {
        val bridgeViewModel = TestBridgeViewModel()
        val viewModel = bridgeViewModel.asIosViewModel()

        viewModel.sendIntent(TestIntent.Submit)
        viewModel.close()

        assertEquals(TestIntent.Submit, bridgeViewModel.handledIntent)
        assertTrue(bridgeViewModel.closed)
    }

    private data object TestState

    private sealed interface TestIntent {
        data object Submit : TestIntent
    }

    private class TestBridgeViewModel : AuthBridgeViewModel<TestState, NoEffect, TestIntent> {
        override val state = MutableStateFlow(TestState)
        override val effects = MutableSharedFlow<NoEffect>()
        var handledIntent: TestIntent? = null
        var closed = false

        override fun sendIntent(intent: TestIntent) {
            handledIntent = intent
        }

        override fun close() {
            closed = true
        }
    }
}
