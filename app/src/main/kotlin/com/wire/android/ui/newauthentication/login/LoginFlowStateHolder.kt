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

package com.wire.android.ui.newauthentication.login

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginFlowHolderState(
    val userIdentifier: String = "",
    val flowState: NewLoginFlowState = NewLoginFlowState.Default,
) {
    val nextEnabled: Boolean = flowState !is NewLoginFlowState.Loading && userIdentifier.isNotEmpty()
}

class LoginFlowStateHolder<Result : Any>(
    initialUserIdentifier: String = "",
    initialFlowState: NewLoginFlowState = NewLoginFlowState.Default,
) {
    private val _state = MutableStateFlow(
        LoginFlowHolderState(
            userIdentifier = initialUserIdentifier,
            flowState = initialFlowState,
        )
    )
    val state: StateFlow<LoginFlowHolderState> = _state.asStateFlow()

    private val _results = MutableSharedFlow<Result>(extraBufferCapacity = 1)
    val results: SharedFlow<Result> = _results.asSharedFlow()

    val userIdentifier: String
        get() = _state.value.userIdentifier

    val flowState: NewLoginFlowState
        get() = _state.value.flowState

    fun updateUserIdentifier(userIdentifier: String) {
        _state.update { currentState ->
            currentState.copy(
                userIdentifier = userIdentifier,
                flowState = currentState.flowState.resetTextFieldError(),
            )
        }
    }

    fun updateFlowState(flowState: NewLoginFlowState) {
        updateFlowState { flowState }
    }

    fun updateFlowState(update: (NewLoginFlowState) -> NewLoginFlowState) {
        _state.update { currentState ->
            currentState.copy(flowState = update(currentState.flowState))
        }
    }

    fun tryEmitResult(result: Result): Boolean = _results.tryEmit(result)

    suspend fun emitResult(result: Result) {
        _results.emit(result)
    }

    private fun NewLoginFlowState.resetTextFieldError(): NewLoginFlowState =
        if (this is NewLoginFlowState.Error.TextFieldError) NewLoginFlowState.Default else this
}
