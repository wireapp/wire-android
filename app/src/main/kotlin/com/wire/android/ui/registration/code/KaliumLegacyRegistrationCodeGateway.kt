/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.registration.code

import com.wire.android.BuildConfig
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.di.ClientScopeProvider
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.authentication.legacyregistration.code.LegacyCodeActivationResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyPersonalRegistrationRequest
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegisterClientResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationCodeGateway
import com.wire.android.ui.authentication.legacyregistration.code.LegacyRegistrationResult
import com.wire.android.ui.authentication.legacyregistration.code.LegacyStoreSessionResult
import com.wire.android.util.WillNeverOccurError
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.register.RegisterParam
import com.wire.kalium.logic.feature.register.RegisterResult
import com.wire.kalium.logic.feature.register.RequestActivationCodeResult

class KaliumLegacyRegistrationCredentials internal constructor(
    internal val result: RegisterResult.Success,
)

internal class KaliumLegacyRegistrationCodeGateway(
    private val coreLogic: CoreLogic,
    private val addUser: AddAuthenticatedUserUseCase,
    private val analytics: RegistrationAnalyticsManagerUseCase,
    private val clientScopes: ClientScopeProvider.Factory,
    private val webSocketEnabled: Boolean,
) : LegacyRegistrationCodeGateway<ServerConfig.Links, CoreFailure, UserId, KaliumLegacyRegistrationCredentials> {
    override suspend fun onCodeVerificationShown() {
        analytics.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.CodeVerification)
    }

    override suspend fun onCodeVerificationFailed() {
        analytics.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.CodeVerificationFailed)
    }

    override suspend fun requestActivationCode(
        serverConfig: ServerConfig.Links,
        email: String,
    ): LegacyCodeActivationResult<CoreFailure> {
        val scope = resolveScope(serverConfig) ?: return LegacyCodeActivationResult.AuthScopeUnavailable
        return when (val result = scope.registerScope.requestActivationCode(email)) {
            RequestActivationCodeResult.Success -> LegacyCodeActivationResult.Sent
            RequestActivationCodeResult.Failure.AlreadyInUse -> LegacyCodeActivationResult.AlreadyInUse
            RequestActivationCodeResult.Failure.BlacklistedEmail -> LegacyCodeActivationResult.Blacklisted
            RequestActivationCodeResult.Failure.DomainBlocked -> LegacyCodeActivationResult.DomainBlocked
            RequestActivationCodeResult.Failure.InvalidEmail -> LegacyCodeActivationResult.InvalidEmail
            is RequestActivationCodeResult.Failure.Generic -> LegacyCodeActivationResult.Generic(result.failure)
        }
    }

    override suspend fun register(
        serverConfig: ServerConfig.Links,
        request: LegacyPersonalRegistrationRequest,
    ): LegacyRegistrationResult<CoreFailure, KaliumLegacyRegistrationCredentials> {
        val scope = resolveScope(serverConfig) ?: return LegacyRegistrationResult.AuthScopeUnavailable
        return when (
            val result = scope.registerScope.register(
                RegisterParam.PersonalAccount(
                    name = request.name,
                    password = request.password,
                    email = request.email,
                    emailActivationCode = request.activationCode(),
                ),
            )
        ) {
            is RegisterResult.Success -> LegacyRegistrationResult.Success(
                KaliumLegacyRegistrationCredentials(result),
            )

            RegisterResult.Failure.InvalidActivationCode -> LegacyRegistrationResult.InvalidActivationCode
            RegisterResult.Failure.AccountAlreadyExists -> LegacyRegistrationResult.AccountAlreadyExists
            RegisterResult.Failure.BlackListed -> LegacyRegistrationResult.Blacklisted
            RegisterResult.Failure.EmailDomainBlocked -> LegacyRegistrationResult.DomainBlocked
            RegisterResult.Failure.InvalidEmail -> LegacyRegistrationResult.InvalidEmail
            RegisterResult.Failure.TeamMembersLimitReached -> LegacyRegistrationResult.TeamMembersLimitReached
            RegisterResult.Failure.UserCreationRestricted -> LegacyRegistrationResult.UserCreationRestricted
            is RegisterResult.Failure.Generic -> LegacyRegistrationResult.Generic(result.failure)
        }
    }

    override suspend fun storeSession(
        credentials: KaliumLegacyRegistrationCredentials,
    ): LegacyStoreSessionResult<CoreFailure, UserId> = when (
        val result = addUser(
            credentials.result.toStoreSessionParam(webSocketEnabled),
            replace = false,
        )
    ) {
        is AddAuthenticatedUserUseCase.Result.Success -> LegacyStoreSessionResult.Success(result.userId)
        is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LegacyStoreSessionResult.Generic(result.genericFailure)
        AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
        AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
        AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> LegacyStoreSessionResult.UserAlreadyExists
    }

    override suspend fun registerClient(
        userId: UserId,
        password: String,
    ): LegacyRegisterClientResult<CoreFailure> = when (
        val result = clientScopes.create(userId).clientScope.getOrRegister(
            RegisterClientParam(
                password = password,
                capabilities = null,
                modelPostfix = privateBuildModelPostfix(),
            ),
        )
    ) {
        is RegisterClientResult.Success -> LegacyRegisterClientResult.Success
        is RegisterClientResult.E2EICertificateRequired -> LegacyRegisterClientResult.E2EICertificateRequired
        RegisterClientResult.Failure.TooManyClients -> LegacyRegisterClientResult.TooManyDevices
        is RegisterClientResult.Failure.Generic -> LegacyRegisterClientResult.Generic(result.genericFailure)
        is RegisterClientResult.Failure.InvalidCredentials -> throw WillNeverOccurError(
            "RegisterClient: wrong password when registering a new account",
        )

        RegisterClientResult.Failure.PasswordAuthRequired -> throw WillNeverOccurError(
            "RegisterClient: password required after creating a new account",
        )
    }

    private suspend fun resolveScope(serverConfig: ServerConfig.Links) = when (
        val result = coreLogic.versionedAuthenticationScope(serverConfig)(null)
    ) {
        is AutoVersionAuthScopeUseCase.Result.Success -> result.authenticationScope
        is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion,
        is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> null
    }
}
