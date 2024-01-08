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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import com.wire.kalium.logic.feature.server.FetchApiVersionResult
import com.wire.kalium.logic.feature.server.FetchApiVersionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Cover this viewModel  with unit test
@HiltViewModel
class CreateAccountEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val fetchApiVersion: FetchApiVersionUseCase,
    private val validateEmail: ValidateEmailUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) : ViewModel() {

    val createAccountNavArgs: CreateAccountNavArgs = savedStateHandle.navArgs()

    var emailState: CreateAccountEmailViewState by mutableStateOf(CreateAccountEmailViewState(createAccountNavArgs.flowType))
        private set

    val serverConfig: ServerConfig.Links = authServerConfigProvider.authServer.value

    fun tosUrl(): String = authServerConfigProvider.authServer.value.tos

    fun onEmailChange(newText: TextFieldValue) {
        emailState = emailState.copy(
            email = newText,
            error = CreateAccountEmailViewState.EmailError.None,
            continueEnabled = newText.text.isNotEmpty() && !emailState.loading
        )
    }

    fun onEmailContinue(onSuccess: () -> Unit) {
        emailState = emailState.copy(loading = true, continueEnabled = false)
        viewModelScope.launch {
            fetchApiVersion(authServerConfigProvider.authServer.value).let {
                when (it) {
                    is FetchApiVersionResult.Success -> {}
                    is FetchApiVersionResult.Failure.UnknownServerVersion -> {
                        emailState = emailState.copy(showServerVersionNotSupportedDialog = true)
                        return@launch
                    }

                    is FetchApiVersionResult.Failure.TooNewVersion -> {
                        emailState = emailState.copy(showClientUpdateDialog = true)
                        return@launch
                    }

                    is FetchApiVersionResult.Failure.Generic -> {
                        return@launch
                    }
                }
            }

            val emailError =
                if (validateEmail(emailState.email.text.trim().lowercase())) CreateAccountEmailViewState.EmailError.None
                else CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError
            emailState = emailState.copy(
                loading = false,
                continueEnabled = true,
                termsDialogVisible = !emailState.termsAccepted && emailError is CreateAccountEmailViewState.EmailError.None,
                error = emailError
            )
            if (emailState.termsAccepted) onTermsAccept(onSuccess)
        }.invokeOnCompletion {
            emailState = emailState.copy(loading = false)
        }
    }

    fun onTermsAccept(onSuccess: () -> Unit) {
        emailState = emailState.copy(loading = true, continueEnabled = false, termsDialogVisible = false, termsAccepted = true)
        viewModelScope.launch {
            val authScope = coreLogic.versionedAuthenticationScope(serverConfig)().let {
                when (it) {
                    is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                    is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                        // TODO: show dialog
                        return@launch
                    }

                    is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                        // TODO: show dialog
                        return@launch
                    }

                    is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                        return@launch
                    }
                }
            }

            val emailError = authScope.registerScope.requestActivationCode(emailState.email.text.trim().lowercase()).toEmailError()
            emailState = emailState.copy(loading = false, continueEnabled = true, error = emailError)
            if (emailError is CreateAccountEmailViewState.EmailError.None) onSuccess()
        }
    }

    fun onEmailErrorDismiss() {
        emailState = emailState.copy(error = CreateAccountEmailViewState.EmailError.None)
    }

    fun onTermsDialogDismiss() {
        emailState = emailState.copy(termsDialogVisible = false)
    }
}

private fun RequestActivationCodeResult.toEmailError() = when (this) {
    RequestActivationCodeResult.Failure.AlreadyInUse -> CreateAccountEmailViewState.EmailError.TextFieldError.AlreadyInUseError
    RequestActivationCodeResult.Failure.BlacklistedEmail -> CreateAccountEmailViewState.EmailError.TextFieldError.BlacklistedEmailError
    RequestActivationCodeResult.Failure.DomainBlocked -> CreateAccountEmailViewState.EmailError.TextFieldError.DomainBlockedError
    RequestActivationCodeResult.Failure.InvalidEmail -> CreateAccountEmailViewState.EmailError.TextFieldError.InvalidEmailError
    is RequestActivationCodeResult.Failure.Generic -> CreateAccountEmailViewState.EmailError.DialogError.GenericError(this.failure)
    RequestActivationCodeResult.Success -> CreateAccountEmailViewState.EmailError.None
}
