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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.config.DefaultServerConfig
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.authentication.login.toLoginError
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialogState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.DomainLookupUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class LoginSSOViewModel(
    private val savedStateHandle: SavedStateHandle,
    val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    coreLogic: CoreLogic,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    userDataStoreProvider: UserDataStoreProvider,
    private val ssoExtension: LoginSSOViewModelExtension,
    serverConfig: ServerConfig.Links
) : LoginViewModel(
    savedStateHandle,
    clientScopeProviderFactory,
    userDataStoreProvider,
    coreLogic,
    serverConfig
) {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        addAuthenticatedUser: AddAuthenticatedUserUseCase,
        validateEmailUseCase: ValidateEmailUseCase,
        @KaliumCoreLogic coreLogic: CoreLogic,
        clientScopeProviderFactory: ClientScopeProvider.Factory,
        userDataStoreProvider: UserDataStoreProvider,
        serverConfig: ServerConfig.Links,
        @DefaultWebSocketEnabledByDefault defaultWebSocketEnabledByDefault: Boolean,
    ) : this(
        savedStateHandle,
        addAuthenticatedUser,
        validateEmailUseCase,
        coreLogic,
        clientScopeProviderFactory,
        userDataStoreProvider,
        LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic, defaultWebSocketEnabledByDefault),
        serverConfig
    )

    var openWebUrl = MutableSharedFlow<Pair<String, ServerConfig.Links>>()

    val ssoTextState: TextFieldState = TextFieldState()
    var loginState: LoginSSOState by mutableStateOf(LoginSSOState())

    init {
        ssoTextState.setTextAndPlaceCursorAtEnd(savedStateHandle[SSO_CODE_SAVED_STATE_KEY] ?: String.EMPTY)
        viewModelScope.launch {
            ssoTextState.textAsFlow().distinctUntilChanged().collectLatest {
                if (loginState.flowState != LoginState.Loading) {
                    updateSSOFlowState(LoginState.Default)
                }
                savedStateHandle[SSO_CODE_SAVED_STATE_KEY] = it.toString()
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
                                onAuthScopeFailure = { updateSSOFlowState(it.toLoginError()) },
                                onSSOInitiateFailure = { updateSSOFlowState(it.toLoginSSOError()) },
                                onSuccess = { requestUrl, _ -> openWebUrl(requestUrl, state.serverLinks) }
                            )
                        }
                    }
                )
            }
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
                onAuthScopeFailure = { updateSSOFlowState(it.toLoginError()) },
                onSSOInitiateFailure = { updateSSOFlowState(it.toLoginSSOError()) },
                onSuccess = { requestUrl, _ -> openWebUrl(requestUrl, serverConfig) }
            )
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    @VisibleForTesting
    fun establishSSOSession(
        cookie: String,
        serverConfigId: String,
        serverConfig: ServerConfig.Links? = null,
    ) {
        updateSSOFlowState(LoginState.Loading)
        viewModelScope.launch {
            ssoExtension.establishSSOSession(
                cookie = cookie,
                serverConfigId = serverConfigId,
                serverConfig = serverConfig ?: this@LoginSSOViewModel.serverConfig,
                onAuthScopeFailure = { updateSSOFlowState(it.toLoginError()) },
                onSSOLoginFailure = { updateSSOFlowState(it.toLoginError()) },
                onAddAuthenticatedUserFailure = { updateSSOFlowState(it.toLoginError()) },
                onSuccess = { storedUserId ->
                    registerClient(storedUserId, null).let {
                        when (it) {
                            is RegisterClientResult.Success ->
                                updateSSOFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), false))

                            is RegisterClientResult.Failure ->
                                updateSSOFlowState(it.toLoginError())

                            is RegisterClientResult.E2EICertificateRequired ->
                                updateSSOFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), true))
                        }
                    }
                }
            )
        }
    }

    fun handleSSOResult(ssoLoginResult: DeepLinkResult.SSOLogin?, serverConfig: ServerConfig.Links? = null) {
        when (ssoLoginResult) {
            is DeepLinkResult.SSOLogin.Success -> {
                establishSSOSession(ssoLoginResult.cookie, ssoLoginResult.serverConfigId, serverConfig)
            }

            is DeepLinkResult.SSOLogin.Failure ->
                updateSSOFlowState(LoginState.Error.DialogError.SSOResultError(ssoLoginResult.ssoError))

            null -> {}
        }
    }

    private fun openWebUrl(url: String, customServerConfig: ServerConfig.Links) {
        viewModelScope.launch {
            updateSSOFlowState(LoginState.Default)
            openWebUrl.emit(url to customServerConfig)
        }
    }

    companion object {
        const val SSO_CODE_SAVED_STATE_KEY = "sso_code"
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
