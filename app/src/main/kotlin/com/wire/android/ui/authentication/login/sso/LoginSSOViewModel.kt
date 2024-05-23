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
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.LoginViewModel
import com.wire.android.ui.authentication.login.toLoginError
import com.wire.android.ui.common.dialogs.CustomServerDialogState
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.DomainLookupUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
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
class LoginSSOViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val validateEmailUseCase: ValidateEmailUseCase,
    @KaliumCoreLogic coreLogic: CoreLogic,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    authServerConfigProvider: AuthServerConfigProvider,
    userDataStoreProvider: UserDataStoreProvider
) : LoginViewModel(
    clientScopeProviderFactory,
    authServerConfigProvider,
    userDataStoreProvider,
    coreLogic
) {

    var openWebUrl = MutableSharedFlow<String>()

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
        updateSSOFlowState(LoginState.Default)
    }

    fun onCustomServerDialogConfirm() {
        viewModelScope.launch {
            loginState.customServerDialogState?.let {
                authServerConfigProvider.updateAuthServer(it.serverLinks)

                // sso does not support proxy
                // TODO: add proxy support
                val authScope = coreLogic.versionedAuthenticationScope(it.serverLinks)(null).let {
                    when (it) {
                        is AutoVersionAuthScopeUseCase.Result.Failure.Generic,
                        AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
                        AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                            updateSSOFlowState(LoginState.Default)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Success -> {
                            it.authenticationScope
                        }
                    }
                }

                authScope.ssoLoginScope.fetchSSOSettings().also {
                    when (it) {
                        is FetchSSOSettingsUseCase.Result.Failure -> {}
                        is FetchSSOSettingsUseCase.Result.Success -> {
                            it.defaultSSOCode?.let { ssoCode ->
                                val ssoCodeWithPrefix = if (ssoCode.startsWith("wire-")) ssoCode else "wire-$ssoCode"
                                authScope.ssoLoginScope.initiate(
                                    SSOInitiateLoginUseCase.Param.WithRedirect(ssoCodeWithPrefix)
                                ).let { result ->
                                    when (result) {
                                        is SSOInitiateLoginResult.Failure ->
                                            updateSSOFlowState(result.toLoginSSOError())
                                        is SSOInitiateLoginResult.Success -> openWebUrl(result.requestUrl)
                                    }
                                }
                            }
                        }
                    }
                }
                updateSSOFlowState(LoginState.Default)
            }
        }
    }

    @VisibleForTesting
    fun domainLookupFlow() {
        viewModelScope.launch {
            val defaultAuthScope: AuthenticationScope =
                coreLogic.versionedAuthenticationScope(
                    authServerConfigProvider.defaultServerLinks()
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
                        loginState = loginState.copy(customServerDialogState = CustomServerDialogState(it.serverLinks))
                        updateSSOFlowState(LoginState.Default)
                    }
                }
            }
        }
    }

    private fun ssoLoginWithCodeFlow() {
        viewModelScope.launch {
            val authScope =
                // sso does not support proxy
                coreLogic.versionedAuthenticationScope(serverConfig)(null).let {
                    when (it) {
                        is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                        is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                            updateSSOFlowState(LoginState.Error.DialogError.ServerVersionNotSupported)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                            updateSSOFlowState(LoginState.Error.DialogError.ClientUpdateRequired)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                            return@launch
                        }
                    }
                }

            authScope.ssoLoginScope.initiate(SSOInitiateLoginUseCase.Param.WithRedirect(ssoTextState.text.toString())).let { result ->
                when (result) {
                    is SSOInitiateLoginResult.Failure -> updateSSOFlowState(result.toLoginSSOError())
                    is SSOInitiateLoginResult.Success -> openWebUrl(result.requestUrl)
                }
            }
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    @VisibleForTesting
    fun establishSSOSession(
        cookie: String,
        serverConfigId: String
    ) {
        updateSSOFlowState(LoginState.Loading)
        viewModelScope.launch {
            val authScope =
                coreLogic.versionedAuthenticationScope(serverConfig)(null).let {
                    when (it) {
                        is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                        is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                            updateSSOFlowState(LoginState.Error.DialogError.ServerVersionNotSupported)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                            updateSSOFlowState(LoginState.Error.DialogError.ClientUpdateRequired)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                            return@launch
                        }
                    }
                }
            val ssoLoginResult = authScope.ssoLoginScope.getLoginSession(cookie).let {
                when (it) {
                    is SSOLoginSessionResult.Failure -> {
                        updateSSOFlowState(it.toLoginError())
                        return@launch
                    }

                    is SSOLoginSessionResult.Success -> it
                }
            }
            val storedUserId = addAuthenticatedUser(
                authTokens = ssoLoginResult.accountTokens,
                ssoId = ssoLoginResult.ssoId,
                serverConfigId = serverConfigId,
                proxyCredentials = ssoLoginResult.proxyCredentials,
                replace = false
            ).let {
                when (it) {
                    is AddAuthenticatedUserUseCase.Result.Failure -> {
                        updateSSOFlowState(it.toLoginError())
                        return@launch
                    }

                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            registerClient(storedUserId, null).let {
                when (it) {
                    is RegisterClientResult.Success -> {
                        updateSSOFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), false))
                    }

                    is RegisterClientResult.Failure -> {
                        updateSSOFlowState(it.toLoginError())
                        return@launch
                    }

                    is RegisterClientResult.E2EICertificateRequired -> {
                        updateSSOFlowState(LoginState.Success(isInitialSyncCompleted(storedUserId), true))
                    }
                }
            }
        }
    }

    fun handleSSOResult(ssoLoginResult: DeepLinkResult.SSOLogin?) =
        when (ssoLoginResult) {
            is DeepLinkResult.SSOLogin.Success -> {
                establishSSOSession(ssoLoginResult.cookie, ssoLoginResult.serverConfigId)
            }

            is DeepLinkResult.SSOLogin.Failure ->
                updateSSOFlowState(LoginState.Error.DialogError.SSOResultError(ssoLoginResult.ssoError))

            null -> {}
        }

    private fun openWebUrl(url: String) {
        viewModelScope.launch {
            updateSSOFlowState(LoginState.Default)
            openWebUrl.emit(url)
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
