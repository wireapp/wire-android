/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.create.code

import com.wire.android.BuildConfig
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.create.common.CreateAccountFlowType
import com.wire.android.ui.authentication.create.common.CreateAccountNavArgs
import com.wire.android.util.WillNeverOccurError
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.register.RegisterParam
import com.wire.kalium.logic.feature.register.RegisterResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult
import dev.zacsweers.metro.Inject

class KaliumCreateAccountCredentials internal constructor(internal val result: RegisterResult.Success)

typealias AppCreateAccountCodeViewModel =
    CreateAccountCodeViewModel<CreateAccountFlowType, ServerConfig.Links, CoreFailure, UserId, KaliumCreateAccountCredentials>

internal class KaliumCreateAccountCodeGateway(
    private val coreLogic: CoreLogic,
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val clientScopeProviderFactory: ClientScopeProvider.Factory,
    private val defaultWebSocketEnabledByDefault: Boolean,
    private val buildFlags: CreateAccountCodeBuildFlags = CreateAccountCodeBuildFlags.current(),
) : CreateAccountCodeGateway<ServerConfig.Links, CoreFailure, UserId, KaliumCreateAccountCredentials> {

    override suspend fun requestActivationCode(
        serverConfig: ServerConfig.Links,
        email: String,
    ): ActivationCodeRequestResult<CoreFailure> {
        val authScope = when (val result = coreLogic.versionedAuthenticationScope(serverConfig)(null)) {
            is AutoVersionAuthScopeUseCase.Result.Success -> result.authenticationScope
            is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion,
            is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
            is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> return ActivationCodeRequestResult.AuthScopeUnavailable
        }
        return when (val result = authScope.registerScope.requestActivationCode(email)) {
            RequestActivationCodeResult.Success -> ActivationCodeRequestResult.Sent
            RequestActivationCodeResult.Failure.AlreadyInUse -> ActivationCodeRequestResult.AlreadyInUse
            RequestActivationCodeResult.Failure.BlacklistedEmail -> ActivationCodeRequestResult.Blacklisted
            RequestActivationCodeResult.Failure.DomainBlocked -> ActivationCodeRequestResult.DomainBlocked
            RequestActivationCodeResult.Failure.InvalidEmail -> ActivationCodeRequestResult.InvalidEmail
            is RequestActivationCodeResult.Failure.Generic -> ActivationCodeRequestResult.Generic(result.failure)
        }
    }

    override suspend fun register(
        serverConfig: ServerConfig.Links,
        request: CreateAccountRegistrationRequest,
    ): AccountRegistrationResult<CoreFailure, KaliumCreateAccountCredentials> {
        val authScope = when (val result = coreLogic.versionedAuthenticationScope(serverConfig)(null)) {
            is AutoVersionAuthScopeUseCase.Result.Success -> result.authenticationScope
            is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion,
            is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
            is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> return AccountRegistrationResult.AuthScopeUnavailable
        }
        val parameter = when (request) {
            is CreateAccountRegistrationRequest.Personal -> RegisterParam.PrivateAccount(
                firstName = request.firstName,
                lastName = request.lastName,
                password = request.password,
                email = request.email,
                emailActivationCode = request.activationCode(),
            )

            is CreateAccountRegistrationRequest.Team -> RegisterParam.Team(
                firstName = request.firstName,
                lastName = request.lastName,
                password = request.password,
                email = request.email,
                emailActivationCode = request.activationCode(),
                teamName = request.teamName,
                teamIcon = request.teamIcon,
            )
        }
        return when (val result = authScope.registerScope.register(parameter)) {
            is RegisterResult.Success -> AccountRegistrationResult.Success(KaliumCreateAccountCredentials(result))
            RegisterResult.Failure.InvalidActivationCode -> AccountRegistrationResult.InvalidActivationCode
            RegisterResult.Failure.AccountAlreadyExists -> AccountRegistrationResult.AccountAlreadyExists
            RegisterResult.Failure.BlackListed -> AccountRegistrationResult.Blacklisted
            RegisterResult.Failure.EmailDomainBlocked -> AccountRegistrationResult.DomainBlocked
            RegisterResult.Failure.InvalidEmail -> AccountRegistrationResult.InvalidEmail
            RegisterResult.Failure.TeamMembersLimitReached -> AccountRegistrationResult.TeamMembersLimitReached
            RegisterResult.Failure.UserCreationRestricted -> AccountRegistrationResult.UserCreationRestricted
            is RegisterResult.Failure.Generic -> AccountRegistrationResult.Generic(result.failure)
        }
    }

    override suspend fun storeSession(
        credentials: KaliumCreateAccountCredentials,
    ): StoreAccountSessionResult<CoreFailure, UserId> {
        val result = credentials.result
        return when (
            val stored = addAuthenticatedUser(
                StoreSessionParam(
                    accountTokens = result.authData,
                    ssoId = result.ssoID,
                    serverConfigId = result.serverConfigId,
                    proxyCredentials = result.proxyCredentials,
                    isPersistentWebSocketEnabled = defaultWebSocketEnabledByDefault,
                ),
                replace = false,
            )
        ) {
            is AddAuthenticatedUserUseCase.Result.Success -> StoreAccountSessionResult.Success(stored.userId)
            is AddAuthenticatedUserUseCase.Result.Failure.Generic -> StoreAccountSessionResult.Generic(stored.genericFailure)
            AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
            AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
            AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> StoreAccountSessionResult.UserAlreadyExists
        }
    }

    override suspend fun registerClient(userId: UserId, password: String): CreateAccountClientResult<CoreFailure> {
        val result = clientScopeProviderFactory.create(userId).clientScope.getOrRegister(
            RegisterClientParam(
                password = password,
                capabilities = null,
                modelPostfix = buildFlags.modelPostfix,
            )
        )
        return when (result) {
            is RegisterClientResult.Success -> CreateAccountClientResult.Success
            is RegisterClientResult.E2EICertificateRequired -> CreateAccountClientResult.E2EICertificateRequired
            RegisterClientResult.Failure.TooManyClients -> CreateAccountClientResult.TooManyDevices
            is RegisterClientResult.Failure.Generic -> CreateAccountClientResult.Generic(result.genericFailure)
            is RegisterClientResult.Failure.InvalidCredentials -> throw WillNeverOccurError(
                "RegisterClient: wrong password when register client after creating a new account"
            )

            RegisterClientResult.Failure.PasswordAuthRequired -> throw WillNeverOccurError(
                "RegisterClient: password required to register client after creating new account with email"
            )
        }
    }
}

internal data class CreateAccountCodeBuildFlags(
    val privateBuild: Boolean,
    val flavor: String,
    val buildType: String,
) {
    val modelPostfix: String?
        get() = if (privateBuild) " [${flavor}_${buildType}]" else null

    companion object {
        fun current() = CreateAccountCodeBuildFlags(BuildConfig.PRIVATE_BUILD, BuildConfig.FLAVOR, BuildConfig.BUILD_TYPE)
    }
}

internal class AndroidCreateAccountCodeResendTimer(private val countdownTimer: CountdownTimer) : CreateAccountCodeResendTimer {
    override suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit) {
        countdownTimer.start(seconds, onUpdate, onFinish)
    }
}

class CreateAccountCodeViewModelHostFactory @Inject constructor(
    @KaliumCoreLogic coreLogic: CoreLogic,
    addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    defaultServerConfig: ServerConfig.Links,
    @DefaultWebSocketEnabledByDefault defaultWebSocketEnabledByDefault: Boolean,
) {
    private val gateway = KaliumCreateAccountCodeGateway(
        coreLogic,
        addAuthenticatedUser,
        clientScopeProviderFactory,
        defaultWebSocketEnabledByDefault,
    )
    private val defaultLinks = defaultServerConfig

    fun create(navArgs: CreateAccountNavArgs): AppCreateAccountCodeViewModel {
        val info = navArgs.userRegistrationInfo
        return CreateAccountCodeViewModel(
            input = CreateAccountCodeInput(
                flowType = navArgs.flowType,
                customServerConfig = navArgs.customServerConfig,
                email = info.email,
                firstName = info.firstName,
                lastName = info.lastName,
                password = info.password,
                teamName = info.teamName,
                isTeam = navArgs.flowType == CreateAccountFlowType.CreateTeam,
            ),
            defaultServerConfig = defaultLinks,
            gateway = gateway,
            resendCodeTimer = AndroidCreateAccountCodeResendTimer(CountdownTimer()),
        )
    }
}
