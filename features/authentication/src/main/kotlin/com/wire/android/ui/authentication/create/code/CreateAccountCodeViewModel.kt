/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.code

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CreateAccountCodeViewModel<FlowT, LinksT, FailureT, UserT, CredentialsT>(
    private val input: CreateAccountCodeInput<FlowT, LinksT>,
    defaultServerConfig: LinksT,
    private val gateway: CreateAccountCodeGateway<LinksT, FailureT, UserT, CredentialsT>,
    private val resendCodeTimer: CreateAccountCodeResendTimer,
) : ViewModel() {
    val flowType: FlowT = input.flowType
    val customServerConfig: LinksT? = input.customServerConfig
    val serverConfig: LinksT = customServerConfig ?: defaultServerConfig
    val codeTextState: TextFieldState = TextFieldState()
    var codeState: CreateAccountCodeViewState<FlowT, UserT, FailureT> by
        mutableStateOf(CreateAccountCodeViewState(flowType))
        private set

    init {
        viewModelScope.launch {
            codeTextState.textAsFlow().collectLatest {
                if (it.length == codeState.codeLength) onCodeContinue()
            }
        }
    }

    fun resendCode() {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch {
            val result = gateway.requestActivationCode(serverConfig, input.email)
            if (result is ActivationCodeRequestResult.AuthScopeUnavailable) return@launch
            if (result is ActivationCodeRequestResult.Sent) startResendCodeTimer()
            codeState = codeState.copy(loading = false, result = result.toCodeResult())
        }
    }

    fun clearCodeError() {
        codeState = codeState.copy(result = CreateAccountCodeResult.None)
    }

    fun clearCodeField() {
        codeTextState.clearText()
    }

    private fun registrationRequest(): CreateAccountRegistrationRequest = if (input.isTeam) {
        CreateAccountRegistrationRequest.Team(
            input.firstName,
            input.lastName,
            input.password,
            input.email,
            { codeTextState.text.toString() },
            input.teamName,
        )
    } else {
        CreateAccountRegistrationRequest.Personal(
            input.firstName,
            input.lastName,
            input.password,
            input.email,
            { codeTextState.text.toString() },
        )
    }

    private fun onCodeContinue() {
        codeState = codeState.copy(loading = true)
        viewModelScope.launch {
            val credentials = when (val result = gateway.register(serverConfig, registrationRequest())) {
                is AccountRegistrationResult.Success -> result.credentials
                AccountRegistrationResult.AuthScopeUnavailable -> return@launch
                else -> return@launch updateCodeErrorState(result.toCodeError())
            }
            val userId = when (val result = gateway.storeSession(credentials)) {
                is StoreAccountSessionResult.Success -> result.userId
                else -> return@launch updateCodeErrorState(result.toCodeError())
            }
            when (val result = gateway.registerClient(userId, input.password)) {
                CreateAccountClientResult.Success,
                CreateAccountClientResult.E2EICertificateRequired ->
                    codeState = codeState.copy(result = CreateAccountCodeResult.Success(userId))

                CreateAccountClientResult.TooManyDevices ->
                    updateCodeErrorState(CreateAccountCodeResult.Error.TooManyDevicesError(userId))

                is CreateAccountClientResult.Generic ->
                    updateCodeErrorState(CreateAccountCodeResult.Error.DialogError.GenericError(result.failure))
            }
        }
    }

    private fun updateCodeErrorState(error: CreateAccountCodeResult.Error<UserT, FailureT>) {
        codeState = codeState.copy(loading = false, result = error)
    }

    private fun ActivationCodeRequestResult<FailureT>.toCodeResult(): CreateAccountCodeResult<UserT, FailureT> = when (this) {
        ActivationCodeRequestResult.Sent -> CreateAccountCodeResult.None
        ActivationCodeRequestResult.AlreadyInUse -> CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError
        ActivationCodeRequestResult.Blacklisted -> CreateAccountCodeResult.Error.DialogError.BlackListedError
        ActivationCodeRequestResult.DomainBlocked -> CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError
        ActivationCodeRequestResult.InvalidEmail -> CreateAccountCodeResult.Error.DialogError.InvalidEmailError
        is ActivationCodeRequestResult.Generic -> CreateAccountCodeResult.Error.DialogError.GenericError(failure)
        ActivationCodeRequestResult.AuthScopeUnavailable -> error("Handled before mapping")
    }

    private fun AccountRegistrationResult<FailureT, CredentialsT>.toCodeError(): CreateAccountCodeResult.Error<UserT, FailureT> =
        when (this) {
            AccountRegistrationResult.InvalidActivationCode -> CreateAccountCodeResult.Error.TextFieldError.InvalidActivationCodeError
            AccountRegistrationResult.AccountAlreadyExists -> CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError
            AccountRegistrationResult.Blacklisted -> CreateAccountCodeResult.Error.DialogError.BlackListedError
            AccountRegistrationResult.DomainBlocked -> CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError
            AccountRegistrationResult.InvalidEmail -> CreateAccountCodeResult.Error.DialogError.InvalidEmailError
            AccountRegistrationResult.TeamMembersLimitReached -> CreateAccountCodeResult.Error.DialogError.TeamMembersLimitError
            AccountRegistrationResult.UserCreationRestricted -> CreateAccountCodeResult.Error.DialogError.CreationRestrictedError
            is AccountRegistrationResult.Generic -> CreateAccountCodeResult.Error.DialogError.GenericError(failure)
            is AccountRegistrationResult.Success,
            AccountRegistrationResult.AuthScopeUnavailable -> error("Handled before mapping")
        }

    private fun StoreAccountSessionResult<FailureT, UserT>.toCodeError(): CreateAccountCodeResult.Error<UserT, FailureT> =
        when (this) {
            StoreAccountSessionResult.UserAlreadyExists -> CreateAccountCodeResult.Error.DialogError.UserAlreadyExistsError
            is StoreAccountSessionResult.Generic -> CreateAccountCodeResult.Error.DialogError.GenericError(failure)
            is StoreAccountSessionResult.Success -> error("Handled before mapping")
        }

    private fun startResendCodeTimer() {
        viewModelScope.launch {
            resendCodeTimer.start(
                seconds = RESEND_TIMER_DELAY,
                onUpdate = { updateResendTimer(it) },
                onFinish = { updateResendTimer(null) },
            )
        }
    }

    private fun updateResendTimer(timerText: String?) {
        codeState = codeState.copy(remainingTimerText = timerText)
    }

    companion object {
        const val RESEND_TIMER_DELAY = 300L
    }
}
