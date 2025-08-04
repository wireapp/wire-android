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

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
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
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.login.ProxyCredentials
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.PersistSelfUserEmailResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList", "ComplexMethod")
@HiltViewModel
class LoginEmailViewModel @Inject constructor(
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val savedStateHandle: SavedStateHandle,
    userDataStoreProvider: UserDataStoreProvider,
    @KaliumCoreLogic coreLogic: CoreLogic,
    private val resendCodeTimer: CountdownTimer,
    private val dispatchers: DispatcherProvider
) : LoginViewModel(
    savedStateHandle,
    clientScopeProviderFactory,
    userDataStoreProvider,
    coreLogic
) {
    val loginNavArgs: LoginNavArgs = savedStateHandle.navArgs()
    private val preFilledUserIdentifier: PreFilledUserIdentifierType = loginNavArgs.userHandle ?: PreFilledUserIdentifierType.None

    val userIdentifierTextState: TextFieldState = TextFieldState()
    val passwordTextState: TextFieldState = TextFieldState()
    val proxyIdentifierTextState: TextFieldState = TextFieldState()
    val proxyPasswordTextState: TextFieldState = TextFieldState()
    var loginState by mutableStateOf(LoginEmailState(preFilledUserIdentifier.userIdentifierEditable))

    val secondFactorVerificationCodeTextState: TextFieldState = TextFieldState()
    var secondFactorVerificationCodeState by mutableStateOf(VerificationCodeState())
    var autoLoginWhenFullCodeEntered: Boolean = true

    @VisibleForTesting
    internal val loginJobData = MutableStateFlow<LoginJobData?>(null)

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
                if (it.length == VerificationCodeState.DEFAULT_VERIFICATION_CODE_LENGTH && autoLoginWhenFullCodeEntered) {
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

    fun login(usernameAllowed: Boolean = true) {
        updateEmailFlowState(LoginState.Loading)
        viewModelScope.launch {
            val previousSessionUserId = coreLogic.getGlobalScope().session.currentSession().let {
                if (it is CurrentSessionResult.Success && it.accountInfo.isValid()) {
                    it.accountInfo.userId
                } else {
                    null
                }
            }
            // first, cancel and revert any previous login if it's still running, just to be sure
            revertLogin()
            // then, start a new login job
            startLoginJob(usernameAllowed).let {
                loginJobData.value = LoginJobData(it, previousSessionUserId)
                it.invokeOnCompletion {
                    loginJobData.value = null
                }
            }
        }
    }

    @Suppress("LongMethod")
    private fun startLoginJob(usernameAllowed: Boolean): Job {
        return viewModelScope.launch {
            // if username is not allowed, we need to check if the provided user identifier is an email
            if (!usernameAllowed && !coreLogic.getGlobalScope().validateEmailUseCase(userIdentifierTextState.text.toString())) {
                updateEmailFlowState(LoginState.Error.TextFieldError.InvalidValue)
                return@launch
            }

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
            loginJobData.update { it?.copy(newSessionUserId = loginResult.authData.userId) }

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
                    revertNewSession()
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
                    is RegisterClientResult.Success -> {
                        updateEmailFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), false))
                    }

                    is RegisterClientResult.E2EICertificateRequired -> {
                        updateEmailFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), true))
                    }

                    is RegisterClientResult.Failure.TooManyClients -> {
                        updateEmailFlowState(LoginState.Error.TooManyDevicesError)
                    }

                    is RegisterClientResult.Failure -> {
                        revertNewSession()
                        updateEmailFlowState(it.toLoginError())
                    }
                }
            }
        }
    }

    private suspend fun revertNewSession() {
        loginJobData.value?.newSessionUserId?.let { newSessionUserId ->
            // logout to cancel all session-related actions, remove all sensitive data and free up resources
            coreLogic.getSessionScope(newSessionUserId).logout(reason = LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
            // delete the session to make it seem like the session was never logged in
            coreLogic.getGlobalScope().deleteSession(newSessionUserId)
        }
        // set the previous session back
        coreLogic.getGlobalScope().session.updateCurrentSession(loginJobData.value?.previousSessionUserId)
    }

    private suspend fun revertLogin() {
        loginJobData.value?.let {
            it.job.cancel()
            revertNewSession()
        }
    }

    fun cancelLogin() {
        viewModelScope.launch {
            revertLogin()
            loginState = loginState.copy(flowState = LoginState.Canceled)
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

internal data class LoginJobData(
    val job: Job,
    val previousSessionUserId: UserId? = null,
    val newSessionUserId: UserId? = null,
)
