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

package com.wire.android.ui.home.settings.mfa

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

enum class MFATurnOffError { INVALID_CODE, GENERIC }

data class MFATurnOffState(
    val isBusy: Boolean = false,
    val isCompleted: Boolean = false,
    val error: MFATurnOffError? = null,
)

class MFATurnOffViewModel @Inject constructor(
    private val turnOffMFA: TurnOffMFAUseCase,
) : ViewModel() {
    val password = TextFieldState()
    val code = TextFieldState()
    private val _state = MutableStateFlow(MFATurnOffState())
    val state = _state.asStateFlow()
    private var operationJob: Job? = null

    @Suppress("TooGenericExceptionCaught")
    fun turnOff() {
        val current = _state.value
        if (current.isBusy || current.isCompleted) return
        val enteredPassword = password.text.toString()
        val enteredCode = code.text.toString()
        if (!canSubmitMFACredentials(enteredPassword, enteredCode)) return
        code.clearText()
        _state.value = MFATurnOffState(isBusy = true)
        operationJob = viewModelScope.launch {
            try {
                if (turnOffMFA(enteredPassword, enteredCode)) {
                    password.clearText()
                    code.clearText()
                    _state.value = MFATurnOffState(isCompleted = true)
                } else {
                    _state.update { it.copy(error = MFATurnOffError.INVALID_CODE) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = MFATurnOffError.GENERIC) }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    fun cancel() {
        operationJob?.cancel()
        password.clearText()
        code.clearText()
        _state.value = MFATurnOffState()
    }

    override fun onCleared() {
        password.clearText()
        code.clearText()
        _state.value = MFATurnOffState()
    }
}

internal fun canSubmitMFACredentials(password: CharSequence, code: CharSequence): Boolean =
    password.isNotBlank() && code.length == AUTHENTICATION_CODE_LENGTH && code.all { it in '0'..'9' }
