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

package com.wire.android.ui.authentication.login.email

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text2.input.forEachTextValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginError
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.authentication.login.toLoginError
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.textfield.CodeFieldValue
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList", "ComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@HiltViewModel
class LoginEmailViewModel @Inject constructor(
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val savedStateHandle: SavedStateHandle,
    authServerConfigProvider: AuthServerConfigProvider,
    userDataStoreProvider: UserDataStoreProvider,
    @KaliumCoreLogic coreLogic: CoreLogic,
    private val dispatchers: DispatcherProvider
) : LoginViewModel(
    savedStateHandle,
    clientScopeProviderFactory,
    authServerConfigProvider,
    userDataStoreProvider,
    coreLogic
) {

    init {
        onPasswordChange()
        onUserIdentifierChange()
        onProxyIdentifierChange()
    }

    var secondFactorVerificationCodeState by mutableStateOf(
        VerificationCodeState()
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Suppress("LongMethod")
    fun login(onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit) {
        loginState = loginState.copy(emailLoginLoading = true, loginError = LoginError.None)
        updateEmailLoginEnabled()
        viewModelScope.launch {
            val authScope = withContext(dispatchers.io()) { resolveCurrentAuthScope() } ?: return@launch

            val secondFactorVerificationCode = secondFactorVerificationCodeState.codeInput.text.text
            val loginResult = withContext(dispatchers.io()) {
                authScope.login(
                    userIdentifier = userIdentifier.text.toString(),
                    password = password.text.toString(),
                    shouldPersistClient = true,
                    secondFactorVerificationCode = secondFactorVerificationCode
                )
            }
            if (loginResult !is AuthenticationResult.Success) {
                loginResult as AuthenticationResult.Failure
                handleAuthenticationFailure(loginResult, authScope)
                return@launch
            }
            secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCodeInputNecessary = false)
            val storedUserId = withContext(dispatchers.io()) {
                addAuthenticatedUser(
                    authTokens = loginResult.authData,
                    ssoId = loginResult.ssoID,
                    serverConfigId = loginResult.serverConfigId,
                    proxyCredentials = loginResult.proxyCredentials,
                    replace = false
                )
            }.let {
                when (it) {
                    is AddAuthenticatedUserUseCase.Result.Failure -> {
                        updateEmailLoginError(it.toLoginError())
                        return@launch
                    }

                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            withContext(dispatchers.io()) {
                registerClient(
                    userId = storedUserId,
                    password = password.text.toString(),
                )
            }.let {
                when (it) {
                    is RegisterClientResult.Failure -> {
                        updateEmailLoginError(it.toLoginError())
                        return@launch
                    }

                    is RegisterClientResult.Success -> {
                        onSuccess(isInitialSyncCompleted(storedUserId), false)
                    }

                    is RegisterClientResult.E2EICertificateRequired -> {
                        onSuccess(isInitialSyncCompleted(storedUserId), true)
                        return@launch
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private suspend fun resolveCurrentAuthScope(): AuthenticationScope? =
        coreLogic.versionedAuthenticationScope(serverConfig).invoke(
            if (proxyIdentifier.text.isNotBlank() && proxyPassword.text.isNotBlank()) {
                ProxyCredentials(proxyIdentifier.text.toString(), proxyPassword.text.toString())
            } else {
                null
            }
        ).let {
            when (it) {
                is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                    updateEmailLoginError(LoginError.DialogError.ServerVersionNotSupported)
                    return null
                }

                is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                    updateEmailLoginError(LoginError.DialogError.ClientUpdateRequired)
                    return null
                }

                is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                    updateEmailLoginError(LoginError.DialogError.GenericError(it.genericFailure))
                    return null
                }
            }
        }

    private suspend fun handleAuthenticationFailure(it: AuthenticationResult.Failure, authScope: AuthenticationScope) {
        when (it) {
            is AuthenticationResult.Failure.InvalidCredentials.Missing2FA -> {
                loginState = loginState.copy(emailLoginLoading = false)
                updateEmailLoginEnabled()
                request2FACode(authScope)
            }

            is AuthenticationResult.Failure.InvalidCredentials.Invalid2FA -> {
                loginState = loginState.copy(emailLoginLoading = false)
                updateEmailLoginEnabled()
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCurrentCodeInvalid = true)
            }

            else -> updateEmailLoginError(it.toLoginError())
        }
    }

    private suspend fun request2FACode(authScope: AuthenticationScope) {
        val email = userIdentifier.text.toString().trim()
        val result = authScope.requestSecondFactorVerificationCode(
            email = email,
            verifiableAction = VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION
        )
        when (result) {
            is RequestSecondFactorVerificationCodeUseCase.Result.Success,
            RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests -> {
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
                    isCodeInputNecessary = true,
                    emailUsed = email,
                )
                updateEmailLoginError(LoginError.None)
            }

            is RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic -> {
                updateEmailLoginError(LoginError.DialogError.GenericError(result.cause))
            }
        }
    }

    fun onUserIdentifierChange() {
        viewModelScope.launch {
            userIdentifier.forEachTextValue {
                // in case an error is showing e.g. inline error it should be cleared TODO KBX
//                if (loginState.loginError is LoginError.TextFieldError && newText != userIdentifier.text.toString()) {
//                    clearEmailLoginError()
//                }
                updateEmailLoginEnabled()
                savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] = it.toString()
            }

        }
    }


    fun onPasswordChange() {
        viewModelScope.launch {
            password.forEachTextValue {
                updateEmailLoginEnabled()
            }
        }
    }

    fun onProxyIdentifierChange() {
        viewModelScope.launch {
            proxyIdentifier.forEachTextValue {
                updateEmailLoginEnabled()
            }
        }
    }

    fun onProxyPasswordChange(newText: TextFieldValue) {
        loginState = loginState.copy(proxyPassword = newText)
        updateEmailLoginEnabled()
    }

    fun onCodeChange(newValue: CodeFieldValue, onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit) {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(codeInput = newValue, isCurrentCodeInvalid = false)
        if (newValue.isFullyFilled) {
            login(onSuccess)
        }
    }

    fun onCodeVerificationBackPress() {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
            codeInput = CodeFieldValue(TextFieldValue(""), false),
            isCodeInputNecessary = false,
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
