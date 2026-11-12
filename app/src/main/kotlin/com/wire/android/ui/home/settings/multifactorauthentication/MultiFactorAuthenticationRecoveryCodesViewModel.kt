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

data class MultiFactorAuthenticationRecoveryCodesState(
    val passwordConfirmed: Boolean = false,
    val isConfirmingPassword: Boolean = false,
    val isGenerating: Boolean = false,
    val showGenerationConfirmation: Boolean = false,
    val codes: List<String> = emptyList(),
    val codesSaved: Boolean = false,
    val hasError: Boolean = false,
    val nextStep: MultiFactorAuthenticationRecoveryCodesStep? = null,
) {
    val isBusy: Boolean get() = isConfirmingPassword || isGenerating
    val canNavigateBack: Boolean get() = !isGenerating && (codes.isEmpty() || codesSaved)
}

class MultiFactorAuthenticationRecoveryCodesViewModel @Inject constructor(
    private val confirmPassword: ConfirmMultiFactorAuthenticationPasswordUseCase,
    private val generateCodes: GenerateMultiFactorAuthenticationRecoveryCodesUseCase,
) : ViewModel() {
    val password = TextFieldState()
    private val _state = MutableStateFlow(MultiFactorAuthenticationRecoveryCodesState())
    val state = _state.asStateFlow()
    private var operationJob: Job? = null

    fun confirmPassword() {
        if (_state.value.isBusy) return
        val enteredPassword = password.text.toString()
        if (enteredPassword.isBlank()) return
        password.clearText()
        _state.update { it.copy(isConfirmingPassword = true, hasError = false) }
        perform {
            confirmPassword(enteredPassword)
            _state.update { it.copy(passwordConfirmed = true, nextStep = MultiFactorAuthenticationRecoveryCodesStep.SUMMARY) }
        }
    }

    fun requestGeneration() {
        val current = _state.value
        if (!current.passwordConfirmed || current.isBusy || current.codes.isNotEmpty()) return
        _state.update { it.copy(showGenerationConfirmation = true, hasError = false) }
    }

    fun dismissGenerationConfirmation() {
        if (!_state.value.isGenerating) _state.update { it.copy(showGenerationConfirmation = false) }
    }

    fun generateNewCodes() {
        val current = _state.value
        if (!current.passwordConfirmed || !current.showGenerationConfirmation) return
        if (current.isBusy || current.codes.isNotEmpty()) return
        _state.update { it.copy(isGenerating = true, hasError = false) }
        perform {
            val codes = generateCodes()
            check(codes.isNotEmpty())
            _state.update {
                it.copy(
                    codes = codes,
                    codesSaved = false,
                    showGenerationConfirmation = false,
                    nextStep = MultiFactorAuthenticationRecoveryCodesStep.NEW_CODES,
                )
            }
        }
    }

    fun setCodesSaved(saved: Boolean) {
        _state.update { it.copy(codesSaved = saved) }
    }

    fun navigationHandled() {
        _state.update { it.copy(nextStep = null) }
    }

    fun cancelPendingOperation() {
        if (!_state.value.canNavigateBack) return
        operationJob?.cancel()
        password.clearText()
        _state.update { it.copy(nextStep = null, hasError = false) }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun perform(operation: suspend () -> Unit) {
        operationJob = viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(hasError = true, showGenerationConfirmation = false) }
            } finally {
                _state.update { it.copy(isConfirmingPassword = false, isGenerating = false) }
            }
        }
    }

    override fun onCleared() {
        password.clearText()
        _state.value = MultiFactorAuthenticationRecoveryCodesState()
    }
}
