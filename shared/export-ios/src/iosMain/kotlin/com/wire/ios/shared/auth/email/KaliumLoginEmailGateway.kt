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
package com.wire.shared.auth.email

import com.wire.ios.shared.IosKaliumRuntimeConfig
import com.wire.ios.shared.WireIosSharedConfig
import com.wire.shared.auth.AuthLoginSuccessPayload
import com.wire.shared.auth.login.model.toKalium
import com.wire.kalium.logic.CoreLogicCommon
import com.wire.kalium.logic.data.auth.AccountTokens
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.PersistSelfUserEmailResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.GetOrRegisterClientUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import dev.zacsweers.metro.Inject

/**
 * iOS runtime implementation of the shared login gateway.
 *
 * This class only bridges the shared auth ViewModel to Kalium. Delete it once the Kalium-backed
 * login orchestration is available from common shared auth code and no longer needs iOS runtime
 * path/config adaptation in export-ios.
 */
@Inject
class KaliumLoginEmailGateway(
    private val config: WireIosSharedConfig,
    private val coreLogic: CoreLogicCommon,
) : LoginEmailGateway {
    private val runtimeConfig: IosKaliumRuntimeConfig?
        get() = config.runtimeConfig

    override suspend fun login(
        userIdentifier: String,
        password: String,
        secondFactorVerificationCode: String?,
        usernameAllowed: Boolean,
    ): LoginEmailGatewayResult {
        val runtime = runtimeConfig ?: return missingRuntimeConfig()
        if (!usernameAllowed && !coreLogic.getGlobalScope().validateEmailUseCase(userIdentifier)) {
            return LoginEmailGatewayResult.Failure(LoginEmailError.InvalidUserIdentifier)
        }

        val authScope = when (val result = resolveAuthScope(runtime)) {
            is AuthScopeResult.Failure -> return LoginEmailGatewayResult.Failure(result.error)
            is AuthScopeResult.Success -> result.authScope
        }
        val loginResult = authScope.login(
            userIdentifier = userIdentifier,
            password = password,
            shouldPersistClient = true,
            secondFactorVerificationCode = secondFactorVerificationCode,
        )
        if (loginResult !is AuthenticationResult.Success) {
            return handleAuthenticationFailure(loginResult as AuthenticationResult.Failure, authScope, userIdentifier)
        }

        val storedUserId = addAuthenticatedUser(loginResult)
            ?: return LoginEmailGatewayResult.Failure(LoginEmailError.UserAlreadyExists)

        if (coreLogic.getGlobalScope().validateEmailUseCase(userIdentifier)) {
            val persistEmailResult = coreLogic.getSessionScope(storedUserId).users.persistSelfUserEmail(userIdentifier)
            if (persistEmailResult is PersistSelfUserEmailResult.Failure) {
                return LoginEmailGatewayResult.Failure(LoginEmailError.Generic(persistEmailResult.coreFailure.toString()))
            }
        }

        return when (
            val clientResult = coreLogic.getSessionScope(storedUserId).client.getOrRegister(
                RegisterClientParam(password = password, capabilities = null)
            )
        ) {
            is RegisterClientResult.Success ->
                LoginEmailGatewayResult.Success(
                    initialSyncCompleted = false,
                    isE2EIRequired = false,
                    payload = loginResult.authData.toSuccessPayload(
                        runtime = runtime,
                        userIdentifier = userIdentifier,
                        password = password,
                        secondFactorVerificationCode = secondFactorVerificationCode,
                        isE2EIRequired = false,
                        clientId = clientResult.client.id.value,
                    ),
                )

            is RegisterClientResult.E2EICertificateRequired ->
                LoginEmailGatewayResult.Success(
                    initialSyncCompleted = false,
                    isE2EIRequired = true,
                    payload = loginResult.authData.toSuccessPayload(
                        runtime = runtime,
                        userIdentifier = userIdentifier,
                        password = password,
                        secondFactorVerificationCode = secondFactorVerificationCode,
                        isE2EIRequired = true,
                        clientId = clientResult.client.id.value,
                    ),
                )

            is RegisterClientResult.Failure.TooManyClients ->
                LoginEmailGatewayResult.RemoveDeviceNeeded

            is RegisterClientResult.Failure ->
                LoginEmailGatewayResult.Failure(clientResult.toLoginEmailError())
        }
    }

    override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailGatewayResult {
        val runtime = runtimeConfig ?: return missingRuntimeConfig()
        val authScope = when (val result = resolveAuthScope(runtime)) {
            is AuthScopeResult.Failure -> return LoginEmailGatewayResult.Failure(result.error)
            is AuthScopeResult.Success -> result.authScope
        }
        return requestSecondFactorCode(authScope, userIdentifier)
    }

    private suspend fun resolveAuthScope(runtime: IosKaliumRuntimeConfig): AuthScopeResult =
        when (val result = coreLogic.versionedAuthenticationScope(runtime.serverLinks.toKalium()).invoke(null)) {
            is AutoVersionAuthScopeUseCase.Result.Success -> AuthScopeResult.Success(result.authenticationScope)
            is AutoVersionAuthScopeUseCase.Result.Failure -> AuthScopeResult.Failure(result.toLoginEmailError())
        }

    private suspend fun addAuthenticatedUser(loginResult: AuthenticationResult.Success): UserId? =
        when (
            val addResult = coreLogic.getGlobalScope().addAuthenticatedAccount(
                session = StoreSessionParam(
                    accountTokens = loginResult.authData,
                    ssoId = loginResult.ssoID,
                    managedBy = loginResult.managedBy,
                    serverConfigId = loginResult.serverConfigId,
                    proxyCredentials = loginResult.proxyCredentials,
                    isPersistentWebSocketEnabled = false,
                ),
                replace = false,
            )
        ) {
            is AddAuthenticatedUserUseCase.Result.Success -> addResult.userId
            is AddAuthenticatedUserUseCase.Result.Failure -> null
        }

    private suspend fun handleAuthenticationFailure(
        failure: AuthenticationResult.Failure,
        authScope: AuthenticationScope,
        userIdentifier: String,
    ): LoginEmailGatewayResult =
        when (failure) {
            AuthenticationResult.Failure.InvalidCredentials.Missing2FA ->
                requestSecondFactorCode(authScope, userIdentifier)

            AuthenticationResult.Failure.InvalidCredentials.Invalid2FA ->
                LoginEmailGatewayResult.SecondFactorRequired(
                    email = userIdentifier,
                    isCurrentCodeInvalid = true,
                )

            else -> LoginEmailGatewayResult.Failure(failure.toLoginEmailError())
        }

    private suspend fun requestSecondFactorCode(
        authScope: AuthenticationScope,
        userIdentifier: String,
    ): LoginEmailGatewayResult {
        if (!userIdentifier.contains("@")) {
            return LoginEmailGatewayResult.Failure(LoginEmailError.RequestSecondFactorWithHandle)
        }
        return when (
            val result = authScope.requestSecondFactorVerificationCode(
                email = userIdentifier,
                verifiableAction = VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION,
            )
        ) {
            RequestSecondFactorVerificationCodeUseCase.Result.Success,
            RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests ->
                LoginEmailGatewayResult.SecondFactorRequired(email = userIdentifier)

            is RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic ->
                LoginEmailGatewayResult.Failure(LoginEmailError.Generic(result.cause.toString()))

            else -> LoginEmailGatewayResult.Failure(LoginEmailError.Generic())
        }
    }
}

private fun AccountTokens.toSuccessPayload(
    runtime: IosKaliumRuntimeConfig,
    userIdentifier: String,
    password: String,
    secondFactorVerificationCode: String?,
    isE2EIRequired: Boolean,
    clientId: String?,
): AuthLoginSuccessPayload =
    AuthLoginSuccessPayload(
        userIdValue = userId.value,
        userIdDomain = userId.domain.ifBlank { null },
        accessTokenValue = accessToken.value,
        accessTokenType = accessToken.tokenType,
        accessTokenExpiresInSeconds = null,
        refreshTokenValue = refreshToken.value,
        refreshTokenCookieDomain = runtime.backendDomain,
        email = userIdentifier,
        password = password,
        secondFactorCode = secondFactorVerificationCode,
        initialSyncCompleted = false,
        isE2EIRequired = isE2EIRequired,
        clientId = clientId,
    )

private sealed interface AuthScopeResult {
    data class Success(val authScope: AuthenticationScope) : AuthScopeResult
    data class Failure(val error: LoginEmailError) : AuthScopeResult
}

private fun AutoVersionAuthScopeUseCase.Result.Failure.toLoginEmailError(): LoginEmailError =
    when (this) {
        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> LoginEmailError.Generic(genericFailure.toString())
        AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> LoginEmailError.ClientUpdateRequired
        AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> LoginEmailError.ServerVersionNotSupported
    }

private fun AuthenticationResult.Failure.toLoginEmailError(): LoginEmailError =
    when (this) {
        AuthenticationResult.Failure.AccountPendingActivation -> LoginEmailError.AccountPendingActivation
        AuthenticationResult.Failure.AccountSuspended -> LoginEmailError.AccountSuspended
        AuthenticationResult.Failure.InvalidCredentials.Invalid2FA -> LoginEmailError.InvalidCredentials
        AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination -> LoginEmailError.InvalidCredentials
        AuthenticationResult.Failure.InvalidCredentials.Missing2FA -> LoginEmailError.RequestSecondFactorWithHandle
        AuthenticationResult.Failure.InvalidUserIdentifier -> LoginEmailError.InvalidUserIdentifier
        AuthenticationResult.Failure.SocketError -> LoginEmailError.Generic()
        is AuthenticationResult.Failure.Generic -> LoginEmailError.Generic(genericFailure.toString())
    }

private fun RegisterClientResult.Failure.toLoginEmailError(): LoginEmailError =
    when (this) {
        is RegisterClientResult.Failure.Generic -> LoginEmailError.Generic(genericFailure.toString())
        is RegisterClientResult.Failure.InvalidCredentials -> LoginEmailError.InvalidCredentials
        is RegisterClientResult.Failure.PasswordAuthRequired -> LoginEmailError.PasswordNeededToRegisterClient
        is RegisterClientResult.Failure.TooManyClients -> LoginEmailError.TooManyDevices
    }

private fun missingRuntimeConfig(): LoginEmailGatewayResult =
    LoginEmailGatewayResult.Failure(LoginEmailError.Generic(MISSING_RUNTIME_CONFIG_ERROR))

private const val MISSING_RUNTIME_CONFIG_ERROR = "Kalium runtime config is required for shared iOS login"
