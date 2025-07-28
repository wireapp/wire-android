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
import com.wire.android.BuildConfig
import com.wire.android.datastore.UserDataStore
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel.Companion.RESEND_TIMER_DELAY
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class RegisterDeviceViewModel @Inject constructor(
    private val registerClientUseCase: GetOrRegisterClientUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val userDataStore: UserDataStore,
    private val getSelfUser: GetSelfUserUseCase,
    private val requestSecondFactorVerificationCodeUseCase: RequestSecondFactorVerificationCodeUseCase,
    private val resendCodeTimer: CountdownTimer,
) : ViewModel() {

    val passwordTextState: TextFieldState = TextFieldState()
    var state: RegisterDeviceState by mutableStateOf(RegisterDeviceState())
        private set

    val secondFactorVerificationCodeTextState: TextFieldState = TextFieldState()
    var secondFactorVerificationCodeState: VerificationCodeState by mutableStateOf(VerificationCodeState())
        private set

    init {
        runBlocking {
            state = state.copy(flowState = RegisterDeviceFlowState.Loading)
            isPasswordRequired().let {
                state = state.copy(flowState = RegisterDeviceFlowState.Default)
                when (it) {
                    is IsPasswordRequiredUseCase.Result.Failure -> {
                        updateFlowState(RegisterDeviceFlowState.Error.GenericError(it.cause))
                    }

                    is IsPasswordRequiredUseCase.Result.Success -> {
                        if (!it.value) registerClient(null)
                    }
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
        registerClientUseCase(
            RegisterClientUseCase.RegisterClientParam(
                password = password,
                secondFactorVerificationCode = secondFactorVerificationCode,
                capabilities = null,
                modelPostfix = if (BuildConfig.PRIVATE_BUILD) " [${BuildConfig.FLAVOR}_${BuildConfig.BUILD_TYPE}]" else null
            )
        ).handle(secondFactorVerificationCode.isNullOrEmpty())
    }

    private suspend fun RegisterClientResult.handle(empty2FACodeInput: Boolean) {
        when (this) {
            is RegisterClientResult.Failure.TooManyClients -> updateFlowState(RegisterDeviceFlowState.TooManyDevices)

            is RegisterClientResult.Success -> updateFlowState(
                RegisterDeviceFlowState.Success(
                    initialSyncCompleted = userDataStore.initialSyncCompleted.first(),
                    isE2EIRequired = false,
                    clientId = this.client.id
                )
            )

            is RegisterClientResult.E2EICertificateRequired -> updateFlowState(
                    RegisterDeviceFlowState.Success(
                        initialSyncCompleted = userDataStore.initialSyncCompleted.first(),
                        isE2EIRequired = true,
                        clientId = this.client.id,
                        userId = this.userId
                    )
                )

            is RegisterClientResult.Failure.InvalidCredentials.Missing2FA -> request2FACode()

            is RegisterClientResult.Failure.InvalidCredentials.Invalid2FA -> {
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

            is RegisterClientResult.Failure.Generic -> state = state.copy(
                continueEnabled = true,
                flowState = RegisterDeviceFlowState.Error.GenericError(this.genericFailure)
            )

            is RegisterClientResult.Failure.InvalidCredentials -> state = state.copy(
                continueEnabled = true,
                flowState = RegisterDeviceFlowState.Error.InvalidCredentialsError
            )

            is RegisterClientResult.Failure.PasswordAuthRequired -> {
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
        getSelfUser()?.email?.let { email ->
            requestSecondFactorVerificationCodeUseCase(
                email = email,
                verifiableAction = VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION
            ).let { result ->
                when (result) {
                    is RequestSecondFactorVerificationCodeUseCase.Result.Success,
                    is RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests -> {
                        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
                            isCodeInputNecessary = true,
                            emailUsed = email,
                        )
                        updateFlowState(RegisterDeviceFlowState.Default)
                        startResendCodeTimer()
                    }

                    is RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic -> {
                        updateFlowState(RegisterDeviceFlowState.Error.GenericError(result.cause))
                    }
                }
            }
        }
    }

    private fun updateFlowState(flowState: RegisterDeviceFlowState) {
        state = state.copy(flowState = flowState)
    }

    private fun startResendCodeTimer() {
        viewModelScope.launch {
            resendCodeTimer.start(
                seconds = RESEND_TIMER_DELAY,
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
            remainingTimerText = timerText?.let { timerText }
        )
    }
}
