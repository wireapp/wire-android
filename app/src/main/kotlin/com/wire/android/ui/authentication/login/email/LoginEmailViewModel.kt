/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.authentication.login.email

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.authentication.login.toLoginError
import com.wire.android.ui.authentication.login.updateEmailLoginEnabled
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.kalium.logic.data.auth.login.ProxyCredentials
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "ComplexMethod")
@HiltViewModel
class LoginEmailViewModel @Inject constructor(
    private val authScope: AutoVersionAuthScopeUseCase,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val savedStateHandle: SavedStateHandle,
    navigationManager: NavigationManager,
    authServerConfigProvider: AuthServerConfigProvider,
    userDataStoreProvider: UserDataStoreProvider
) : LoginViewModel(
    savedStateHandle,
    navigationManager,
    clientScopeProviderFactory,
    authServerConfigProvider,
    userDataStoreProvider
) {

    var secondFactorVerificationCodeState by mutableStateOf(
        VerificationCodeState()
    )

    @Suppress("LongMethod")
    fun login() {
        loginState = loginState.copy(emailLoginLoading = true, loginError = LoginError.None).updateEmailLoginEnabled()
        viewModelScope.launch {
            val authScope = resolveCurrentAuthScope() ?: return@launch

            val secondFactorVerificationCode = secondFactorVerificationCodeState.code.text.text
            val loginResult = authScope.login(
                userIdentifier = loginState.userIdentifier.text,
                password = loginState.password.text,
                shouldPersistClient = true,
                secondFactorVerificationCode = secondFactorVerificationCode
            )
            if (loginResult !is AuthenticationResult.Success) {
                loginResult as AuthenticationResult.Failure
                handleAuthenticationFailure(loginResult, authScope)
                return@launch
            }
            val storedUserId = addAuthenticatedUser(
                authTokens = loginResult.authData,
                ssoId = loginResult.ssoID,
                serverConfigId = loginResult.serverConfigId,
                proxyCredentials = loginResult.proxyCredentials,
                replace = false
            ).let {
                when (it) {
                    is AddAuthenticatedUserUseCase.Result.Failure -> {
                        updateEmailLoginError(it.toLoginError())
                        return@launch
                    }

                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            registerClient(
                userId = storedUserId,
                password = loginState.password.text,
            ).let {
                when (it) {
                    is RegisterClientResult.Failure -> {
                        updateEmailLoginError(it.toLoginError())
                        return@launch
                    }

                    is RegisterClientResult.Success -> {
                        navigateAfterRegisterClientSuccess(storedUserId)
                    }
                }
            }
        }
    }

    private suspend fun resolveCurrentAuthScope(): AuthenticationScope? = authScope(
        AutoVersionAuthScopeUseCase.ProxyAuthentication.UsernameAndPassword(
            ProxyCredentials(loginState.proxyIdentifier.text, loginState.proxyPassword.text)
        )
    ).let {
        loginState = loginState.copy(emailLoginLoading = false)
        when (it) {
            is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

            is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                loginState = loginState.copy(loginError = LoginError.DialogError.ServerVersionNotSupported)
                return null
            }

            is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                loginState = loginState.copy(loginError = LoginError.DialogError.ClientUpdateRequired)
                return null
            }

            is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                loginState = loginState.copy(loginError = LoginError.DialogError.GenericError(it.genericFailure))
                return null
            }
        }
    }

    private suspend fun handleAuthenticationFailure(it: AuthenticationResult.Failure, authScope: AuthenticationScope) {
        when (it) {
            is AuthenticationResult.Failure.InvalidCredentials.Missing2FA -> {
                loginState = loginState.updateEmailLoginEnabled()
                request2FACode(authScope)
            }

            is AuthenticationResult.Failure.InvalidCredentials.Invalid2FA -> {
                loginState = loginState.updateEmailLoginEnabled()
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCurrentCodeInvalid = true)
            }

            else -> updateEmailLoginError(it.toLoginError())
        }
    }

    private suspend fun request2FACode(authScope: AuthenticationScope) {
        val email = loginState.userIdentifier.text.trim()
        val result = authScope.requestSecondFactorVerificationCode(
            email = email,
            verifiableAction = VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION
        )
        when (result) {
            is RequestSecondFactorVerificationCodeUseCase.Result.Success -> {
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
                    isCodeSent = true,
                    emailUsed = email,
                )
                loginState = loginState.copy(loginError = LoginError.None)
            }

            is RequestSecondFactorVerificationCodeUseCase.Result.Failure -> {
                updateEmailLoginError(LoginError.DialogError.GenericError(result.cause))
            }
        }
    }

    fun onUserIdentifierChange(newText: TextFieldValue) {
        // in case an error is showing e.g. inline error it should be cleared
        if (loginState.loginError is LoginError.TextFieldError && newText != loginState.userIdentifier) {
            clearEmailLoginError()
        }
        loginState = loginState.copy(userIdentifier = newText).updateEmailLoginEnabled()
        savedStateHandle.set(USER_IDENTIFIER_SAVED_STATE_KEY, newText.text)
    }

    fun onPasswordChange(newText: TextFieldValue) {
        loginState = loginState.copy(password = newText).updateEmailLoginEnabled()
    }

    fun onProxyIdentifierChange(newText: TextFieldValue) {
        loginState = loginState.copy(proxyIdentifier = newText).updateEmailLoginEnabled()
    }

    fun onProxyPasswordChange(newText: TextFieldValue) {
        loginState = loginState.copy(proxyPassword = newText).updateEmailLoginEnabled()
    }

    fun onCodeChange(newValue: CodeFieldValue) {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(code = newValue, isCurrentCodeInvalid = false)
        if (newValue.isFullyFilled) {
            login()
        }
    }

    fun onCodeVerificationBackPress() {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
            code = CodeFieldValue(TextFieldValue(""), false),
            isCodeSent = false,
            emailUsed = "",
        )
    }

    fun onCodeResend() {
        viewModelScope.launch {
            resolveCurrentAuthScope()?.let {
                request2FACode(it)
            }
        }
    }
}
