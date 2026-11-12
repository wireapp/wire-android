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

enum class MultiFactorAuthenticationTurnOffError { INVALID_CODE, GENERIC }

data class MultiFactorAuthenticationTurnOffState(
    val isBusy: Boolean = false,
    val isCompleted: Boolean = false,
    val error: MultiFactorAuthenticationTurnOffError? = null,
)

class MultiFactorAuthenticationTurnOffViewModel @Inject constructor(
    private val turnOffMultiFactorAuthentication: TurnOffMultiFactorAuthenticationUseCase,
) : ViewModel() {
    val password = TextFieldState()
    val code = TextFieldState()
    private val _state = MutableStateFlow(MultiFactorAuthenticationTurnOffState())
    val state = _state.asStateFlow()
    private var operationJob: Job? = null

    @Suppress("TooGenericExceptionCaught")
    fun turnOff() {
        val current = _state.value
        if (current.isBusy || current.isCompleted) return
        val enteredPassword = password.text.toString()
        val enteredCode = code.text.toString()
        if (!canSubmitMultiFactorAuthenticationTurnOff(enteredPassword, enteredCode)) return
        code.clearText()
        _state.value = MultiFactorAuthenticationTurnOffState(isBusy = true)
        operationJob = viewModelScope.launch {
            try {
                if (turnOffMultiFactorAuthentication(enteredPassword, enteredCode)) {
                    password.clearText()
                    code.clearText()
                    _state.value = MultiFactorAuthenticationTurnOffState(isCompleted = true)
                } else {
                    _state.update { it.copy(error = MultiFactorAuthenticationTurnOffError.INVALID_CODE) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = MultiFactorAuthenticationTurnOffError.GENERIC) }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        password.clearText()
        code.clearText()
        _state.value = MultiFactorAuthenticationTurnOffState()
    }

    override fun onCleared() {
        password.clearText()
        code.clearText()
        _state.value = MultiFactorAuthenticationTurnOffState()
    }
}

internal fun canSubmitMultiFactorAuthenticationTurnOff(password: CharSequence, code: CharSequence): Boolean =
    password.isNotBlank() && code.length == AUTHENTICATION_CODE_LENGTH && code.all { it in '0'..'9' }
