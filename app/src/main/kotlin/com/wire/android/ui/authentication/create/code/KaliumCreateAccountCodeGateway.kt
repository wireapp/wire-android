/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.create.code

import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.register.RegisterResult

class KaliumCreateAccountCredentials internal constructor(internal val result: RegisterResult.Success)

typealias AppCreateAccountCodeViewModel =
    CreateAccountCodeViewModel<CreateAccountRouteFlowType, ServerConfig.Links, CoreFailure, UserId, KaliumCreateAccountCredentials>

internal class KaliumCreateAccountCodeGateway(
    private val coreLogic: CoreLogic,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val defaultWebSocketEnabledByDefault: Boolean,
    private val buildFlags: CreateAccountCodeBuildFlags = CreateAccountCodeBuildFlags.current(),
) : CreateAccountCodeGateway<ServerConfig.Links, CoreFailure, UserId, KaliumCreateAccountCredentials> {
    override suspend fun requestActivationCode(
        serverConfig: ServerConfig.Links,
        email: String
    ): ActivationCodeRequestResult<CoreFailure> {
        val scope = authenticationScope(serverConfig) ?: return ActivationCodeRequestResult.AuthScopeUnavailable
        return scope.registerScope.requestActivationCode(email).toActivationCodeResult()
    }

    override suspend fun register(
        serverConfig: ServerConfig.Links,
        request: CreateAccountRegistrationRequest
    ): AccountRegistrationResult<CoreFailure, KaliumCreateAccountCredentials> {
        val scope = authenticationScope(serverConfig) ?: return AccountRegistrationResult.AuthScopeUnavailable
        return scope.registerScope.register(request.toRegisterParam()).toRegistrationResult()
    }

    override suspend fun storeSession(credentials: KaliumCreateAccountCredentials): StoreAccountSessionResult<CoreFailure, UserId> =
        addAuthenticatedUser(
            credentials.result.toStoreSessionParam(defaultWebSocketEnabledByDefault),
            replace = false
        ).toStoreSessionResult()

    override suspend fun registerClient(userId: UserId, password: String): CreateAccountClientResult<CoreFailure> =
        clientScopeProviderFactory.create(userId).clientScope.getOrRegister(
            RegisterClientParam(password, capabilities = null, modelPostfix = buildFlags.modelPostfix),
        ).toCreateAccountClientResult()

    private suspend fun authenticationScope(serverConfig: ServerConfig.Links) = when (
        val result = coreLogic.versionedAuthenticationScope(serverConfig)(null)
    ) {
        is AutoVersionAuthScopeUseCase.Result.Success -> result.authenticationScope
        is AutoVersionAuthScopeUseCase.Result.Failure -> null
    }
}
