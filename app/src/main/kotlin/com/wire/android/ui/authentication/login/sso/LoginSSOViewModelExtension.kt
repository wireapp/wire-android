/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import com.wire.android.appLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase.Companion.SSO_CODE_WIRE_PREFIX

class LoginSSOViewModelExtension(
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val coreLogic: CoreLogic,
    private val defaultWebSocketEnabledByDefault: Boolean,
) {
    suspend fun withAuthenticationScope(
        serverConfig: ServerConfig.Links,
        onAuthScopeFailure: (AutoVersionAuthScopeUseCase.Result.Failure) -> Unit,
        onSuccess: suspend (AuthenticationScope) -> Unit
    ) {
        coreLogic.versionedAuthenticationScope(serverConfig).invoke(null).let { // sso does not support proxy
            when (it) {
                is AutoVersionAuthScopeUseCase.Result.Success -> onSuccess(it.authenticationScope)
                is AutoVersionAuthScopeUseCase.Result.Failure -> onAuthScopeFailure(it)
            }
        }
    }

    suspend fun initiateSSO(
        serverConfig: ServerConfig.Links,
        ssoCode: String,
        onAuthScopeFailure: (AutoVersionAuthScopeUseCase.Result.Failure) -> Unit,
        onSSOInitiateFailure: (SSOInitiateLoginResult.Failure) -> Unit,
        onSuccess: suspend (redirectUrl: String) -> Unit,
    ) {
        withAuthenticationScope(serverConfig, onAuthScopeFailure) { authScope ->
            authScope.ssoLoginScope.initiate(SSOInitiateLoginUseCase.Param.WithRedirect(ssoCode)).let { result ->
                when (result) {
                    is SSOInitiateLoginResult.Failure -> onSSOInitiateFailure(result)
                    is SSOInitiateLoginResult.Success -> onSuccess(result.requestUrl)
                }
            }
        }
    }

    suspend fun fetchDefaultSSOCode(
        serverConfig: ServerConfig.Links,
        onAuthScopeFailure: (AutoVersionAuthScopeUseCase.Result.Failure) -> Unit,
        onFetchSSOSettingsFailure: (FetchSSOSettingsUseCase.Result.Failure) -> Unit,
        onSuccess: suspend (String?) -> Unit,
    ) {
        withAuthenticationScope(serverConfig, onAuthScopeFailure) { authScope ->
            authScope.ssoLoginScope.fetchSSOSettings().also {
                when (it) {
                    is FetchSSOSettingsUseCase.Result.Failure -> onFetchSSOSettingsFailure(it)
                    is FetchSSOSettingsUseCase.Result.Success -> onSuccess(it.defaultSSOCode?.ssoCodeWithPrefix())
                }
            }
        }
    }

    @Suppress("LongParameterList")
    suspend fun establishSSOSession(
        cookie: String,
        serverConfigId: String,
        consumeNomadServiceUrl: () -> String? = { null },
        onAuthScopeFailure: (AutoVersionAuthScopeUseCase.Result.Failure) -> Unit,
        onSSOLoginFailure: (SSOLoginSessionResult.Failure) -> Unit,
        onAddAuthenticatedUserFailure: (AddAuthenticatedUserUseCase.Result.Failure) -> Unit,
        onSuccess: suspend (UserId) -> Unit,
    ) {
        val authScope = when (val result = coreLogic.authenticationScopeForConfigId(serverConfigId)) {
            is AutoVersionAuthScopeUseCase.Result.Success -> {
                appLogger.i("SSO: Resolved auth scope from serverConfigId=$serverConfigId")
                result.authenticationScope
            }
            is AutoVersionAuthScopeUseCase.Result.Failure -> {
                appLogger.e("SSO: Failed to resolve auth scope for serverConfigId=$serverConfigId")
                onAuthScopeFailure(result)
                return
            }
        }

        val ssoLoginSuccess = when (val ssoLoginResult = authScope.ssoLoginScope.getLoginSession(cookie)) {
            is SSOLoginSessionResult.Failure -> {
                onSSOLoginFailure(ssoLoginResult)
                return
            }
            is SSOLoginSessionResult.Success -> ssoLoginResult
        }

        val authenticatedUserResult = addAuthenticatedUser(
            StoreSessionParam(
                accountTokens = ssoLoginSuccess.accountTokens,
                ssoId = ssoLoginSuccess.ssoId,
                serverConfigId = serverConfigId,
                proxyCredentials = ssoLoginSuccess.proxyCredentials,
                managedBy = ssoLoginSuccess.managedBy,
                isPersistentWebSocketEnabled = defaultWebSocketEnabledByDefault,
                nomadServiceUrl = consumeNomadServiceUrl(),
            ),
            replace = false
        )

        when (authenticatedUserResult) {
            is AddAuthenticatedUserUseCase.Result.Failure -> onAddAuthenticatedUserFailure(authenticatedUserResult)
            is AddAuthenticatedUserUseCase.Result.Success -> onSuccess(authenticatedUserResult.userId)
        }
    }
}

fun String.ssoCodeWithPrefix() = if (this.startsWith(SSO_CODE_WIRE_PREFIX)) this else "$SSO_CODE_WIRE_PREFIX$this"
