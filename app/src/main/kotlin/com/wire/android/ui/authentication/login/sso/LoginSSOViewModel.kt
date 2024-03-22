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
import com.wire.android.ui.authentication.login.updateSSOLoginEnabled
import com.wire.android.ui.common.dialogs.CustomServerDialogState
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
    savedStateHandle,
    clientScopeProviderFactory,
    authServerConfigProvider,
    userDataStoreProvider,
    coreLogic
) {

    var openWebUrl = MutableSharedFlow<String>()

    fun login() {
        loginState = loginState.copy(ssoLoginLoading = true, loginError = LoginError.None).updateSSOLoginEnabled()

        loginState.userInput.text.also {
            if (validateEmailUseCase(it)) {
                domainLookupFlow()
            } else {
                ssoLoginWithCodeFlow()
            }
        }
    }

    fun onCustomServerDialogDismiss() {
        loginState = loginState.copy(customServerDialogState = null)
    }

    fun onCustomServerDialogConfirm() {
        viewModelScope.launch {
            if (loginState.customServerDialogState != null) {
                authServerConfigProvider.updateAuthServer(loginState.customServerDialogState!!.serverLinks)

                // sso does not support proxy
                // TODO: add proxy support
                val authScope = coreLogic.versionedAuthenticationScope(loginState.customServerDialogState!!.serverLinks)(null).let {
                    when (it) {
                        is AutoVersionAuthScopeUseCase.Result.Failure.Generic,
                        AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
                        AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                            loginState = loginState.copy(customServerDialogState = null)
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
                                        is SSOInitiateLoginResult.Failure -> updateSSOLoginError(result.toLoginSSOError())
                                        is SSOInitiateLoginResult.Success -> openWebUrl(result.requestUrl)
                                    }
                                }
                            }
                        }
                    }
                }
                loginState = loginState.copy(customServerDialogState = null)
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
                            loginState = loginState.copy(loginError = LoginError.DialogError.ServerVersionNotSupported)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope
                    }
                }

            defaultAuthScope.domainLookup(loginState.userInput.text).also {
                when (it) {
                    is DomainLookupUseCase.Result.Failure -> {
                        loginState = loginState.copy(ssoLoginLoading = false, loginError = it.toLoginError())
                    }

                    is DomainLookupUseCase.Result.Success -> {
                        loginState = loginState.copy(
                            ssoLoginLoading = false,
                            loginError = LoginError.None,
                            customServerDialogState = CustomServerDialogState(it.serverLinks)
                        )
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
                            loginState = loginState.copy(loginError = LoginError.DialogError.ServerVersionNotSupported)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                            loginState = loginState.copy(loginError = LoginError.DialogError.ClientUpdateRequired)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> {
                            return@launch
                        }
                    }
                }

            authScope.ssoLoginScope.initiate(SSOInitiateLoginUseCase.Param.WithRedirect(loginState.userInput.text)).let { result ->
                when (result) {
                    is SSOInitiateLoginResult.Failure -> updateSSOLoginError(result.toLoginSSOError())
                    is SSOInitiateLoginResult.Success -> openWebUrl(result.requestUrl)
                }
            }
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    @VisibleForTesting
    fun establishSSOSession(
        cookie: String,
        serverConfigId: String,
        onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit
    ) {
        loginState = loginState.copy(ssoLoginLoading = true, loginError = LoginError.None).updateSSOLoginEnabled()
        viewModelScope.launch {
            val authScope =
                coreLogic.versionedAuthenticationScope(serverConfig)(null).let {
                    when (it) {
                        is AutoVersionAuthScopeUseCase.Result.Success -> it.authenticationScope

                        is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> {
                            loginState = loginState.copy(loginError = LoginError.DialogError.ServerVersionNotSupported)
                            return@launch
                        }

                        is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> {
                            loginState = loginState.copy(loginError = LoginError.DialogError.ClientUpdateRequired)
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
                        updateSSOLoginError(it.toLoginError())
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
                        updateSSOLoginError(it.toLoginError())
                        return@launch
                    }

                    is AddAuthenticatedUserUseCase.Result.Success -> it.userId
                }
            }
            registerClient(storedUserId, null).let {
                when (it) {
                    is RegisterClientResult.Success -> {
                        onSuccess(isInitialSyncCompleted(storedUserId), false)
                    }

                    is RegisterClientResult.Failure -> {
                        updateSSOLoginError(it.toLoginError())
                        return@launch
                    }

                    is RegisterClientResult.E2EICertificateRequired -> {
                        onSuccess(isInitialSyncCompleted(storedUserId), true)
                    }
                }
            }
        }
    }

    fun onSSOCodeChange(newText: TextFieldValue) {
        // in case an error is showing e.g. inline error is should be cleared
        if (loginState.loginError is LoginError.TextFieldError && newText != loginState.userInput) {
            clearSSOLoginError()
        }
        loginState = loginState.copy(userInput = newText).updateSSOLoginEnabled()
        savedStateHandle.set(SSO_CODE_SAVED_STATE_KEY, newText.text)
    }

    fun handleSSOResult(
        ssoLoginResult: DeepLinkResult.SSOLogin?,
        onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit
    ) =
        when (ssoLoginResult) {
            is DeepLinkResult.SSOLogin.Success -> {
                establishSSOSession(ssoLoginResult.cookie, ssoLoginResult.serverConfigId, onSuccess)
            }

            is DeepLinkResult.SSOLogin.Failure -> updateSSOLoginError(LoginError.DialogError.SSOResultError(ssoLoginResult.ssoError))
            null -> {}
        }

    private fun openWebUrl(url: String) {
        viewModelScope.launch {
            loginState = loginState.copy(ssoLoginLoading = false, loginError = LoginError.None).updateSSOLoginEnabled()
            openWebUrl.emit(url)
        }
    }
}

private fun SSOInitiateLoginResult.Failure.toLoginSSOError() = when (this) {
    SSOInitiateLoginResult.Failure.InvalidCodeFormat -> LoginError.TextFieldError.InvalidValue
    SSOInitiateLoginResult.Failure.InvalidCode -> LoginError.DialogError.InvalidSSOCodeError
    is SSOInitiateLoginResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
    SSOInitiateLoginResult.Failure.InvalidRedirect ->
        LoginError.DialogError.GenericError(CoreFailure.Unknown(IllegalArgumentException("Invalid Redirect")))
}

private fun SSOLoginSessionResult.Failure.toLoginError() = when (this) {
    SSOLoginSessionResult.Failure.InvalidCookie -> LoginError.DialogError.InvalidSSOCookie
    is SSOLoginSessionResult.Failure.Generic -> LoginError.DialogError.GenericError(this.genericFailure)
}
