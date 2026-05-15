/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.ios.shared.auth.newlogin

import com.wire.ios.shared.IosKaliumRuntimeConfig
import com.wire.ios.shared.WireIosSharedConfig
import com.wire.ios.shared.auth.login.model.LoginApiProxy
import com.wire.ios.shared.auth.login.model.LoginServerLinks
import com.wire.kalium.logic.CoreLogicCommon
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.EnterpriseLoginResult
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginUseCase
import dev.zacsweers.metro.Inject

@Inject
class KaliumNewLoginIdentifierBackend(
    private val config: WireIosSharedConfig,
    private val coreLogic: CoreLogicCommon,
) : NewLoginIdentifierBackend {
    private val runtimeConfig: IosKaliumRuntimeConfig?
        get() = config.runtimeConfig

    private val localBackend = LocalNewLoginIdentifierBackend()

    override suspend fun resolveEmail(userIdentifier: String): NewLoginIdentifierBackendResult {
        val runtime = runtimeConfig ?: return localBackend.resolveEmail(userIdentifier)
        return when (val authScope = coreLogic.versionedAuthenticationScope(runtime.serverLinks.toKalium()).invoke(null)) {
            is AutoVersionAuthScopeUseCase.Result.Failure ->
                NewLoginIdentifierBackendResult.Error(authScope.toDialogError())

            is AutoVersionAuthScopeUseCase.Result.Success ->
                when (val result = authScope.authenticationScope.getLoginFlowForDomainUseCase(userIdentifier)) {
                    is EnterpriseLoginResult.Failure.Generic ->
                        NewLoginIdentifierBackendResult.Error(NewLoginIdentifierDialogError.GenericError(result.coreFailure.toString()))

                    EnterpriseLoginResult.Failure.NotSupported ->
                        NewLoginIdentifierBackendResult.EnterpriseLoginNotSupported(userIdentifier)

                    is EnterpriseLoginResult.Success ->
                        result.loginRedirectPath.toBackendResult(userIdentifier)
                }
        }
    }

    override suspend fun initiateSso(ssoCode: String): NewLoginIdentifierBackendResult {
        val runtime = runtimeConfig ?: return localBackend.initiateSso(ssoCode)
        return when (val authScope = coreLogic.versionedAuthenticationScope(runtime.serverLinks.toKalium()).invoke(null)) {
            is AutoVersionAuthScopeUseCase.Result.Failure ->
                NewLoginIdentifierBackendResult.Error(authScope.toDialogError())

            is AutoVersionAuthScopeUseCase.Result.Success ->
                when (
                    val result = authScope.authenticationScope.ssoLoginScope.initiate(
                        SSOInitiateLoginUseCase.Param.WithRedirect(ssoCode)
                    )
                ) {
                    SSOInitiateLoginResult.Failure.InvalidCode,
                    SSOInitiateLoginResult.Failure.InvalidCodeFormat ->
                        NewLoginIdentifierBackendResult.Error(NewLoginIdentifierDialogError.InvalidSSOCode)

                    SSOInitiateLoginResult.Failure.InvalidRedirect ->
                        NewLoginIdentifierBackendResult.Error(NewLoginIdentifierDialogError.GenericError("Invalid SSO redirect"))

                    is SSOInitiateLoginResult.Failure.Generic ->
                        NewLoginIdentifierBackendResult.Error(NewLoginIdentifierDialogError.GenericError(result.genericFailure.toString()))

                    is SSOInitiateLoginResult.Success ->
                        NewLoginIdentifierBackendResult.OpenSso(
                            url = result.requestUrl,
                            config = NewLoginSsoUrlConfig(userIdentifier = ssoCode),
                        )
                }
        }
    }
}

private fun AutoVersionAuthScopeUseCase.Result.Failure.toDialogError(): NewLoginIdentifierDialogError =
    when (this) {
        AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> NewLoginIdentifierDialogError.ClientUpdateRequired
        AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> NewLoginIdentifierDialogError.ServerVersionNotSupported
        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> NewLoginIdentifierDialogError.GenericError(genericFailure.toString())
    }

private fun LoginRedirectPath.toBackendResult(userIdentifier: String): NewLoginIdentifierBackendResult =
    when (this) {
        is LoginRedirectPath.CustomBackend ->
            NewLoginIdentifierBackendResult.OpenCustomConfig(
                userIdentifier = userIdentifier,
                serverLinks = serverLinks.toIos(),
            )

        LoginRedirectPath.Default,
        LoginRedirectPath.NoRegistration ->
            NewLoginIdentifierBackendResult.OpenEmailPassword(
                userIdentifier = userIdentifier,
                path = NewLoginPasswordPath(
                    isCloudAccountCreationPossible = isCloudAccountCreationPossible,
                ),
            )

        is LoginRedirectPath.ExistingAccountWithClaimedDomain ->
            NewLoginIdentifierBackendResult.OpenEmailPassword(
                userIdentifier = userIdentifier,
                path = NewLoginPasswordPath(
                    isCloudAccountCreationPossible = isCloudAccountCreationPossible,
                    domainClaimedByOrg = NewLoginDomainClaimedByOrg.Claimed(domain),
                ),
            )

        is LoginRedirectPath.SSO ->
            NewLoginIdentifierBackendResult.OpenSso(
                url = ssoCode.withWireSsoPrefix(),
                config = NewLoginSsoUrlConfig(userIdentifier = userIdentifier),
            )
    }

private fun LoginServerLinks.toKalium(): ServerConfig.Links =
    ServerConfig.Links(
        api = api,
        accounts = accounts,
        webSocket = webSocket,
        blackList = blackList,
        teams = teams,
        website = website,
        title = title,
        isOnPremises = isOnPremises,
        apiProxy = apiProxy?.let {
            ServerConfig.ApiProxy(
                needsAuthentication = it.needsAuthentication,
                host = it.host,
                port = it.port,
            )
        },
    )

private fun ServerConfig.Links.toIos(): LoginServerLinks =
    LoginServerLinks(
        api = api,
        accounts = accounts,
        webSocket = webSocket,
        blackList = blackList,
        teams = teams,
        website = website,
        title = title,
        isOnPremises = isOnPremises,
        apiProxy = apiProxy?.let {
            LoginApiProxy(
                needsAuthentication = it.needsAuthentication,
                host = it.host,
                port = it.port,
            )
        },
    )

private fun String.withWireSsoPrefix(): String =
    if (startsWith(SSO_CODE_PREFIX)) this else "$SSO_CODE_PREFIX$this"

private const val SSO_CODE_PREFIX = "wire-"
