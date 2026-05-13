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

package com.wire.android.ui.authentication.login.sso

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.config.DefaultServerConfig
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginSavedInputStore
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.toLoginError
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialogState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.DomainLookupUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.backup.RestoreCryptoStateResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel(assistedFactory = LoginSSOViewModel.Factory::class)
class LoginSSOViewModel : LoginViewModel {
    private val savedInputStore: LoginSavedInputStore
    val addAuthenticatedUser: AddAuthenticatedUserUseCase
    private val validateEmailUseCase: ValidateEmailUseCase
    private val ssoExtension: LoginSSOViewModelExtension
    private val sessionExceptionClassifier: LoginSSOSessionExceptionClassifier
    private val dispatchers: DispatcherProvider

    private var pendingNomadServiceUrl: String? = null
    private var pendingCookieLabel: String? = null

    constructor(
        loginNavArgs: LoginNavArgs,
        savedInputStore: LoginSavedInputStore,
        addAuthenticatedUser: AddAuthenticatedUserUseCase,
        validateEmailUseCase: ValidateEmailUseCase,
        coreLogic: CoreLogic,
        clientScopeProviderFactory: ClientScopeProvider.Factory,
        userDataStoreProvider: UserDataStoreProvider,
        serverConfig: ServerConfig.Links,
        ssoExtension: LoginSSOViewModelExtension,
        sessionExceptionClassifier: LoginSSOSessionExceptionClassifier,
        dispatchers: DispatcherProvider,
    ) : this(
        loginNavArgs,
        savedInputStore,
        addAuthenticatedUser,
        validateEmailUseCase,
        coreLogic,
        clientScopeProviderFactory,
        userDataStoreProvider,
        ssoExtension,
        serverConfig,
        sessionExceptionClassifier,
        dispatchers,
    )

    @AssistedInject
    constructor(
        @Assisted loginNavArgs: LoginNavArgs,
        savedInputStore: LoginSavedInputStore,
        addAuthenticatedUser: AddAuthenticatedUserUseCase,
        validateEmailUseCase: ValidateEmailUseCase,
        @KaliumCoreLogic coreLogic: CoreLogic,
        clientScopeProviderFactory: ClientScopeProvider.Factory,
        userDataStoreProvider: UserDataStoreProvider,
        serverConfig: ServerConfig.Links,
        @DefaultWebSocketEnabledByDefault defaultWebSocketEnabledByDefault: Boolean,
        sessionExceptionClassifier: LoginSSOSessionExceptionClassifier,
        dispatchers: DispatcherProvider,
    ) : this(
        loginNavArgs,
        savedInputStore,
        addAuthenticatedUser,
        validateEmailUseCase,
        coreLogic,
        clientScopeProviderFactory,
        userDataStoreProvider,
        serverConfig,
        LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic, defaultWebSocketEnabledByDefault),
        sessionExceptionClassifier,
        dispatchers,
    )

    private constructor(
        loginNavArgs: LoginNavArgs,
        savedInputStore: LoginSavedInputStore,
        addAuthenticatedUser: AddAuthenticatedUserUseCase,
        validateEmailUseCase: ValidateEmailUseCase,
        coreLogic: CoreLogic,
        clientScopeProviderFactory: ClientScopeProvider.Factory,
        userDataStoreProvider: UserDataStoreProvider,
        ssoExtension: LoginSSOViewModelExtension,
        serverConfig: ServerConfig.Links,
        sessionExceptionClassifier: LoginSSOSessionExceptionClassifier,
        dispatchers: DispatcherProvider,
    ) : super(
        loginNavArgs,
        clientScopeProviderFactory,
        userDataStoreProvider,
        coreLogic,
        LoginViewModelExtension(clientScopeProviderFactory, userDataStoreProvider),
        serverConfig
    ) {
        this.savedInputStore = savedInputStore
        this.addAuthenticatedUser = addAuthenticatedUser
        this.validateEmailUseCase = validateEmailUseCase
        this.ssoExtension = ssoExtension
        this.sessionExceptionClassifier = sessionExceptionClassifier
        this.dispatchers = dispatchers
        observeSSOCodeInput()
    }

    var openWebUrl = MutableSharedFlow<Pair<String, ServerConfig.Links>>()

    val ssoTextState: TextFieldState = TextFieldState()
    var loginState: LoginSSOState by mutableStateOf(LoginSSOState())

    @AssistedFactory
    interface Factory {
        fun create(args: LoginNavArgs): LoginSSOViewModel
    }

    private fun observeSSOCodeInput() {
        ssoTextState.setTextAndPlaceCursorAtEnd(savedInputStore.ssoCode ?: String.EMPTY)
        viewModelScope.launch {
            ssoTextState.textAsFlow().distinctUntilChanged().collectLatest {
                if (loginState.flowState != LoginState.Loading) {
                    updateSSOFlowState(LoginState.Default)
                }
                savedInputStore.ssoCode = it.toString()
            }
        }
    }

    private fun updateSSOFlowState(flowState: LoginState) {
        loginState = loginState.copy(
            flowState = flowState,
            loginEnabled = ssoTextState.text.isNotEmpty() && flowState !is LoginState.Loading
        )
    }

    fun clearLoginErrors() {
        updateSSOFlowState(LoginState.Default)
    }

    fun login() {
        updateSSOFlowState(LoginState.Loading)
        ssoTextState.text.toString().also {
            if (validateEmailUseCase(it)) {
                domainLookupFlow()
            } else {
                ssoLoginWithCodeFlow()
            }
        }
    }

    fun onCustomServerDialogDismiss() {
        loginState = loginState.copy(customServerDialogState = null)
        updateSSOFlowState(LoginState.Default)
    }

    fun onCustomServerDialogConfirm() {
        viewModelScope.launch {
            loginState.customServerDialogState?.let { state ->
                // sso does not support proxy
                // TODO: add proxy support
                ssoExtension.fetchDefaultSSOCode(
                    serverConfig = state.serverLinks,
                    onAuthScopeFailure = { updateSSOFlowState(it.toLoginError()) },
                    onFetchSSOSettingsFailure = {},
                    onSuccess = { defaultSSOCode ->
                        if (defaultSSOCode != null) {
                            ssoExtension.initiateSSO(
                                serverConfig = state.serverLinks,
                                ssoCode = defaultSSOCode,
                                cookieLabel = pendingCookieLabel,
                                onAuthScopeFailure = { updateSSOFlowState(it.toLoginError()) },
                                onSSOInitiateFailure = { updateSSOFlowState(it.toLoginSSOError()) },
                                onSuccess = { requestUrl -> openWebUrl(requestUrl, state.serverLinks) }
                            )
                        }
                    }
                )
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

        if (autoInitiateLogin) {
            login()
        }
    }

    @VisibleForTesting
    fun domainLookupFlow() {
        viewModelScope.launch {
            val defaultAuthScope: AuthenticationScope =
                coreLogic.versionedAuthenticationScope(
                    DefaultServerConfig
                    // domain lockup does not support proxy
                    // TODO: add proxy support
                )(null).let {
                    when (it) {
                        is AutoVersionAuthScopeUseCase.Result.Failure.Generic,
                        AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
                        AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                            updateSSOFlowState(LoginState.Error.DialogError.ServerVersionNotSupported)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope
                    }
                }

            defaultAuthScope.domainLookup(ssoTextState.text.toString()).also {
                when (it) {
                    is DomainLookupUseCase.Result.Failure -> {
                        updateSSOFlowState(it.toLoginError())
                    }

                    is DomainLookupUseCase.Result.Success -> {
                        loginState = loginState.copy(customServerDialogState = CustomServerDetailsDialogState(it.serverLinks))
                        updateSSOFlowState(LoginState.Default)
                    }
                }
            }
        }
    }

    private fun ssoLoginWithCodeFlow() {
        viewModelScope.launch {
            ssoExtension.initiateSSO(
                serverConfig = serverConfig,
                ssoCode = ssoTextState.text.toString(),
                cookieLabel = pendingCookieLabel,
                onAuthScopeFailure = { updateSSOFlowState(it.toLoginError()) },
                onSSOInitiateFailure = { updateSSOFlowState(it.toLoginSSOError()) },
                onSuccess = { requestUrl -> openWebUrl(requestUrl, serverConfig) }
            )
        }
    }

    @VisibleForTesting
    fun establishSSOSession(
        cookie: String,
        serverConfigId: String,
    ) {
        updateSSOFlowState(LoginState.Loading)
        val isNomadFlow = pendingNomadServiceUrl != null
        viewModelScope.launch {
            ssoExtension.establishSSOSession(
                cookie = cookie,
                serverConfigId = serverConfigId,
                consumeNomadServiceUrl = ::consumePendingNomadServiceUrl,
                consumeCookieLabel = ::consumePendingCookieLabel,
                onAuthScopeFailure = { updateSSOFlowState(it.toLoginError()) },
                onSSOLoginFailure = { updateSSOFlowState(it.toLoginError()) },
                onAddAuthenticatedUserFailure = { updateSSOFlowState(it.toLoginError()) },
                onSuccess = { storedUserId ->
                    if (!isNomadFlow) {
                        appLogger.i("$TAG Not a nomad flow, proceeding with regular login")
                        registerClientAndUpdateState(storedUserId, setLastDeviceId = false)
                    } else {
                        appLogger.i("$TAG Nomad flow, attempting crypto state restore")
                        restoreCryptoStateAndContinue(storedUserId)
                    }
                }
            )
        }
    }

    fun handleSSOResult(
        ssoLoginResult: DeepLinkResult.SSOLogin?,
    ) {
        when (ssoLoginResult) {
            is DeepLinkResult.SSOLogin.Success -> {
                establishSSOSession(
                    ssoLoginResult.cookie,
                    ssoLoginResult.serverConfigId,
                )
            }

            is DeepLinkResult.SSOLogin.Failure ->
                updateSSOFlowState(LoginState.Error.DialogError.SSOResultError(ssoLoginResult.ssoError))

            null -> {}
        }
    }

    private suspend fun registerClientAndUpdateState(userId: UserId, setLastDeviceId: Boolean = false) {
        withContext(dispatchers.io()) {
            registerClient(userId = userId, password = null)
        }.let {
            when (it) {
                is RegisterClientResult.Success -> {
                    if (setLastDeviceId) {
                        coreLogic.getSessionScope(userId).backup.setLastDeviceId(it.client.id.value)
                    }
                    updateSSOFlowState(LoginState.Success(isInitialSyncCompleted(userId), false))
                }

                is RegisterClientResult.E2EICertificateRequired ->
                    updateSSOFlowState(LoginState.Success(isInitialSyncCompleted(userId), true))

                is RegisterClientResult.Failure.TooManyClients ->
                    updateSSOFlowState(LoginState.Error.TooManyDevicesError)

                is RegisterClientResult.Failure -> {
                    revertSSOSession(userId)
                    updateSSOFlowState(it.toLoginError())
                }
            }
        }
    }

    private suspend fun isSessionStillValid(userId: UserId): Boolean =
        (coreLogic.getGlobalScope().doesValidSessionExist(userId) as? DoesValidSessionExistResult.Success)
            ?.doesValidSessionExist == true

    @Suppress("ThrowsCount", "TooGenericExceptionCaught")
    private suspend fun restoreCryptoStateAndContinue(storedUserId: UserId) {
        val restoreResult = try {
            withContext(dispatchers.io()) {
                coreLogic.getSessionScope(storedUserId).backup.restoreCryptoState()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (sessionExceptionClassifier.isRecoverableSessionException(e)) {
                if (isSessionStillValid(storedUserId)) throw e
                appLogger.w("$TAG Crypto restore interrupted by concurrent logout: ${e.message}")
                return
            }
            throw e
        }

        when (restoreResult) {
            is RestoreCryptoStateResult.Success -> {
                updateSSOFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), false))
            }
            is RestoreCryptoStateResult.NoBackupAvailable -> {
                registerClientAndUpdateState(storedUserId, setLastDeviceId = true)
            }
            is RestoreCryptoStateResult.Failure -> {
                appLogger.e("$TAG Failed to restore crypto state during SSO login")
                revertSSOSession(storedUserId)
                updateSSOFlowState(
                    LoginState.Error.DialogError.GenericError(
                        CoreFailure.Unknown(Exception("Failed to restore crypto state"))
                    )
                )
            }
        }
    }

    @Suppress("ThrowsCount", "TooGenericExceptionCaught")
    private suspend fun revertSSOSession(userId: UserId) {
        try {
            coreLogic.getSessionScope(userId).logout(reason = LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
            coreLogic.getGlobalScope().deleteSession(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (sessionExceptionClassifier.isRecoverableSessionException(e)) {
                if (isSessionStillValid(userId)) throw e
                appLogger.w("$TAG Failed to revert SSO session, may have been already logged out: ${e.message}")
            } else {
                throw e
            }
        }
    }

    private fun openWebUrl(url: String, customServerConfig: ServerConfig.Links) {
        viewModelScope.launch {
            updateSSOFlowState(LoginState.Default)
            openWebUrl.emit(url to customServerConfig)
        }
    }

    companion object {
        private const val TAG = "[LoginSSOViewModel]"
    }

    private fun consumePendingNomadServiceUrl(): String? = pendingNomadServiceUrl.also {
        pendingNomadServiceUrl = null
    }

    private fun consumePendingCookieLabel(): String? = pendingCookieLabel.also {
        pendingCookieLabel = null
    }
}

private fun SSOInitiateLoginResult.Failure.toLoginSSOError() = when (this) {
    SSOInitiateLoginResult.Failure.InvalidCodeFormat -> LoginState.Error.TextFieldError.InvalidValue
    SSOInitiateLoginResult.Failure.InvalidCode -> LoginState.Error.DialogError.InvalidSSOCodeError
    is SSOInitiateLoginResult.Failure.Generic -> LoginState.Error.DialogError.GenericError(this.genericFailure)
    SSOInitiateLoginResult.Failure.InvalidRedirect ->
        LoginState.Error.DialogError.GenericError(CoreFailure.Unknown(IllegalArgumentException("Invalid Redirect")))
}

private fun SSOLoginSessionResult.Failure.toLoginError() = when (this) {
    SSOLoginSessionResult.Failure.InvalidCookie -> LoginState.Error.DialogError.InvalidSSOCookie
    is SSOLoginSessionResult.Failure.Generic -> LoginState.Error.DialogError.GenericError(this.genericFailure)
}

private fun AutoVersionAuthScopeUseCase.Result.Failure.toLoginError() = when (this) {
    is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> LoginState.Error.DialogError.GenericError(this.genericFailure)
    AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> LoginState.Error.DialogError.ClientUpdateRequired
    AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> LoginState.Error.DialogError.ServerVersionNotSupported
}
