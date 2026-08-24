/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.textfield.textAsFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "TooManyFunctions")
class NewLoginViewModel<LinksT, FailureT, UserT, SsoFailureT, SessionT, BackendRequestT>(
    input: NewLoginInput<LinksT>,
    private val gateway: NewLoginGateway<LinksT, FailureT, UserT, SessionT>,
    private val backendGateway: NewLoginBackendGateway<LinksT, BackendRequestT>,
    private val savedStateStore: NewLoginSavedStateStore,
) : ActionsViewModel<NewLoginAction<LinksT, UserT>>() {
    private val defaultServerConfig = input.defaultServerConfig
    private val emptyServerConfig = input.emptyServerConfig
    private val isDefaultServerConfigured = input.isDefaultServerConfigured
    private var hasPreFilledIdentifier = input.preFilledIdentifier != null
    private var pendingNomadServiceUrl = input.pendingNomadServiceUrl
    private var pendingCookieLabel = input.pendingCookieLabel
    private var pendingSsoSession: PendingSsoSession<SessionT>? = null
    private var canUseBackend by mutableStateOf(
        if (input.initialCustomServerConfig != null) input.isInitialCustomServerConfigured else isDefaultServerConfigured
    )

    var serverConfig: LinksT by mutableStateOf(input.initialCustomServerConfig ?: defaultServerConfig)
        private set
    var state by mutableStateOf(NewLoginScreenState<LinksT, FailureT, SsoFailureT>())
        private set
    val userIdentifierTextState = TextFieldState()

    init {
        userIdentifierTextState.setTextAndPlaceCursorAtEnd(
            input.preFilledIdentifier
                ?: input.managedSsoCode?.takeIf { input.initialCustomServerConfig == null }
                ?: savedStateStore.userIdentifier.orEmpty()
        )
        viewModelScope.launch {
            userIdentifierTextState.textAsFlow().distinctUntilChanged().onEach {
                savedStateStore.userIdentifier = it.toString()
            }.collectLatest {
                getAndUpdateLoginFlowState { currentState ->
                    if (!canUseBackend) {
                        when (currentState) {
                            NewLoginFlowState.LoadingBackendConfig,
                            NewLoginFlowState.BackendConfigError,
                            NewLoginFlowState.BackendConfigSuccess -> currentState
                            else -> NewLoginFlowState.MissingBackendConfig
                        }
                    } else if (currentState is NewLoginFlowState.Error.TextFieldError) {
                        NewLoginFlowState.Default
                    } else {
                        currentState
                    }
                }
            }
        }

        when {
            !canUseBackend -> updateLoginFlowState(NewLoginFlowState.MissingBackendConfig)
            input.showBackendConfigSuccess && input.isInitialCustomServerConfigured ->
                updateLoginFlowState(NewLoginFlowState.BackendConfigSuccess)
            userIdentifierTextState.text.isEmpty() && !hasPreFilledIdentifier ->
                fetchDefaultSsoCodeIfNeeded(persistImmediately = true)
        }
    }

    fun onBackendConfigSuccessContinue() {
        updateLoginFlowState(NewLoginFlowState.Default)
        if (userIdentifierTextState.text.isEmpty() && !hasPreFilledIdentifier) {
            fetchDefaultSsoCodeIfNeeded(persistImmediately = false)
        }
    }

    fun onNavigationArgumentsChanged(input: NewLoginNavigationInput<LinksT>) {
        hasPreFilledIdentifier = input.preFilledIdentifier != null
        pendingNomadServiceUrl = input.pendingNomadServiceUrl
        pendingCookieLabel = input.pendingCookieLabel
        input.preFilledIdentifier?.let(userIdentifierTextState::setTextAndPlaceCursorAtEnd)

        serverConfig = input.customServerConfig ?: defaultServerConfig
        canUseBackend = if (input.customServerConfig != null) input.isCustomServerConfigured else isDefaultServerConfigured
        backendGateway.select(serverConfig)

        when {
            !canUseBackend -> updateLoginFlowState(NewLoginFlowState.MissingBackendConfig)
            input.showBackendConfigSuccess && input.customServerConfig != null ->
                updateLoginFlowState(NewLoginFlowState.BackendConfigSuccess)
        }
    }

    fun onNoBackendSelected() {
        serverConfig = emptyServerConfig
        canUseBackend = false
        backendGateway.clear()
        updateLoginFlowState(NewLoginFlowState.MissingBackendConfig)
    }

    fun onBackendConfigLinkEntered(input: String) {
        viewModelScope.launch {
            val request = backendGateway.parse(input)
            if (request == null) {
                updateLoginFlowState(NewLoginFlowState.BackendConfigError)
                return@launch
            }
            updateLoginFlowState(NewLoginFlowState.LoadingBackendConfig)
            when (val result = backendGateway.configure(request)) {
                is NewLoginBackendResult.Success -> {
                    serverConfig = result.serverConfig
                    canUseBackend = true
                    updateLoginFlowState(NewLoginFlowState.BackendConfigSuccess)
                }
                NewLoginBackendResult.Failure -> updateLoginFlowState(NewLoginFlowState.BackendConfigError)
            }
        }
    }

    fun onLoginStarted() {
        viewModelScope.launch {
            if (!canUseBackend) {
                updateLoginFlowState(NewLoginFlowState.MissingBackendConfig)
                return@launch
            }
            updateLoginFlowState(NewLoginFlowState.Loading)
            val sanitizedInput = userIdentifierTextState.text.trim().toString()
            when (gateway.validateIdentifier(sanitizedInput)) {
                NewLoginIdentifierValidation.Invalid ->
                    updateLoginFlowState(NewLoginFlowState.Error.TextFieldError.InvalidValue)
                NewLoginIdentifierValidation.Email -> getEnterpriseLoginFlow(sanitizedInput)
                NewLoginIdentifierValidation.SsoCode -> initiateSSO(serverConfig, sanitizedInput)
            }
        }
    }

    internal suspend fun getEnterpriseLoginFlow(email: String) {
        when (val result = gateway.enterpriseLogin(serverConfig, email)) {
            NewLoginEnterpriseResult.NotSupported -> {
                sendAction(NewLoginAction.EnterpriseLoginNotSupported(email))
                updateLoginFlowState(NewLoginFlowState.Default)
            }
            is NewLoginEnterpriseResult.Password -> {
                sendAction(
                    NewLoginAction.EmailPassword(
                        userIdentifier = email,
                        serverConfig = serverConfig,
                        isCloudAccountCreationPossible = result.isCloudAccountCreationPossible,
                        claimedDomain = result.claimedDomain,
                    )
                )
                updateLoginFlowState(NewLoginFlowState.Default)
            }
            is NewLoginEnterpriseResult.Sso -> initiateSSO(serverConfig, result.code, result.identityProviderId)
            is NewLoginEnterpriseResult.CustomBackend ->
                updateLoginFlowState(NewLoginFlowState.CustomConfigDialog(result.serverConfig))
            is NewLoginEnterpriseResult.Failure -> updateLoginFlowState(result.cause.toFlowError())
        }
    }

    fun onDismissDialog() = updateLoginFlowState(NewLoginFlowState.Default)

    fun onSsoIdentityChangeDismissed() {
        pendingSsoSession = null
        updateLoginFlowState(NewLoginFlowState.Default)
    }

    fun onSsoIdentityChangeConfirmed() {
        val pending = pendingSsoSession ?: return
        pendingSsoSession = null
        updateLoginFlowState(NewLoginFlowState.Loading)
        viewModelScope.launch {
            when (val result = gateway.replaceRetainedSession(pending.session)) {
                is NewLoginReplaceSessionResult.Success -> continueAfterSsoSessionStored(result.userId, pending.isNomadSession)
                NewLoginReplaceSessionResult.SsoIdentityChanged ->
                    updateLoginFlowState(NewLoginFlowState.SsoIdentityChanged)
                NewLoginReplaceSessionResult.UserAlreadyExists ->
                    updateLoginFlowState(NewLoginFlowState.Error.DialogError.UserAlreadyExists)
                is NewLoginReplaceSessionResult.Failure ->
                    updateLoginFlowState(NewLoginFlowState.Error.DialogError.GenericError(result.failure))
            }
        }
    }

    fun onCustomServerDialogConfirm(customServerConfig: LinksT) {
        viewModelScope.launch {
            when (val result = gateway.fetchDefaultSsoCode(customServerConfig)) {
                is NewLoginDefaultSsoCodeResult.Success -> if (result.code != null) {
                    initiateSSO(customServerConfig, result.code)
                } else {
                    sendAction(NewLoginAction.CustomConfig(userIdentifierTextState.text.toString(), customServerConfig))
                    updateLoginFlowState(NewLoginFlowState.Default)
                }
                is NewLoginDefaultSsoCodeResult.Failure -> updateLoginFlowState(result.cause.toFlowError())
            }
        }
    }

    internal suspend fun initiateSSO(
        serverConfig: LinksT,
        ssoCode: String,
        ssoIdentityProviderId: String? = null,
    ) {
        savedStateStore.pendingSsoIdentityProviderId = null
        when (val result = gateway.initiateSso(serverConfig, ssoCode, pendingCookieLabel)) {
            is NewLoginSsoInitiationResult.Success -> {
                savedStateStore.pendingSsoIdentityProviderId = ssoIdentityProviderId
                updateLoginFlowState(NewLoginFlowState.Default)
                sendAction(NewLoginAction.SSO(result.redirectUrl, userIdentifierTextState.text.toString()))
                updateLoginFlowState(NewLoginFlowState.Default)
            }
            NewLoginSsoInitiationResult.InvalidCodeFormat ->
                updateLoginFlowState(NewLoginFlowState.Error.TextFieldError.InvalidValue)
            NewLoginSsoInitiationResult.InvalidCode ->
                updateLoginFlowState(NewLoginFlowState.Error.DialogError.InvalidSSOCode)
            is NewLoginSsoInitiationResult.Failure -> updateLoginFlowState(result.cause.toFlowError())
        }
    }

    fun handleSSOResult(result: NewLoginSsoCallback<SsoFailureT>) {
        updateLoginFlowState(NewLoginFlowState.Loading)
        when (result) {
            is NewLoginSsoCallback.Failure ->
                updateLoginFlowState(NewLoginFlowState.Error.DialogError.SSOResultFailure(result.failure))
            is NewLoginSsoCallback.Success -> {
                val isNomadFlow = pendingNomadServiceUrl != null
                viewModelScope.launch {
                    when (val session = gateway.establishSession(
                        cookie = result.cookie,
                        serverConfigId = result.serverConfigId,
                        ssoIdentityProviderId = savedStateStore.consumePendingSsoIdentityProviderId(),
                        consumeNomadServiceUrl = ::consumePendingNomadServiceUrl,
                        consumeCookieLabel = ::consumePendingCookieLabel,
                    )) {
                        is NewLoginSessionResult.Success -> continueAfterSsoSessionStored(session.userId, isNomadFlow)
                        is NewLoginSessionResult.IdentityChanged -> {
                            pendingSsoSession = PendingSsoSession(session.session, session.isNomadSession)
                            updateLoginFlowState(NewLoginFlowState.SsoIdentityChanged)
                        }
                        NewLoginSessionResult.InvalidCookie ->
                            updateLoginFlowState(NewLoginFlowState.Error.DialogError.InvalidSSOCookie)
                        NewLoginSessionResult.UserAlreadyExists ->
                            updateLoginFlowState(NewLoginFlowState.Error.DialogError.UserAlreadyExists)
                        is NewLoginSessionResult.Failure -> updateLoginFlowState(session.cause.toFlowError())
                    }
                }
            }
        }
    }

    private suspend fun continueAfterSsoSessionStored(userId: UserT, isNomadFlow: Boolean) {
        gateway.logSessionContinuation(isNomadFlow)
        if (isNomadFlow) restoreCryptoStateAndContinue(userId) else registerClientAndUpdateState(userId)
    }

    private suspend fun registerClientAndUpdateState(userId: UserT, setLastDeviceId: Boolean = false) {
        when (val result = gateway.registerClient(userId, setLastDeviceId)) {
            is NewLoginRegisterClientResult.Success -> {
                sendAction(
                    NewLoginAction.Success(
                        if (result.initialSyncCompleted) NewLoginAction.Success.NextStep.None(userId)
                        else NewLoginAction.Success.NextStep.InitialSync(userId)
                    )
                )
                updateLoginFlowState(NewLoginFlowState.Default)
            }
            NewLoginRegisterClientResult.E2EICertificateRequired -> {
                sendAction(NewLoginAction.Success(NewLoginAction.Success.NextStep.E2EIEnrollment(userId)))
                updateLoginFlowState(NewLoginFlowState.Default)
            }
            NewLoginRegisterClientResult.TooManyDevices -> {
                sendAction(NewLoginAction.Success(NewLoginAction.Success.NextStep.TooManyDevices(userId)))
                updateLoginFlowState(NewLoginFlowState.Default)
            }
            is NewLoginRegisterClientResult.Failure ->
                updateLoginFlowState(NewLoginFlowState.Error.DialogError.GenericError(result.failure))
        }
    }

    private suspend fun restoreCryptoStateAndContinue(userId: UserT) {
        when (val result = gateway.restoreCryptoState(userId)) {
            is NewLoginRestoreResult.Success -> {
                sendAction(
                    NewLoginAction.Success(
                        if (result.initialSyncCompleted) NewLoginAction.Success.NextStep.None(userId)
                        else NewLoginAction.Success.NextStep.InitialSync(userId)
                    )
                )
                updateLoginFlowState(NewLoginFlowState.Default)
            }
            NewLoginRestoreResult.NoBackupAvailable -> registerClientAndUpdateState(userId, setLastDeviceId = true)
            NewLoginRestoreResult.SessionUnavailable -> Unit
            is NewLoginRestoreResult.Failure -> {
                gateway.revertSession(userId)
                updateLoginFlowState(NewLoginFlowState.Error.DialogError.GenericError(result.failure))
            }
        }
    }

    private fun updateLoginFlowState(flowState: NewLoginFlowState<LinksT, FailureT, SsoFailureT>) =
        getAndUpdateLoginFlowState { flowState }

    private fun getAndUpdateLoginFlowState(
        update: (NewLoginFlowState<LinksT, FailureT, SsoFailureT>) -> NewLoginFlowState<LinksT, FailureT, SsoFailureT>,
    ) = viewModelScope.launch {
        val newState = update(state.flowState)
        state = state.copy(
            flowState = newState,
            nextEnabled = newState !is NewLoginFlowState.Loading &&
                    newState !is NewLoginFlowState.MissingBackendConfig &&
                    newState !is NewLoginFlowState.LoadingBackendConfig &&
                    newState !is NewLoginFlowState.BackendConfigError &&
                    newState !is NewLoginFlowState.BackendConfigSuccess &&
                    userIdentifierTextState.text.isNotEmpty()
        )
    }

    private fun fetchDefaultSsoCodeIfNeeded(persistImmediately: Boolean) {
        viewModelScope.launch {
            when (val result = gateway.fetchDefaultSsoCode(serverConfig)) {
                is NewLoginDefaultSsoCodeResult.Success -> if (result.code != null && userIdentifierTextState.text.isEmpty()) {
                    userIdentifierTextState.setTextAndPlaceCursorAtEnd(result.code)
                    if (persistImmediately) savedStateStore.userIdentifier = result.code
                }
                is NewLoginDefaultSsoCodeResult.Failure -> Unit
            }
        }
    }

    private fun consumePendingNomadServiceUrl(): String? = pendingNomadServiceUrl.also { pendingNomadServiceUrl = null }
    private fun consumePendingCookieLabel(): String? = pendingCookieLabel.also { pendingCookieLabel = null }

    private fun NewLoginFailure<FailureT>.toFlowError(): NewLoginFlowState.Error.DialogError<FailureT, Nothing> = when (this) {
        is NewLoginFailure.Generic -> NewLoginFlowState.Error.DialogError.GenericError(failure)
        NewLoginFailure.ClientUpdateRequired -> NewLoginFlowState.Error.DialogError.ClientUpdateRequired
        NewLoginFailure.ServerVersionNotSupported -> NewLoginFlowState.Error.DialogError.ServerVersionNotSupported
    }

    companion object {
        const val SSO_LOGIN_RESULT_KEY = "sso_login_result_json"
    }
}

private data class PendingSsoSession<SessionT>(val session: SessionT, val isNomadSession: Boolean)
