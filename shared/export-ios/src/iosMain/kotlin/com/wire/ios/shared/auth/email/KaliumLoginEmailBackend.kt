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
package com.wire.ios.shared.auth.email

import com.wire.ios.shared.IosKaliumRuntimeConfig
import com.wire.ios.shared.WireIosSharedConfig
import com.wire.ios.shared.auth.login.model.toKalium
import com.wire.kalium.logic.CoreLogicCommon
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

@Inject
class KaliumLoginEmailBackend(
    private val config: WireIosSharedConfig,
    private val coreLogic: CoreLogicCommon,
) : LoginEmailBackend {
    private val runtimeConfig: IosKaliumRuntimeConfig?
        get() = config.runtimeConfig

    private val localBackend = LocalLoginEmailBackend()

    override suspend fun login(
        userIdentifier: String,
        password: String,
        secondFactorVerificationCode: String?,
        usernameAllowed: Boolean,
    ): LoginEmailBackendResult {
        val runtime = runtimeConfig ?: return localBackend.login(
            userIdentifier = userIdentifier,
            password = password,
            secondFactorVerificationCode = secondFactorVerificationCode,
            usernameAllowed = usernameAllowed,
        )
        if (!usernameAllowed && !coreLogic.getGlobalScope().validateEmailUseCase(userIdentifier)) {
            return LoginEmailBackendResult.Failure(LoginEmailError.InvalidUserIdentifier)
        }

        val authScope = when (val result = resolveAuthScope(runtime)) {
            is AuthScopeResult.Failure -> return LoginEmailBackendResult.Failure(result.error)
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
            ?: return LoginEmailBackendResult.Failure(LoginEmailError.UserAlreadyExists)

        if (coreLogic.getGlobalScope().validateEmailUseCase(userIdentifier)) {
            val persistEmailResult = coreLogic.getSessionScope(storedUserId).users.persistSelfUserEmail(userIdentifier)
            if (persistEmailResult is PersistSelfUserEmailResult.Failure) {
                return LoginEmailBackendResult.Failure(LoginEmailError.Generic(persistEmailResult.coreFailure.toString()))
            }
        }

        return when (
            val clientResult = coreLogic.getSessionScope(storedUserId).client.getOrRegister(
                RegisterClientParam(password = password, capabilities = null)
            )
        ) {
            is RegisterClientResult.Success ->
                LoginEmailBackendResult.Success(initialSyncCompleted = false, isE2EIRequired = false)

            is RegisterClientResult.E2EICertificateRequired ->
                LoginEmailBackendResult.Success(initialSyncCompleted = false, isE2EIRequired = true)

            is RegisterClientResult.Failure.TooManyClients ->
                LoginEmailBackendResult.RemoveDeviceNeeded

            is RegisterClientResult.Failure ->
                LoginEmailBackendResult.Failure(clientResult.toLoginEmailError())
        }
    }

    override suspend fun requestSecondFactorCode(userIdentifier: String): LoginEmailBackendResult {
        val runtime = runtimeConfig ?: return localBackend.requestSecondFactorCode(userIdentifier)
        val authScope = when (val result = resolveAuthScope(runtime)) {
            is AuthScopeResult.Failure -> return LoginEmailBackendResult.Failure(result.error)
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
    ): LoginEmailBackendResult =
        when (failure) {
            AuthenticationResult.Failure.InvalidCredentials.Missing2FA ->
                requestSecondFactorCode(authScope, userIdentifier)

            AuthenticationResult.Failure.InvalidCredentials.Invalid2FA ->
                LoginEmailBackendResult.SecondFactorRequired(
                    email = userIdentifier,
                    isCurrentCodeInvalid = true,
                )

            else -> LoginEmailBackendResult.Failure(failure.toLoginEmailError())
        }

    private suspend fun requestSecondFactorCode(
        authScope: AuthenticationScope,
        userIdentifier: String,
    ): LoginEmailBackendResult {
        if (!userIdentifier.contains("@")) {
            return LoginEmailBackendResult.Failure(LoginEmailError.RequestSecondFactorWithHandle)
        }
        return when (
            val result = authScope.requestSecondFactorVerificationCode(
                email = userIdentifier,
                verifiableAction = VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION,
            )
        ) {
            RequestSecondFactorVerificationCodeUseCase.Result.Success,
            RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests ->
                LoginEmailBackendResult.SecondFactorRequired(email = userIdentifier)

            is RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic ->
                LoginEmailBackendResult.Failure(LoginEmailError.Generic(result.cause.toString()))

            else -> LoginEmailBackendResult.Failure(LoginEmailError.Generic())
        }
    }
}

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
