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

package com.wire.android.ui.home.settings.mfa.authenticatorapps.add

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.settings.mfa.setup.MFASetupError
import com.wire.android.ui.home.settings.mfa.setup.MFASetupStep
import com.wire.kalium.common.functional.fold
import com.wire.kalium.logic.feature.mfa.FinishAddingAuthenticatorAppUseCase
import com.wire.kalium.logic.feature.mfa.MFAConstraints.AUTHENTICATION_CODE_LENGTH
import com.wire.kalium.logic.feature.mfa.MFAEnrollment
import com.wire.kalium.logic.feature.mfa.MFAFailure
import com.wire.kalium.logic.feature.mfa.StartMFASetupUseCase
import com.wire.kalium.logic.feature.mfa.VerifyAuthenticatorAppCodeUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MFAAddAuthenticatorAppState(
    val enrollment: MFAEnrollment? = null,
    val isVerified: Boolean = false,
    val isBusy: Boolean = false,
    val error: MFASetupError? = null,
    val nextStep: MFASetupStep? = null,
    val completion: AuthenticatorAppAddedResult? = null,
)

class MFAAddAuthenticatorAppViewModel @Inject constructor(
    private val startSetup: StartMFASetupUseCase,
    private val verifyCode: VerifyAuthenticatorAppCodeUseCase,
    private val finishAdding: FinishAddingAuthenticatorAppUseCase,
) : ViewModel() {
    val password = TextFieldState()
    val code = TextFieldState()
    val name = TextFieldState()
    private val _state = MutableStateFlow(MFAAddAuthenticatorAppState())
    val state = _state.asStateFlow()
    private var operationJob: Job? = null

    fun confirmPassword() {
        if (_state.value.isBusy) return
        val enteredPassword = password.text.toString()
        if (enteredPassword.isBlank()) return
        password.clearText()
        perform {
            val existing = _state.value.enrollment
            if (existing != null) {
                _state.update { it.copy(nextStep = MFASetupStep.LINK_AUTHENTICATOR) }
            } else {
                startSetup(enteredPassword).fold(
                    { _state.update { it.copy(error = MFASetupError.GENERIC) } },
                    { enrollment -> _state.update { it.copy(enrollment = enrollment, nextStep = MFASetupStep.LINK_AUTHENTICATOR) } },
                )
            }
        }
    }

    fun verifyAuthenticationCode() {
        val current = _state.value
        val enrollment = current.enrollment
        if (current.isBusy || enrollment == null) return
        val enteredCode = code.text.toString()
        if (enteredCode.length != AUTHENTICATION_CODE_LENGTH) return
        code.clearText()
        perform {
            verifyCode(enrollment.id, enteredCode).fold(
                { failure ->
                    _state.update {
                        it.copy(error = if (failure == MFAFailure.INVALID_CODE) MFASetupError.INVALID_CODE else MFASetupError.GENERIC)
                    }
                },
                { _state.update { it.copy(isVerified = true, nextStep = MFASetupStep.NAME_APP) } },
            )
        }
    }

    fun addApp() {
        val current = _state.value
        val enrollment = current.enrollment ?: return
        val appName = name.text.toString().trim()
        val canAdd = !current.isBusy && current.isVerified && current.completion == null
        if (!canAdd || appName.isEmpty()) return
        perform {
            finishAdding(enrollment.id, appName).fold(
                { _state.update { it.copy(error = MFASetupError.GENERIC) } },
                { app -> _state.update { it.copy(completion = AuthenticatorAppAddedResult(app.id, app.name)) } },
            )
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
        name.clearText()
        _state.value = MFAAddAuthenticatorAppState()
    }
}
