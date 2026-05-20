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
package com.wire.ios.shared

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IosViewModelTest {
    @Test
    fun givenState_whenCurrentStateIsRead_thenReturnsStateValue() {
        val viewModel = IosViewModel<TestState, NoEffect, TestIntent>(
            state = MutableStateFlow(TestState),
            effects = MutableSharedFlow(),
            onIntent = {},
        )

        assertEquals(TestState, viewModel.currentState)
    }

    @Test
    fun givenIntent_whenSendIntent_thenDelegatesToHandler() {
        var handledIntent: TestIntent? = null
        val viewModel = IosViewModel<TestState, NoEffect, TestIntent>(
            state = MutableStateFlow(TestState),
            effects = MutableSharedFlow(),
            onIntent = { handledIntent = it },
        )

        viewModel.sendIntent(TestIntent)

        assertEquals(TestIntent, handledIntent)
    }

    @Test
    fun givenStateObserver_whenObserving_thenObserverReceivesCurrentState() = runTest {
        val state = MutableStateFlow(TestState)
        val viewModel = IosViewModel<TestState, NoEffect, TestIntent>(
            state = state,
            effects = MutableSharedFlow(),
            onIntent = {},
        )
        val initialState = async(start = CoroutineStart.UNDISPATCHED) {
            var observedState: TestState? = null
            val closeable = viewModel.observeState { observedState = it }
            closeable.close()
            observedState
        }

        assertEquals(TestState, initialState.await())
    }

    @Test
    fun givenEffectObserver_whenEffectEmits_thenObserverReceivesEffect() = runTest {
        val effects = MutableSharedFlow<NoEffect>(extraBufferCapacity = 1)
        val viewModel = IosViewModel<TestState, NoEffect, TestIntent>(
            state = MutableStateFlow(TestState),
            effects = effects,
            onIntent = {},
        )
        var observedEffect: NoEffect? = null
        val closeable = viewModel.observeEffect { observedEffect = it }

        effects.emit(NoEffect)
        closeable.close()

        assertEquals(NoEffect, observedEffect)
    }

    @Test
    fun givenCloseHandler_whenClose_thenDelegatesToHandler() {
        var closeCount = 0
        val viewModel = IosViewModel<TestState, NoEffect, TestIntent>(
            state = MutableStateFlow(TestState),
            effects = MutableSharedFlow<NoEffect>(),
            onIntent = {},
            onClose = { closeCount++ },
        )

        viewModel.close()
        viewModel.close()

        assertEquals(1, closeCount)
    }

    private data object TestState
    private data object TestIntent
}
