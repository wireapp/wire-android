/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.email

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.verificationcode.VerificationCodeState
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList", "ComplexMethod", "TooManyFunctions")
class LoginEmailViewModel<LinksT, FailureT, UserT, ScopeT, SessionT, BackendRequestT, DomainClaimT>(
    input: LoginEmailInput<LinksT, DomainClaimT>,
    private val savedInputStore: LoginSavedInputStore,
    private val gateway: LoginEmailGateway<LinksT, FailureT, UserT, ScopeT, SessionT, BackendRequestT>,
    private val resendCodeTimer: LoginEmailTimer,
) : ViewModel() {
    var serverConfig: LinksT by mutableStateOf(input.serverConfig)
        private set
    var isBackendConfigured: Boolean by mutableStateOf(input.isBackendConfigured)
        private set
    val domainClaimedByOrg: DomainClaimT? = input.domainClaimedByOrg
    val userIdentifierTextState = TextFieldState()
    val passwordTextState = TextFieldState()
    val proxyIdentifierTextState = TextFieldState()
    val proxyPasswordTextState = TextFieldState()
    var loginState by mutableStateOf(LoginEmailState<FailureT, UserT>(input.identifierEditable))
    val secondFactorVerificationCodeTextState = TextFieldState()
    var secondFactorVerificationCodeState by mutableStateOf(VerificationCodeState())
    var autoLoginWhenFullCodeEntered = true
    internal val loginJobData = MutableStateFlow<LoginJobData<UserT>?>(null)

    init {
        userIdentifierTextState.setTextAndPlaceCursorAtEnd(input.preFilledIdentifier ?: savedInputStore.userIdentifier.orEmpty())
        viewModelScope.launch {
            combine(
                userIdentifierTextState.textAsFlow().distinctUntilChanged().onEach { savedInputStore.userIdentifier = it.toString() },
                passwordTextState.textAsFlow(),
                proxyIdentifierTextState.textAsFlow(),
                proxyPasswordTextState.textAsFlow(),
            ) { _, _, _, _ -> }.collectLatest {
                if (loginState.flowState.canBeResetByCredentialChange()) {
                    updateEmailFlowState(LoginState.Default, showInvalidCredentialsError = false)
                }
            }
        }
        viewModelScope.launch {
            secondFactorVerificationCodeTextState.textAsFlow().collectLatest {
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCurrentCodeInvalid = false)
                if (it.length == VerificationCodeState.DEFAULT_VERIFICATION_CODE_LENGTH && autoLoginWhenFullCodeEntered) login()
            }
        }
    }

    fun onBackendConfigLinkEntered(input: String) {
        viewModelScope.launch {
            val request = gateway.parseBackendConfig(input)
            if (request == null) {
                updateBackendConfigState(LoginEmailState.BackendConfigState.Error)
                return@launch
            }
            updateBackendConfigState(LoginEmailState.BackendConfigState.Loading)
            when (val result = gateway.configureBackend(request)) {
                is LoginEmailBackendResult.Success -> {
                    serverConfig = result.serverConfig
                    isBackendConfigured = true
                    updateBackendConfigState(LoginEmailState.BackendConfigState.Success)
                }
                LoginEmailBackendResult.Failure -> updateBackendConfigState(LoginEmailState.BackendConfigState.Error)
            }
        }
    }

    fun onBackendConfigSuccessContinue() = updateBackendConfigState(LoginEmailState.BackendConfigState.Missing)
    private fun updateBackendConfigState(state: LoginEmailState.BackendConfigState) {
        loginState = loginState.copy(backendConfigState = state)
    }

    private fun updateEmailFlowState(
        flowState: LoginState<FailureT, UserT, Nothing>,
        showInvalidCredentialsError: Boolean = when (flowState) {
            LoginState.Error.DialogError.InvalidCredentialsError -> true
            LoginState.Default -> loginState.showInvalidCredentialsError
            else -> false
        },
    ) {
        val proxyFieldsNotEmpty = proxyIdentifierTextState.text.isNotEmpty() && proxyPasswordTextState.text.isNotEmpty()
        loginState = loginState.copy(
            flowState = flowState,
            loginEnabled = userIdentifierTextState.text.isNotEmpty() && passwordTextState.text.isNotEmpty() &&
                    (!gateway.isProxyAuthRequired(serverConfig) || proxyFieldsNotEmpty) && flowState !is LoginState.Loading,
            showInvalidCredentialsError = showInvalidCredentialsError,
        )
    }

    private fun LoginState<FailureT, UserT, Nothing>.canBeResetByCredentialChange() = when (this) {
        LoginState.Loading, LoginState.Canceled, is LoginState.Success<*>, is LoginState.Error.TooManyDevicesError<*> -> false
        else -> true
    }

    fun clearLoginErrors() = updateEmailFlowState(LoginState.Default)

    fun login(usernameAllowed: Boolean = true) {
        updateEmailFlowState(LoginState.Loading)
        viewModelScope.launch {
            val previousSessionUserId = gateway.currentValidSession()
            revertLogin()
            startLoginJob(usernameAllowed).let { job ->
                loginJobData.value = LoginJobData(job, previousSessionUserId)
                job.invokeOnCompletion { loginJobData.value = null }
            }
        }
    }

    @Suppress("LongMethod")
    private fun startLoginJob(usernameAllowed: Boolean): Job = viewModelScope.launch {
        var retainAuthenticatedSession = false
        try {
            if (!usernameAllowed && !gateway.isEmail(userIdentifierTextState.text.toString())) {
                updateEmailFlowState(LoginState.Error.TextFieldError.InvalidValue)
                return@launch
            }
            val scope = resolveCurrentAuthScope() ?: return@launch
            val secondFactorCode = secondFactorVerificationCodeTextState.text.toString()
            when (val result = gateway.authenticate(
                scope,
                { userIdentifierTextState.text.toString() },
                { passwordTextState.text.toString() },
                secondFactorCode,
            )) {
                is LoginEmailAuthenticationResult.Success -> {
                    secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCodeInputNecessary = false)
                    val storedUserId = when (val stored = gateway.storeSession(result.session)) {
                        is LoginEmailStoreResult.Success -> stored.userId
                        LoginEmailStoreResult.UserAlreadyExists -> {
                            updateEmailFlowState(LoginState.Error.DialogError.UserAlreadyExists); return@launch
                        }
                        is LoginEmailStoreResult.Failure -> {
                            updateEmailFlowState(LoginState.Error.DialogError.GenericError(stored.failure)); return@launch
                        }
                    }
                    loginJobData.update { it?.copy(newSessionUserId = storedUserId) }
                    when (val persisted = gateway.persistEmailIfNeeded(storedUserId) { userIdentifierTextState.text.toString() }) {
                        LoginEmailPersistResult.Success -> Unit
                        is LoginEmailPersistResult.Failure -> {
                            updateEmailFlowState(LoginState.Error.DialogError.GenericError(persisted.failure)); return@launch
                        }
                    }
                    when (val client = gateway.registerClient(storedUserId) { passwordTextState.text.toString() }) {
                        is LoginEmailClientResult.Success -> {
                            retainAuthenticatedSession = true
                            updateEmailFlowState(LoginState.Success(client.initialSyncCompleted, false, storedUserId))
                        }
                        is LoginEmailClientResult.E2EICertificateRequired -> {
                            retainAuthenticatedSession = true
                            updateEmailFlowState(LoginState.Success(client.initialSyncCompleted, true, storedUserId))
                        }
                        LoginEmailClientResult.TooManyDevices -> {
                            retainAuthenticatedSession = true
                            updateEmailFlowState(LoginState.Error.TooManyDevicesError(storedUserId))
                        }
                        LoginEmailClientResult.InvalidCredentials -> updateEmailFlowState(LoginState.Error.DialogError.InvalidCredentialsError)
                        LoginEmailClientResult.PasswordRequired ->
                            updateEmailFlowState(LoginState.Error.DialogError.PasswordNeededToRegisterClient)
                        is LoginEmailClientResult.Failure -> updateEmailFlowState(LoginState.Error.DialogError.GenericError(client.failure))
                    }
                }
                LoginEmailAuthenticationResult.MissingSecondFactor -> {
                    updateEmailFlowState(LoginState.Default); request2FACode(scope)
                }
                LoginEmailAuthenticationResult.InvalidSecondFactor -> {
                    updateEmailFlowState(LoginState.Default)
                    secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCurrentCodeInvalid = true)
                }
                LoginEmailAuthenticationResult.ProxyError -> updateEmailFlowState(LoginState.Error.DialogError.ProxyError)
                LoginEmailAuthenticationResult.InvalidCredentials -> updateEmailFlowState(LoginState.Error.DialogError.InvalidCredentialsError)
                LoginEmailAuthenticationResult.InvalidIdentifier -> updateEmailFlowState(LoginState.Error.TextFieldError.InvalidValue)
                LoginEmailAuthenticationResult.AccountSuspended -> updateEmailFlowState(LoginState.Error.DialogError.AccountSuspended)
                LoginEmailAuthenticationResult.AccountPendingActivation ->
                    updateEmailFlowState(LoginState.Error.DialogError.AccountPendingActivation)
                is LoginEmailAuthenticationResult.Failure -> updateEmailFlowState(LoginState.Error.DialogError.GenericError(result.failure))
            }
        } finally {
            val data = loginJobData.value
            if (!retainAuthenticatedSession && data?.newSessionUserId != null) {
                withContext(NonCancellable) { gateway.revertSession(data.newSessionUserId, data.previousSessionUserId) }
            }
        }
    }

    private suspend fun revertLogin() {
        loginJobData.value?.let { data ->
            data.job.cancelAndJoin()
            if (loginJobData.value?.newSessionUserId != null) gateway.revertSession(data.newSessionUserId, data.previousSessionUserId)
        }
    }

    fun cancelLogin() {
        viewModelScope.launch {
            revertLogin()
            loginState = loginState.copy(flowState = LoginState.Canceled)
        }
    }

    private fun currentProxyCredentials(): LoginEmailProxyCredentials? =
        if (proxyIdentifierTextState.text.isNotBlank() && proxyPasswordTextState.text.isNotBlank()) {
            LoginEmailProxyCredentials(proxyIdentifierTextState.text.toString(), proxyPasswordTextState.text.toString())
        } else null

    private suspend fun resolveCurrentAuthScope(): ScopeT? = when (val result = gateway.resolveScope(serverConfig, ::currentProxyCredentials)) {
        is LoginEmailScopeResult.Success -> result.scope
        LoginEmailScopeResult.UnknownServerVersion -> {
            updateEmailFlowState(LoginState.Error.DialogError.ServerVersionNotSupported); null
        }
        LoginEmailScopeResult.ClientUpdateRequired -> {
            updateEmailFlowState(LoginState.Error.DialogError.ClientUpdateRequired); null
        }
        is LoginEmailScopeResult.Failure -> {
            updateEmailFlowState(LoginState.Error.DialogError.GenericError(result.failure)); null
        }
    }

    private suspend fun request2FACode(scope: ScopeT) {
        val email = userIdentifierTextState.text.trim().toString()
        if (!email.contains("@")) {
            updateEmailFlowState(LoginState.Error.DialogError.Request2FAWithHandle); return
        }
        when (val result = gateway.requestSecondFactorCode(scope, email)) {
            LoginEmailVerificationResult.Sent, LoginEmailVerificationResult.TooManyRequests -> {
                secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCodeInputNecessary = true, emailUsed = email)
                updateEmailFlowState(LoginState.Default)
                startResendCodeTimer()
            }
            is LoginEmailVerificationResult.Failure -> updateEmailFlowState(LoginState.Error.DialogError.GenericError(result.failure))
        }
    }

    private fun startResendCodeTimer() {
        viewModelScope.launch { resendCodeTimer.start(RESEND_TIMER_DELAY, ::updateResendTimer) { updateResendTimer(null) } }
    }
    private fun updateResendTimer(timerText: String?) {
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(remainingTimerText = timerText)
    }
    fun onCodeVerificationBackPress() {
        secondFactorVerificationCodeTextState.clearText()
        secondFactorVerificationCodeState = secondFactorVerificationCodeState.copy(isCodeInputNecessary = false, emailUsed = "")
    }
    fun onCodeResend() {
        viewModelScope.launch { resolveCurrentAuthScope()?.let { request2FACode(it) } }
    }

    companion object {
        const val USER_IDENTIFIER_SAVED_STATE_KEY = "user_identifier"
        const val RESEND_TIMER_DELAY = 300L
    }
}

internal data class LoginJobData<UserT>(
    val job: Job,
    val previousSessionUserId: UserT? = null,
    val newSessionUserId: UserT? = null,
)
