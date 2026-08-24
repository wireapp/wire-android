/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.registration.code

import com.wire.android.BuildConfig
import com.wire.android.analytics.RegistrationAnalyticsManagerUseCase
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.ui.authentication.create.common.CreateAccountDataNavArgs
import com.wire.android.ui.authentication.legacyregistration.code.*
import com.wire.android.util.WillNeverOccurError
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
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

class KaliumLegacyRegistrationCredentials internal constructor(internal val result: RegisterResult.Success)

/** App-only Kalium/BuildConfig bridge around the feature-owned legacy registration VM. */
class CreateAccountVerificationCodeViewModel @AssistedInject constructor(
    @Assisted val createAccountNavArgs: CreateAccountDataNavArgs,
    @KaliumCoreLogic coreLogic: CoreLogic,
    addAuthenticatedUser: AddAuthenticatedUserUseCase,
    analytics: RegistrationAnalyticsManagerUseCase,
    clientScopes: ClientScopeProvider.Factory,
    defaultServerConfig: ServerConfig.Links,
    @DefaultWebSocketEnabledByDefault webSocketEnabled: Boolean,
) : LegacyRegistrationCodeViewModel<ServerConfig.Links, CoreFailure, UserId, KaliumLegacyRegistrationCredentials>(
    LegacyRegistrationCodeInput(createAccountNavArgs.customServerConfig, createAccountNavArgs.userRegistrationInfo.email, createAccountNavArgs.userRegistrationInfo.name, createAccountNavArgs.userRegistrationInfo.password),
    defaultServerConfig,
    KaliumLegacyRegistrationCodeGateway(coreLogic, addAuthenticatedUser, analytics, clientScopes, webSocketEnabled),
) {
    @AssistedFactory interface Factory { fun create(createAccountNavArgs: CreateAccountDataNavArgs): CreateAccountVerificationCodeViewModel }
    val codeState: CreateAccountVerificationCodeViewState get() = state.toLegacy()
    fun clearCodeError() = clearError()
}

private class KaliumLegacyRegistrationCodeGateway(
    private val coreLogic: CoreLogic,
    private val addUser: AddAuthenticatedUserUseCase,
    private val analytics: RegistrationAnalyticsManagerUseCase,
    private val clientScopes: ClientScopeProvider.Factory,
    private val webSocketEnabled: Boolean,
) : LegacyRegistrationCodeGateway<ServerConfig.Links, CoreFailure, UserId, KaliumLegacyRegistrationCredentials> {
    override fun onCodeVerificationShown() = analytics.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.CodeVerification)
    override fun onCodeVerificationFailed() = analytics.sendEventIfEnabled(AnalyticsEvent.RegistrationPersonalAccount.CodeVerificationFailed)
    override suspend fun requestActivationCode(serverConfig: ServerConfig.Links, email: String): LegacyCodeActivationResult<CoreFailure> = when (val scope = scope(serverConfig)) {
        null -> LegacyCodeActivationResult.AuthScopeUnavailable
        else -> when (val result = scope.registerScope.requestActivationCode(email)) {
            RequestActivationCodeResult.Success -> LegacyCodeActivationResult.Sent
            RequestActivationCodeResult.Failure.AlreadyInUse -> LegacyCodeActivationResult.AlreadyInUse
            RequestActivationCodeResult.Failure.BlacklistedEmail -> LegacyCodeActivationResult.Blacklisted
            RequestActivationCodeResult.Failure.DomainBlocked -> LegacyCodeActivationResult.DomainBlocked
            RequestActivationCodeResult.Failure.InvalidEmail -> LegacyCodeActivationResult.InvalidEmail
            is RequestActivationCodeResult.Failure.Generic -> LegacyCodeActivationResult.Generic(result.failure)
        }
    }
    override suspend fun register(serverConfig: ServerConfig.Links, request: LegacyPersonalRegistrationRequest): LegacyRegistrationResult<CoreFailure, KaliumLegacyRegistrationCredentials> {
        val scope = scope(serverConfig) ?: return LegacyRegistrationResult.AuthScopeUnavailable
        return when (val result = scope.registerScope.register(RegisterParam.PersonalAccount(
            name = request.name, password = request.password, email = request.email, emailActivationCode = request.activationCode(),
        ))) {
            is RegisterResult.Success -> LegacyRegistrationResult.Success(KaliumLegacyRegistrationCredentials(result))
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
    override suspend fun storeSession(credentials: KaliumLegacyRegistrationCredentials): LegacyStoreSessionResult<CoreFailure, UserId> = when (val result = credentials.result.let {
        addUser(StoreSessionParam(accountTokens = it.authData, ssoId = it.ssoID, serverConfigId = it.serverConfigId, proxyCredentials = it.proxyCredentials, isPersistentWebSocketEnabled = webSocketEnabled), replace = false)
    }) {
        is AddAuthenticatedUserUseCase.Result.Success -> LegacyStoreSessionResult.Success(result.userId)
        is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LegacyStoreSessionResult.Generic(result.genericFailure)
        AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists, AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged, AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> LegacyStoreSessionResult.UserAlreadyExists
    }
    override suspend fun registerClient(userId: UserId, password: String): LegacyRegisterClientResult<CoreFailure> = when (val result = clientScopes.create(userId).clientScope.getOrRegister(RegisterClientParam(password = password, capabilities = null, modelPostfix = if (BuildConfig.PRIVATE_BUILD) " [${BuildConfig.FLAVOR}_${BuildConfig.BUILD_TYPE}]" else null))) {
        is RegisterClientResult.Success -> LegacyRegisterClientResult.Success
        is RegisterClientResult.E2EICertificateRequired -> LegacyRegisterClientResult.E2EICertificateRequired
        RegisterClientResult.Failure.TooManyClients -> LegacyRegisterClientResult.TooManyDevices
        is RegisterClientResult.Failure.Generic -> LegacyRegisterClientResult.Generic(result.genericFailure)
        is RegisterClientResult.Failure.InvalidCredentials -> throw WillNeverOccurError("RegisterClient: wrong password when registering a new account")
        RegisterClientResult.Failure.PasswordAuthRequired -> throw WillNeverOccurError("RegisterClient: password required after creating a new account")
    }
    private suspend fun scope(config: ServerConfig.Links) = when (val result = coreLogic.versionedAuthenticationScope(config)(null)) {
        is AutoVersionAuthScopeUseCase.Result.Success -> result.authenticationScope
        is AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion, is AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion,
        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> null
    }
}

private fun LegacyRegistrationCodeState<UserId, CoreFailure>.toLegacy() = CreateAccountVerificationCodeViewState(
    codeLength = codeLength, email = email, loading = loading, result = when (val value = result) {
        LegacyRegistrationCodeState.Result.None -> CreateAccountCodeResult.None
        is LegacyRegistrationCodeState.Result.Success -> CreateAccountCodeResult.Success(value.userId)
        LegacyRegistrationCodeState.Result.InvalidActivationCode -> CreateAccountCodeResult.Error.TextFieldError.InvalidActivationCodeError
        LegacyRegistrationCodeState.Result.AccountAlreadyExists -> CreateAccountCodeResult.Error.DialogError.AccountAlreadyExistsError
        LegacyRegistrationCodeState.Result.Blacklisted -> CreateAccountCodeResult.Error.DialogError.BlackListedError
        LegacyRegistrationCodeState.Result.DomainBlocked -> CreateAccountCodeResult.Error.DialogError.EmailDomainBlockedError
        LegacyRegistrationCodeState.Result.InvalidEmail -> CreateAccountCodeResult.Error.DialogError.InvalidEmailError
        LegacyRegistrationCodeState.Result.TeamMembersLimit -> CreateAccountCodeResult.Error.DialogError.TeamMembersLimitError
        LegacyRegistrationCodeState.Result.CreationRestricted -> CreateAccountCodeResult.Error.DialogError.CreationRestrictedError
        LegacyRegistrationCodeState.Result.UserAlreadyExists -> CreateAccountCodeResult.Error.DialogError.UserAlreadyExistsError
        is LegacyRegistrationCodeState.Result.Generic -> CreateAccountCodeResult.Error.DialogError.GenericError(value.failure)
        is LegacyRegistrationCodeState.Result.TooManyDevices -> CreateAccountCodeResult.Error.TooManyDevicesError(value.userId)
    }
)
