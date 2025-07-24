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

import android.text.format.DateUtils.formatElapsedTime
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.isProxyAuthRequired
import com.wire.android.ui.authentication.login.toLoginError
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.login.ProxyCredentials
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.PersistSelfUserEmailResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList", "ComplexMethod")
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
    clientScopeProviderFactory,
    authServerConfigProvider,
    userDataStoreProvider,
    coreLogic
) {
    private val loginNavArgs: LoginNavArgs = savedStateHandle.navArgs()
    private val preFilledUserIdentifier: PreFilledUserIdentifierType = loginNavArgs.userHandle.let {
        if (it.isNullOrEmpty()) PreFilledUserIdentifierType.None else PreFilledUserIdentifierType.PreFilled(it)
    }

    val userIdentifierTextState: TextFieldState = TextFieldState()
    val passwordTextState: TextFieldState = TextFieldState()
    val proxyIdentifierTextState: TextFieldState = TextFieldState()
    val proxyPasswordTextState: TextFieldState = TextFieldState()
    var loginState by mutableStateOf(LoginEmailState(preFilledUserIdentifier is PreFilledUserIdentifierType.None))

    val secondFactorVerificationCodeTextState: TextFieldState = TextFieldState()
    var secondFactorVerificationCodeState by mutableStateOf(VerificationCodeState())

    private var resendCodeTimer: Job? = null

    init {
        userIdentifierTextState.setTextAndPlaceCursorAtEnd(
            if (preFilledUserIdentifier is PreFilledUserIdentifierType.PreFilled) {
                preFilledUserIdentifier.userIdentifier
            } else {
                savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] ?: String.EMPTY
            }
        )
        viewModelScope.launch {
            combine(
                userIdentifierTextState.textAsFlow().distinctUntilChanged().onEach {
                    savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] = it.toString()
                },
                passwordTextState.textAsFlow(),
                proxyIdentifierTextState.textAsFlow(),
                proxyPasswordTextState.textAsFlow()
            ) { _, _, _, _ -> }.collectLatest {
                if (loginState.flowState != LoginState.Loading) {
                    updateEmailFlowState(LoginState.Default)
                }
            }
        }
        viewModelScope.launch {
            secondFactorVerificationCodeTextState.textAsFlow().collectLatest {
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCurrentCodeInvalid = false)
                if (it.length == VerificationCodeState.DEFAULT_VERIFICATION_CODE_LENGTH) {
                    login()
                }
            }
        }
    }

    private fun updateEmailFlowState(flowState: LoginState) {
        val proxyFieldsNotEmpty = proxyIdentifierTextState.text.isNotEmpty() && proxyPasswordTextState.text.isNotEmpty()
        loginState = loginState.copy(
            flowState = flowState,
            loginEnabled = userIdentifierTextState.text.isNotEmpty()
                    && passwordTextState.text.isNotEmpty()
                    && (!serverConfig.isProxyAuthRequired || proxyFieldsNotEmpty)
                    && flowState !is LoginState.Loading
        )
    }

    fun clearLoginErrors() {
        updateEmailFlowState(LoginState.Default)
    }

    @Suppress("LongMethod")
    fun login() {
        updateEmailFlowState(LoginState.Loading)
        viewModelScope.launch {
            val authScope = withContext(dispatchers.io()) { resolveCurrentAuthScope() } ?: return@launch

            val secondFactorVerificationCode = secondFactorVerificationCodeTextState.text.toString()
            val loginResult = withContext(dispatchers.io()) {
                authScope.login(
                    userIdentifier = userIdentifierTextState.text.toString(),
                    password = passwordTextState.text.toString(),
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
                        updateEmailFlowState(it.toLoginError())
                        return@launch
                    }

                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }

            withContext(dispatchers.io()) {
                if (coreLogic.getGlobalScope().validateEmailUseCase(userIdentifierTextState.text.toString())) {
                    coreLogic.getSessionScope(storedUserId).users.persistSelfUserEmail(userIdentifierTextState.text.toString())
                } else {
                    null
                }
            }.let {
                if (it is PersistSelfUserEmailResult.Failure) {
                    updateEmailFlowState(LoginState.Error.DialogError.GenericError(it.coreFailure))
                    return@launch
                }
            }

            withContext(dispatchers.io()) {
                registerClient(
                    userId = storedUserId,
                    password = passwordTextState.text.toString(),
                )
            }.let {
                when (it) {
                    is RegisterClientResult.Failure -> {
                        updateEmailFlowState(it.toLoginError())
                        return@launch
                    }

                    is RegisterClientResult.Success -> {
                        updateEmailFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), false))
                    }

                    is RegisterClientResult.E2EICertificateRequired -> {
                        updateEmailFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), true))
                        return@launch
                    }
                }
            }
        }
    }

    private fun getProxyCredentials(): ProxyCredentials? =
        if (proxyIdentifierTextState.text.isNotBlank() && proxyPasswordTextState.text.isNotBlank()) {
            ProxyCredentials(proxyIdentifierTextState.text.toString(), proxyPasswordTextState.text.toString())
        } else {
            null
        }

    private suspend fun resolveCurrentAuthScope(): AuthenticationScope? =
        coreLogic.versionedAuthenticationScope(serverConfig).invoke(getProxyCredentials()).let {
            when (it) {
                is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                    updateEmailFlowState(LoginState.Error.DialogError.ServerVersionNotSupported)
                    return null
                }

                is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                    updateEmailFlowState(LoginState.Error.DialogError.ClientUpdateRequired)
                    return null
                }

                is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                    updateEmailFlowState(LoginState.Error.DialogError.GenericError(it.genericFailure))
                    return null
                }
            }
        }

    private suspend fun handleAuthenticationFailure(it: AuthenticationResult.Failure, authScope: AuthenticationScope) {
        when (it) {
            is AuthenticationResult.Failure.InvalidCredentials.Missing2FA -> {
                updateEmailFlowState(LoginState.Default)
                request2FACode(authScope)
            }

            is AuthenticationResult.Failure.InvalidCredentials.Invalid2FA -> {
                updateEmailFlowState(LoginState.Default)
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCurrentCodeInvalid = true)
            }

            else -> updateEmailFlowState(it.toLoginError())
        }
    }

    private suspend fun request2FACode(authScope: AuthenticationScope) {
        val email = userIdentifierTextState.text.trim().toString().also {
            // user is using handle to login when 2FA is required
            if (!it.contains("@")) {
                updateEmailFlowState(LoginState.Error.DialogError.Request2FAWithHandle)
                return
            }
        }

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
                updateEmailFlowState(LoginState.Default)
                startResendCodeTimer()
            }

            is RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic -> {
                updateEmailFlowState(LoginState.Error.DialogError.GenericError(result.cause))
            }
        }
    }

    private fun startResendCodeTimer() {
        resendCodeTimer?.cancel()
        resendCodeTimer = viewModelScope.launch {
            var elapsed = RESEND_TIMER_DELAY
            while (elapsed > 0 && isActive) {
                updateResendTimer(elapsed)
                delay(1.seconds)
                elapsed--
            }
            updateResendTimer(null)
        }
    }

    private fun updateResendTimer(elapsed: Long?) {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
            elapsedTimerText = elapsed?.let { formatElapsedTime(elapsed) }
        )
    }

    fun onCodeVerificationBackPress() {
        secondFactorVerificationCodeTextState.clearText()
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(
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

    companion object {
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
        const val RESEND_TIMER_DELAY = 300L
    }
}
