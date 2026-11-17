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

package com.wire.android.ui.home.settings.mfa.setup

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

enum class MFASetupError { PASSWORD_REQUIRED, INVALID_CODE, GENERIC }

data class MFASetupState(
    val enrollment: MFAEnrollment? = null,
    val recoveryCodes: List<String> = emptyList(),
    val recoveryCodesSaved: Boolean = false,
    val isBusy: Boolean = false,
    val error: MFASetupError? = null,
    val nextStep: MFASetupStep? = null,
    val completion: MFASetupResult? = null,
) {
    fun canNavigateBackFrom(step: MFASetupStep): Boolean =
        step != MFASetupStep.RECOVERY_CODES
}

class MFASetupViewModel @Inject constructor(
    private val startSetup: StartMFASetupUseCase,
    private val verifyCode: VerifyMFASetupCodeUseCase,
) : ViewModel() {
    // Plain in-memory field state: these values must not be restored from saved state.
    val password = TextFieldState()
    val code = TextFieldState()
    private val _state = MutableStateFlow(MFASetupState())
    val state = _state.asStateFlow()
    private var operationJob: Job? = null

    fun confirmPassword() {
        if (_state.value.isBusy) return
        val enteredPassword = password.text.toString()
        if (enteredPassword.isBlank()) {
            _state.update { it.copy(error = MFASetupError.PASSWORD_REQUIRED) }
            return
        }
        password.clearText()
        perform {
            // Going back through confirmation must not create a second enrollment.
            val enrollment = _state.value.enrollment ?: startSetup(enteredPassword)
            _state.update { it.copy(enrollment = enrollment, nextStep = MFASetupStep.LINK_AUTHENTICATOR) }
        }
    }

    fun verifyAuthenticationCode() {
        if (_state.value.isBusy || _state.value.enrollment == null) return
        val enteredCode = code.text.toString()
        if (enteredCode.length != AUTHENTICATION_CODE_LENGTH) {
            _state.update { it.copy(error = MFASetupError.INVALID_CODE) }
            return
        }
        code.clearText()
        perform {
            val codes = verifyCode(enteredCode)
            if (codes == null) {
                _state.update { it.copy(error = MFASetupError.INVALID_CODE) }
            } else {
                _state.update {
                    it.copy(
                        recoveryCodes = it.recoveryCodes.ifEmpty { codes },
                        nextStep = MFASetupStep.RECOVERY_CODES,
                    )
                }
            }
        }
    }

    fun setRecoveryCodesSaved(saved: Boolean) {
        _state.update { it.copy(recoveryCodesSaved = saved) }
    }

    fun finishSetup(authenticatorName: String) {
        val current = _state.value
        val enrollment = current.enrollment ?: return
        if (!current.recoveryCodesSaved || current.recoveryCodes.isEmpty() || current.isBusy) return
        _state.update {
            it.copy(completion = MFASetupResult(enrollment.id, authenticatorName, current.recoveryCodes.size))
        }
    }

    fun navigationHandled() {
        _state.update { it.copy(nextStep = null, error = null) }
    }

    fun cancelPendingOperation() {
        operationJob?.cancel()
        password.clearText()
        code.clearText()
        _state.update { it.copy(nextStep = null, error = null) }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun perform(operation: suspend () -> Unit) {
        _state.update { it.copy(isBusy = true, error = null) }
        operationJob = viewModelScope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = MFASetupError.GENERIC) }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    override fun onCleared() {
        password.clearText()
        code.clearText()
        _state.value = MFASetupState()
    }
}
