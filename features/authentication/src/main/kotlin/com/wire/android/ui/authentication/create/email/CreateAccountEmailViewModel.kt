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
package com.wire.android.ui.authentication.create.email

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CreateAccountEmailViewModel<FlowT, LinksT, FailureT>(
    val flowType: FlowT,
    val customServerConfig: LinksT?,
    defaultServerConfig: LinksT,
    private val tosUrlFor: (LinksT) -> String,
    private val gateway: CreateAccountEmailGateway<LinksT, FailureT>,
) : ViewModel() {
    val emailTextState: TextFieldState = TextFieldState()
    var emailState: CreateAccountEmailViewState<FlowT, FailureT> by mutableStateOf(CreateAccountEmailViewState(flowType))
        private set

    val serverConfig: LinksT = customServerConfig ?: defaultServerConfig

    fun tosUrl(): String = tosUrlFor(serverConfig)

    init {
        viewModelScope.launch {
            emailTextState.textAsFlow().collectLatest {
                emailState = emailState.copy(
                    error = CreateAccountEmailViewState.EmailError.None,
                    continueEnabled = it.isNotEmpty() && !emailState.loading
                )
            }
        }
    }

    fun onEmailContinue() {
        emailState = emailState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            val email = emailTextState.text.toString().trim().lowercase()
            val emailError = when (gateway.isEmailValid(email)) {
                true -> CreateAccountEmailViewState.EmailError.None
                false -> CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError
            }
            emailState = emailState.copy(
                loading = false,
                continueEnabled = true,
                termsDialogVisible = !emailState.termsAccepted && emailError is CreateAccountEmailViewState.EmailError.None,
                error = emailError
            )
            if (emailState.termsAccepted) onTermsAccept()
        }.invokeOnCompletion {
            emailState = emailState.copy(loading = false)
        }
    }

    fun onTermsAccept() {
        emailState = emailState.copy(loading = true, continueEnabled = false, termsDialogVisible = false, termsAccepted = true)
        viewModelScope.launch {
            val emailError = when (val result = gateway.requestActivationCode(serverConfig) {
                emailTextState.text.toString().trim().lowercase()
            }) {
                ActivationCodeResult.AuthScopeUnavailable -> return@launch
                ActivationCodeResult.Sent -> CreateAccountEmailViewState.EmailError.None
                ActivationCodeResult.AlreadyInUse -> CreateAccountEmailViewState.EmailError.TextFieldError.AlreadyInUseError
                ActivationCodeResult.Blacklisted -> CreateAccountEmailViewState.EmailError.TextFieldError.BlacklistedEmailError
                ActivationCodeResult.DomainBlocked -> CreateAccountEmailViewState.EmailError.TextFieldError.DomainBlockedError
                ActivationCodeResult.InvalidEmail -> CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError
                is ActivationCodeResult.Generic -> CreateAccountEmailViewState.EmailError.DialogError.GenericError(result.failure)
            }
            emailState = emailState.copy(loading = false, continueEnabled = true, error = emailError)
            if (emailError is CreateAccountEmailViewState.EmailError.None) emailState = emailState.copy(success = true)
        }
    }

    fun onEmailErrorDismiss() {
        emailState = emailState.copy(error = CreateAccountEmailViewState.EmailError.None)
    }

    fun onTermsDialogDismiss() {
        emailState = emailState.copy(termsDialogVisible = false)
    }
}
