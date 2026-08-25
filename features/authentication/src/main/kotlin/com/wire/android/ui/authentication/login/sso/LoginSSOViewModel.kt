/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.sso

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class LoginSSOViewModel<LinksT, FailureT, UserT, SsoFailureT, SessionT>(
    input: LoginSSOInput<LinksT>,
    private val savedInputStore: LoginSavedInputStore,
    private val gateway: LoginSSOGateway<LinksT, FailureT, UserT, SessionT>,
) : ViewModel() {
    val serverConfig: LinksT = input.serverConfig
    val ssoTextState: TextFieldState = TextFieldState()
    var openWebUrl = MutableSharedFlow<LoginSSOWebRequest<LinksT>>()

    var loginState: LoginSSOState<LinksT, FailureT, UserT, SsoFailureT> by mutableStateOf(LoginSSOState())

    private var pendingNomadServiceUrl: String? = input.pendingNomadServiceUrl
    private var pendingCookieLabel: String? = input.pendingCookieLabel
    private var pendingSsoSession: PendingSsoSession<SessionT>? = null

    init {
        observeSSOCodeInput()
    }

    private fun observeSSOCodeInput() {
        ssoTextState.setTextAndPlaceCursorAtEnd(savedInputStore.ssoCode.orEmpty())
        viewModelScope.launch {
            ssoTextState.textAsFlow().distinctUntilChanged().collectLatest {
                if (loginState.flowState != LoginState.Loading) {
                    updateSSOFlowState(LoginState.Default)
                }
                savedInputStore.ssoCode = it.toString()
            }
        }
    }

    private fun updateSSOFlowState(flowState: LoginState<FailureT, UserT, SsoFailureT>) {
        loginState = loginState.copy(
            flowState = flowState,
            loginEnabled = ssoTextState.text.isNotEmpty() && flowState !is LoginState.Loading,
        )
    }

    fun clearLoginErrors() {
        updateSSOFlowState(LoginState.Default)
    }

    fun onSsoIdentityChangeDismissed() {
        pendingSsoSession = null
        loginState = loginState.copy(showSsoIdentityChangedDialog = false)
        updateSSOFlowState(LoginState.Default)
    }

    fun onSsoIdentityChangeConfirmed() {
        val pending = pendingSsoSession ?: return
        pendingSsoSession = null
        loginState = loginState.copy(showSsoIdentityChangedDialog = false)
        updateSSOFlowState(LoginState.Loading)

        viewModelScope.launch {
            when (val result = gateway.replaceRetainedSession(pending.session)) {
                is LoginSSOReplaceSessionResult.Success ->
                    continueAfterSsoSessionStored(result.userId, pending.isNomadSession)
                LoginSSOReplaceSessionResult.UserAlreadyExists ->
                    updateSSOFlowState(LoginState.Error.DialogError.UserAlreadyExists)
                is LoginSSOReplaceSessionResult.Failure ->
                    updateSSOFlowState(LoginState.Error.DialogError.GenericError(result.failure))
            }
        }
    }

    fun login() {
        updateSSOFlowState(LoginState.Loading)
        val currentInput = ssoTextState.text.toString()
        if (gateway.isEmail(currentInput)) domainLookupFlow() else ssoLoginWithCodeFlow()
    }

    fun onCustomServerDialogDismiss() {
        loginState = loginState.copy(customServerDialogState = null)
        updateSSOFlowState(LoginState.Default)
    }

    fun onCustomServerDialogConfirm() {
        viewModelScope.launch {
            loginState.customServerDialogState?.let { state ->
                when (val result = gateway.fetchDefaultSSOCode(state.serverLinks)) {
                    is LoginSSODefaultCodeResult.Success -> result.code?.let { initiateSSO(state.serverLinks, it) }
                    is LoginSSODefaultCodeResult.Failure -> updateSSOFlowState(result.cause.toLoginError())
                    LoginSSODefaultCodeResult.Unavailable -> Unit
                }
            }
        }
    }

    fun handleSSOCodeAutoLogin(
        ssoCode: String,
        autoInitiateLogin: Boolean,
        nomadServiceUrl: String?,
        cookieLabel: String?,
    ) {
        pendingNomadServiceUrl = nomadServiceUrl
        pendingCookieLabel = cookieLabel
        ssoTextState.setTextAndPlaceCursorAtEnd(ssoCode)
        if (autoInitiateLogin) login()
    }

    fun domainLookupFlow() {
        viewModelScope.launch {
            when (val result = gateway.lookupDomain { ssoTextState.text.toString() }) {
                is LoginSSODomainLookupResult.Success -> {
                    loginState = loginState.copy(
                        customServerDialogState = LoginSSOCustomServerDialogState(result.serverConfig),
                    )
                    updateSSOFlowState(LoginState.Default)
                }
                is LoginSSODomainLookupResult.Failure ->
                    updateSSOFlowState(LoginState.Error.DialogError.GenericError(result.failure))
                LoginSSODomainLookupResult.AuthenticationUnavailable ->
                    updateSSOFlowState(LoginState.Error.DialogError.ServerVersionNotSupported)
            }
        }
    }

    private fun ssoLoginWithCodeFlow() {
        viewModelScope.launch { initiateSSO(serverConfig, ssoTextState.text.toString()) }
    }

    private suspend fun initiateSSO(serverConfig: LinksT, ssoCode: String) {
        when (val result = gateway.initiateSSO(serverConfig, ssoCode, pendingCookieLabel)) {
            is LoginSSOInitiationResult.Success -> openWebUrl(result.redirectUrl, serverConfig)
            LoginSSOInitiationResult.InvalidCodeFormat -> updateSSOFlowState(LoginState.Error.TextFieldError.InvalidValue)
            LoginSSOInitiationResult.InvalidCode -> updateSSOFlowState(LoginState.Error.DialogError.InvalidSSOCodeError)
            is LoginSSOInitiationResult.Failure -> updateSSOFlowState(result.cause.toLoginError())
        }
    }

    fun establishSSOSession(cookie: String, serverConfigId: String) {
        updateSSOFlowState(LoginState.Loading)
        val isNomadFlow = pendingNomadServiceUrl != null
        viewModelScope.launch {
            when (
                val result = gateway.establishSession(
                    cookie = cookie,
                    serverConfigId = serverConfigId,
                    consumeNomadServiceUrl = ::consumePendingNomadServiceUrl,
                    consumeCookieLabel = ::consumePendingCookieLabel,
                )
            ) {
                is LoginSSOSessionResult.Success -> continueAfterSsoSessionStored(result.userId, isNomadFlow)
                is LoginSSOSessionResult.IdentityChanged -> {
                    pendingSsoSession = PendingSsoSession(result.session, result.isNomadSession)
                    loginState = loginState.copy(
                        flowState = LoginState.Default,
                        showSsoIdentityChangedDialog = true,
                    )
                }
                LoginSSOSessionResult.InvalidCookie -> updateSSOFlowState(LoginState.Error.DialogError.InvalidSSOCookie)
                LoginSSOSessionResult.UserAlreadyExists -> updateSSOFlowState(LoginState.Error.DialogError.UserAlreadyExists)
                is LoginSSOSessionResult.Failure -> updateSSOFlowState(result.cause.toLoginError())
            }
        }
    }

    fun handleSSOFailure(failure: SsoFailureT) {
        updateSSOFlowState(LoginState.Error.DialogError.SSOResultError(failure))
    }

    private suspend fun continueAfterSsoSessionStored(userId: UserT, isNomadFlow: Boolean) {
        gateway.logSessionContinuation(isNomadFlow)
        if (isNomadFlow) restoreCryptoStateAndContinue(userId) else registerClientAndUpdateState(userId)
    }

    private suspend fun registerClientAndUpdateState(userId: UserT, setLastDeviceId: Boolean = false) {
        when (val result = gateway.registerClient(userId, setLastDeviceId)) {
            is LoginSSORegisterClientResult.Success ->
                updateSSOFlowState(LoginState.Success(result.initialSyncCompleted, false, userId))
            is LoginSSORegisterClientResult.E2EICertificateRequired ->
                updateSSOFlowState(LoginState.Success(result.initialSyncCompleted, true, userId))
            LoginSSORegisterClientResult.TooManyDevices ->
                updateSSOFlowState(LoginState.Error.TooManyDevicesError(userId))
            LoginSSORegisterClientResult.InvalidCredentials -> {
                gateway.revertSession(userId)
                updateSSOFlowState(LoginState.Error.DialogError.InvalidCredentialsError)
            }
            LoginSSORegisterClientResult.PasswordRequired -> {
                gateway.revertSession(userId)
                updateSSOFlowState(LoginState.Error.DialogError.PasswordNeededToRegisterClient)
            }
            is LoginSSORegisterClientResult.Failure -> {
                gateway.revertSession(userId)
                updateSSOFlowState(LoginState.Error.DialogError.GenericError(result.failure))
            }
        }
    }

    private suspend fun restoreCryptoStateAndContinue(userId: UserT) {
        when (val result = gateway.restoreCryptoState(userId)) {
            is LoginSSORestoreResult.Success ->
                updateSSOFlowState(LoginState.Success(result.initialSyncCompleted, false, userId))
            LoginSSORestoreResult.NoBackupAvailable -> registerClientAndUpdateState(userId, setLastDeviceId = true)
            LoginSSORestoreResult.SessionUnavailable -> Unit
            is LoginSSORestoreResult.Failure -> {
                gateway.revertSession(userId)
                updateSSOFlowState(LoginState.Error.DialogError.GenericError(result.failure))
            }
        }
    }

    private fun openWebUrl(url: String, customServerConfig: LinksT) {
        viewModelScope.launch {
            updateSSOFlowState(LoginState.Default)
            openWebUrl.emit(LoginSSOWebRequest(url, customServerConfig))
        }
    }

    private fun LoginSSOFailure<FailureT>.toLoginError(): LoginState<FailureT, UserT, SsoFailureT> = when (this) {
        is LoginSSOFailure.Generic -> LoginState.Error.DialogError.GenericError(failure)
        LoginSSOFailure.ClientUpdateRequired -> LoginState.Error.DialogError.ClientUpdateRequired
        LoginSSOFailure.ServerVersionNotSupported -> LoginState.Error.DialogError.ServerVersionNotSupported
    }

    private fun consumePendingNomadServiceUrl(): String? = pendingNomadServiceUrl.also { pendingNomadServiceUrl = null }
    private fun consumePendingCookieLabel(): String? = pendingCookieLabel.also { pendingCookieLabel = null }

    private data class PendingSsoSession<SessionT>(val session: SessionT, val isNomadSession: Boolean)
}
