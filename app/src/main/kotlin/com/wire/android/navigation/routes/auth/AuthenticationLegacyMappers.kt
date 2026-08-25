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

@file:Suppress("TooManyFunctions")

package com.wire.android.navigation.routes.auth

import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.PreFilledUserIdentifierType
import com.wire.android.ui.authentication.login.SSOCodeAutoLogin
import com.wire.android.ui.authentication.welcome.WelcomeNavArgs
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.navigation.WireNavEntryId

internal fun LoginNavArgs.toAuthenticationArguments(): AuthenticationLoginArguments =
    AuthenticationLoginArguments(
        userHandle = userHandle?.let {
            AuthenticationPrefilledUserIdentifier(
                value = it.userIdentifier,
                editable = it.editable,
            )
        },
        ssoLoginResult = ssoLoginResult?.toAuthenticationSsoLoginResult(),
        loginPasswordPath = loginPasswordPath?.toAuthenticationLoginPasswordPath(),
        ssoCodeAutoLogin = ssoCodeAutoLogin?.let {
            AuthenticationSsoCodeAutoLogin(
                ssoCode = it.ssoCode,
                autoInitiateLogin = it.autoInitiateLogin,
                nomadServiceUrl = it.nomadServiceUrl,
                cookieLabel = it.cookieLabel,
            )
        },
        showBackendConfigSuccess = showBackendConfigSuccess,
    )

internal fun AuthenticationLoginArguments.toLegacy(): LoginNavArgs =
    LoginNavArgs(
        userHandle = userHandle?.let {
            PreFilledUserIdentifierType.PreFilled(
                userIdentifier = it.value,
                editable = it.editable,
            )
        },
        ssoLoginResult = ssoLoginResult?.toLegacy(),
        loginPasswordPath = loginPasswordPath?.toLegacy(),
        ssoCodeAutoLogin = ssoCodeAutoLogin?.let {
            SSOCodeAutoLogin(
                ssoCode = it.ssoCode,
                autoInitiateLogin = it.autoInitiateLogin,
                nomadServiceUrl = it.nomadServiceUrl,
                cookieLabel = it.cookieLabel,
            )
        },
        showBackendConfigSuccess = showBackendConfigSuccess,
    )

internal fun LoginNavArgs.toLoginRoute(
    entryId: WireNavEntryId = WireNavEntryId.random(),
    flowId: String = entryId.value,
): LoginRoute = LoginRoute(toAuthenticationArguments(), entryId, flowId)

internal fun LoginRoute.toLegacyNavArgs(): LoginNavArgs = args.toLegacy()

internal fun LoginNavArgs.toNewLoginRoute(
    flowId: String,
    entryId: WireNavEntryId = WireNavEntryId.random(),
): NewLoginRoute = NewLoginRoute(toAuthenticationArguments(), flowId, entryId)

internal fun NewLoginRoute.toLegacyNavArgs(): LoginNavArgs = args.toLegacy()

/**
 * Starts an isolated password attempt within the wider authentication flow.
 *
 * [LoginEmailViewModel] is shared with the verification-code entry, so both entries need a Flow
 * owner. That owner must not be the parent login flow, however: after cancellation it retains a
 * terminal [com.wire.android.ui.authentication.login.LoginState.Canceled] state and would
 * immediately cancel the next password attempt. Tying the attempt flow to its first entry gives a
 * retry a fresh owner while password -> verification continues to share one ViewModel.
 */
internal fun LoginNavArgs.toNewLoginPasswordAttemptRoute(
    entryId: WireNavEntryId = WireNavEntryId.random(),
): NewLoginPasswordRoute = NewLoginPasswordRoute(
    args = toAuthenticationArguments(),
    flowId = "new-login-password:${entryId.value}",
    entryId = entryId,
)

internal fun NewLoginPasswordRoute.toLegacyNavArgs(): LoginNavArgs = args.toLegacy()

internal fun LoginNavArgs.toNewLoginVerificationCodeRoute(
    flowId: String,
    entryId: WireNavEntryId = WireNavEntryId.random(),
): NewLoginVerificationCodeRoute =
    NewLoginVerificationCodeRoute(toAuthenticationArguments(), flowId, entryId)

internal fun NewLoginVerificationCodeRoute.toLegacyNavArgs(): LoginNavArgs = args.toLegacy()

internal fun WelcomeRoute.toLegacyNavArgs(): WelcomeNavArgs =
    WelcomeNavArgs(customServerConfig?.toLegacy())

internal fun DeepLinkResult.SSOLogin.toAuthenticationSsoLoginResult(): AuthenticationSsoLoginResult =
    when (this) {
        is DeepLinkResult.SSOLogin.Success -> AuthenticationSsoLoginResult.Success(
            cookie = cookie,
            serverConfigId = serverConfigId,
        )
        is DeepLinkResult.SSOLogin.Failure -> AuthenticationSsoLoginResult.Failure(
            code = ssoError.toAuthenticationSsoFailureCode(),
        )
    }

private fun AuthenticationSsoLoginResult.toLegacy(): DeepLinkResult.SSOLogin =
    when (this) {
        is AuthenticationSsoLoginResult.Success -> DeepLinkResult.SSOLogin.Success(
            cookie = cookie,
            serverConfigId = serverConfigId,
        )
        is AuthenticationSsoLoginResult.Failure -> DeepLinkResult.SSOLogin.Failure(code.toLegacy())
    }

private fun SSOFailureCodes.toAuthenticationSsoFailureCode(): AuthenticationSsoFailureCode =
    when (this) {
        SSOFailureCodes.ServerErrorUnsupportedSaml -> AuthenticationSsoFailureCode.SERVER_ERROR_UNSUPPORTED_SAML
        SSOFailureCodes.BadSuccessRedirect -> AuthenticationSsoFailureCode.BAD_SUCCESS_REDIRECT
        SSOFailureCodes.BadFailureRedirect -> AuthenticationSsoFailureCode.BAD_FAILURE_REDIRECT
        SSOFailureCodes.BadUsername -> AuthenticationSsoFailureCode.BAD_USERNAME
        SSOFailureCodes.BadUpstream -> AuthenticationSsoFailureCode.BAD_UPSTREAM
        SSOFailureCodes.ServerError -> AuthenticationSsoFailureCode.SERVER_ERROR
        SSOFailureCodes.NotFound -> AuthenticationSsoFailureCode.NOT_FOUND
        SSOFailureCodes.Forbidden -> AuthenticationSsoFailureCode.FORBIDDEN
        SSOFailureCodes.NoMatchingAuthReq -> AuthenticationSsoFailureCode.NO_MATCHING_AUTH_REQUEST
        SSOFailureCodes.InsufficientPermissions -> AuthenticationSsoFailureCode.INSUFFICIENT_PERMISSIONS
        SSOFailureCodes.Unknown -> AuthenticationSsoFailureCode.UNKNOWN
    }

private fun AuthenticationSsoFailureCode.toLegacy(): SSOFailureCodes =
    when (this) {
        AuthenticationSsoFailureCode.SERVER_ERROR_UNSUPPORTED_SAML -> SSOFailureCodes.ServerErrorUnsupportedSaml
        AuthenticationSsoFailureCode.BAD_SUCCESS_REDIRECT -> SSOFailureCodes.BadSuccessRedirect
        AuthenticationSsoFailureCode.BAD_FAILURE_REDIRECT -> SSOFailureCodes.BadFailureRedirect
        AuthenticationSsoFailureCode.BAD_USERNAME -> SSOFailureCodes.BadUsername
        AuthenticationSsoFailureCode.BAD_UPSTREAM -> SSOFailureCodes.BadUpstream
        AuthenticationSsoFailureCode.SERVER_ERROR -> SSOFailureCodes.ServerError
        AuthenticationSsoFailureCode.NOT_FOUND -> SSOFailureCodes.NotFound
        AuthenticationSsoFailureCode.FORBIDDEN -> SSOFailureCodes.Forbidden
        AuthenticationSsoFailureCode.NO_MATCHING_AUTH_REQUEST -> SSOFailureCodes.NoMatchingAuthReq
        AuthenticationSsoFailureCode.INSUFFICIENT_PERMISSIONS -> SSOFailureCodes.InsufficientPermissions
        AuthenticationSsoFailureCode.UNKNOWN -> SSOFailureCodes.Unknown
    }

private fun LoginPasswordPath.toAuthenticationLoginPasswordPath(): AuthenticationLoginPasswordPath =
    AuthenticationLoginPasswordPath(
        customServerConfig = customServerConfig?.toAuthenticationServerLinks(),
        isCloudAccountCreationPossible = isCloudAccountCreationPossible,
        domainClaim = isDomainClaimedByOrg.toAuthenticationDomainClaim(),
    )

private fun AuthenticationLoginPasswordPath.toLegacy(): LoginPasswordPath =
    LoginPasswordPath(
        customServerConfig = customServerConfig?.toLegacy(),
        isCloudAccountCreationPossible = isCloudAccountCreationPossible,
        isDomainClaimedByOrg = domainClaim.toLegacy(),
    )

private fun DomainClaimedByOrg.toAuthenticationDomainClaim(): AuthenticationDomainClaim =
    when (this) {
        DomainClaimedByOrg.NotClaimed -> AuthenticationDomainClaim.NotClaimed
        is DomainClaimedByOrg.Claimed -> AuthenticationDomainClaim.Claimed(domain)
    }

private fun AuthenticationDomainClaim.toLegacy(): DomainClaimedByOrg =
    when (this) {
        AuthenticationDomainClaim.NotClaimed -> DomainClaimedByOrg.NotClaimed
        is AuthenticationDomainClaim.Claimed -> DomainClaimedByOrg.Claimed(domain)
    }

internal fun ServerConfig.Links.toAuthenticationServerLinks(): AuthenticationServerLinks =
    AuthenticationServerLinks(
        api = api,
        accounts = accounts,
        webSocket = webSocket,
        blackList = blackList,
        teams = teams,
        website = website,
        title = title,
        isOnPremises = isOnPremises,
        apiProxy = apiProxy?.let {
            AuthenticationApiProxy(
                needsAuthentication = it.needsAuthentication,
                host = it.host,
                port = it.port,
            )
        },
        supportEmail = supportEmail,
    )

internal fun AuthenticationServerLinks.toLegacy(): ServerConfig.Links =
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
        supportEmail = supportEmail,
    )
