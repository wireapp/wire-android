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

package com.wire.android.ui.home.settings.multifactorauthentication

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RemoveAuthenticatorAppError { INVALID_CODE, GENERIC, LAST_APP, UNAVAILABLE }

data class MultiFactorAuthenticationRemoveAuthenticatorAppState(
    val isBusy: Boolean = false,
    val isValidated: Boolean = false,
    val error: RemoveAuthenticatorAppError? = null,
)

class MultiFactorAuthenticationRemoveAuthenticatorAppViewModel @Inject constructor(
    private val removeApp: RemoveAuthenticatorAppUseCase,
) : ViewModel() {
    val password = TextFieldState()
    val code = TextFieldState()
    private val _state = MutableStateFlow(MultiFactorAuthenticationRemoveAuthenticatorAppState())
    val state = _state.asStateFlow()
    private var operationJob: Job? = null

    @Suppress("TooGenericExceptionCaught")
    fun confirmRemoval(appId: String) {
        val current = _state.value
        if (current.isBusy || current.isValidated) return
        val enteredPassword = password.text.toString()
        val enteredCode = code.text.toString()
        if (!canSubmitMultiFactorAuthenticationCredentials(enteredPassword, enteredCode)) return
        code.clearText()
        _state.value = MultiFactorAuthenticationRemoveAuthenticatorAppState(isBusy = true)
        operationJob = viewModelScope.launch {
            try {
                if (removeApp(appId, enteredPassword, enteredCode)) {
                    password.clearText()
                    code.clearText()
                    _state.update { it.copy(isValidated = true) }
                } else {
                    _state.update { it.copy(error = RemoveAuthenticatorAppError.INVALID_CODE) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = RemoveAuthenticatorAppError.GENERIC) }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun removalRejected(outcome: AuthenticatorAppMutationResult) {
        _state.value = MultiFactorAuthenticationRemoveAuthenticatorAppState(
            error = if (outcome == AuthenticatorAppMutationResult.LAST_APP) {
                RemoveAuthenticatorAppError.LAST_APP
            } else {
                RemoveAuthenticatorAppError.UNAVAILABLE
            },
        )
    }

    fun cancel() {
        operationJob?.cancel()
        password.clearText()
        code.clearText()
        _state.value = MultiFactorAuthenticationRemoveAuthenticatorAppState()
    }

    override fun onCleared() {
        password.clearText()
        code.clearText()
        _state.value = MultiFactorAuthenticationRemoveAuthenticatorAppState()
    }
}
