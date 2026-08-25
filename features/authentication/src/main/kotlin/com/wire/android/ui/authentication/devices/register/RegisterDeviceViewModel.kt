/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.authentication.devices.register

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RegisterDeviceViewModel<SessionT>(
    private val gateway: RegisterDeviceGateway<SessionT>,
    private val resendCodeTimer: RegisterDeviceResendTimer,
) : ViewModel() {

    val passwordTextState: TextFieldState = TextFieldState()
    var state: RegisterDeviceState<SessionT> by mutableStateOf(RegisterDeviceState())
        private set

    val secondFactorVerificationCodeTextState: TextFieldState = TextFieldState()
    var secondFactorVerificationCodeState: VerificationCodeState by mutableStateOf(VerificationCodeState())
        private set

    init {
        runBlocking {
            state = state.copy(flowState = RegisterDeviceFlowState.Loading)
            gateway.passwordRequirement().let {
                state = state.copy(flowState = RegisterDeviceFlowState.Default)
                when (it) {
                    is PasswordRequirement.Failure ->
                        updateFlowState(RegisterDeviceFlowState.Error.GenericError(it.failure))

                    PasswordRequirement.NotRequired -> registerClient(null)
                    PasswordRequirement.Required -> Unit
                }
            }
        }
        viewModelScope.launch {
            passwordTextState.textAsFlow().distinctUntilChanged().collectLatest {
                state = state.copy(flowState = RegisterDeviceFlowState.Default, continueEnabled = it.isNotEmpty())
            }
        }
        viewModelScope.launch {
            secondFactorVerificationCodeTextState.textAsFlow().collectLatest {
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCurrentCodeInvalid = false)
                if (it.length == VerificationCodeState.DEFAULT_VERIFICATION_CODE_LENGTH) {
                    registerClient(passwordTextState.text.toString(), it.toString())
                }
            }
        }
    }

    fun onErrorDismiss() {
        updateFlowState(RegisterDeviceFlowState.Default)
    }

    private suspend fun registerClient(password: String?, secondFactorVerificationCode: String? = null) {
        state = state.copy(flowState = RegisterDeviceFlowState.Loading, continueEnabled = false)
        gateway.registerClient(
            RegisterDeviceRequest(
                password = password,
                verificationCode = secondFactorVerificationCode,
            )
        ).handle(secondFactorVerificationCode.isNullOrEmpty())
    }

    private suspend fun RegisterDeviceResult<SessionT>.handle(empty2FACodeInput: Boolean) {
        when (this) {
            RegisterDeviceResult.TooManyDevices -> updateFlowState(RegisterDeviceFlowState.TooManyDevices)

            is RegisterDeviceResult.Success -> updateFlowState(
                RegisterDeviceFlowState.Success(
                    initialSyncCompleted = initialSyncCompleted,
                    isE2EIRequired = isE2EIRequired,
                    e2eiSessionId = e2eiSessionId,
                )
            )

            RegisterDeviceResult.MissingSecondFactor -> request2FACode()

            RegisterDeviceResult.InvalidSecondFactor -> {
                state = state.copy(
                    continueEnabled = true,
                    flowState = RegisterDeviceFlowState.Default
                )
                if (empty2FACodeInput) {
                    // code not yet entered so invalid code was the one reused from last login so just request a new one
                    request2FACode()
                } else {
                    // invalid code was the one already entered so show invalid code error
                    secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
                        isCodeInputNecessary = true,
                        isCurrentCodeInvalid = true,
                    )
                }
            }

            is RegisterDeviceResult.Failure -> state = state.copy(
                continueEnabled = true,
                flowState = RegisterDeviceFlowState.Error.GenericError(failure)
            )

            RegisterDeviceResult.InvalidCredentials -> state = state.copy(
                continueEnabled = true,
                flowState = RegisterDeviceFlowState.Error.InvalidCredentialsError
            )

            RegisterDeviceResult.PasswordRequired -> {
                /* app is already waiting for the user to enter the password */
            }
        }
    }

    fun onContinue() {
        viewModelScope.launch {
            registerClient(passwordTextState.text.toString())
        }
    }

    fun onCodeResend() {
        viewModelScope.launch {
            request2FACode()
        }
    }

    fun onCodeVerificationBackPress() {
        secondFactorVerificationCodeTextState.clearText()
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
            isCodeInputNecessary = false,
            emailUsed = "",
        )
    }

    private suspend fun request2FACode() {
        when (val result = gateway.requestVerificationCode()) {
            is RequestVerificationCodeResult.Sent -> showVerificationCodeInput(result.email)
            is RequestVerificationCodeResult.TooManyRequests -> showVerificationCodeInput(result.email)
            RequestVerificationCodeResult.MissingEmail -> Unit
            is RequestVerificationCodeResult.Failure -> {
                updateFlowState(RegisterDeviceFlowState.Error.GenericError(result.failure))
            }
        }
    }

    private fun showVerificationCodeInput(email: String) {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
            isCodeInputNecessary = true,
            emailUsed = email,
        )
        updateFlowState(RegisterDeviceFlowState.Default)
        startResendCodeTimer()
    }

    private fun updateFlowState(flowState: RegisterDeviceFlowState<SessionT>) {
        state = state.copy(flowState = flowState)
    }

    private fun startResendCodeTimer() {
        viewModelScope.launch {
            resendCodeTimer.start(
                seconds = RESEND_TIMER_DELAY_SECONDS,
                onUpdate = { timerText ->
                    updateResendTimer(timerText)
                },
                onFinish = {
                    updateResendTimer(null)
                }
            )
        }
    }

    private fun updateResendTimer(timerText: String?) {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
            remainingTimerText = timerText
        )
    }

    private companion object {
        const val RESEND_TIMER_DELAY_SECONDS = 300L
    }
}
