/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.email

import androidx.lifecycle.SavedStateHandle
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.SavedStateLoginSavedInputStore
import com.wire.android.ui.authentication.login.isProxyAuthRequired
import com.wire.android.ui.authentication.toBackendConfigUrl
import com.wire.android.util.BackendSupportConfig
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.SupportUrlResolver
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.login.ProxyCredentials
import com.wire.kalium.logic.data.auth.verification.VerifiableAction
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.AuthenticationResult
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.PersistSelfUserEmailResult
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.verification.RequestSecondFactorVerificationCodeUseCase
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.withContext

typealias AppLoginEmailViewModel = LoginEmailViewModel<
        ServerConfig.Links,
        CoreFailure,
        UserId,
        AuthenticationScope,
        StoreSessionParam,
        String,
        DomainClaimedByOrg,
        >
typealias AppLoginEmailState = LoginEmailState<CoreFailure, UserId>

@Suppress("TooManyFunctions", "LongParameterList")
internal class KaliumLoginEmailGateway(
    private val addAuthenticatedUser: AddAuthenticatedUserUseCase,
    private val coreLogic: CoreLogic,
    private val loginExtension: LoginViewModelExtension,
    private val dispatchers: DispatcherProvider,
    private val defaultWebSocketEnabledByDefault: Boolean,
    private val getServerConfigUseCase: Lazy<GetServerConfigUseCase>?,
    private val globalDataStore: Lazy<GlobalDataStore>?,
) : LoginEmailGateway<ServerConfig.Links, CoreFailure, UserId, AuthenticationScope, StoreSessionParam, String> {
    override fun isProxyAuthRequired(serverConfig: ServerConfig.Links) = serverConfig.isProxyAuthRequired
    override fun isEmail(value: String) = coreLogic.getGlobalScope().validateEmailUseCase(value)

    override suspend fun currentValidSession(): UserId? =
        coreLogic.getGlobalScope().session.currentSession().let {
            if (it is CurrentSessionResult.Success && it.accountInfo.isValid()) it.accountInfo.userId else null
        }

    override suspend fun resolveScope(
        serverConfig: ServerConfig.Links,
        proxyCredentials: () -> LoginEmailProxyCredentials?,
    ): LoginEmailScopeResult<CoreFailure, AuthenticationScope> = withContext(dispatchers.io()) {
        val proxy = proxyCredentials()?.let { ProxyCredentials(it.identifier, it.password) }
        when (val result = coreLogic.versionedAuthenticationScope(serverConfig)(proxy)) {
            is AutoVersionAuthScopeUseCase.Result.Success -> LoginEmailScopeResult.Success(result.authenticationScope)
            AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> LoginEmailScopeResult.UnknownServerVersion
            AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> LoginEmailScopeResult.ClientUpdateRequired
            is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> LoginEmailScopeResult.Failure(result.genericFailure)
        }
    }

    override suspend fun authenticate(
        scope: AuthenticationScope,
        identifier: () -> String,
        password: () -> String,
        secondFactorCode: String,
    ): LoginEmailAuthenticationResult<CoreFailure, StoreSessionParam> = withContext(dispatchers.io()) {
        when (val result = scope.login(identifier(), password(), true, secondFactorVerificationCode = secondFactorCode)) {
            is AuthenticationResult.Success -> LoginEmailAuthenticationResult.Success(
                StoreSessionParam(
                    accountTokens = result.authData,
                    ssoId = result.ssoID,
                    managedBy = result.managedBy,
                    serverConfigId = result.serverConfigId,
                    proxyCredentials = result.proxyCredentials,
                    isPersistentWebSocketEnabled = defaultWebSocketEnabledByDefault,
                )
            )
            AuthenticationResult.Failure.SocketError -> LoginEmailAuthenticationResult.ProxyError
            AuthenticationResult.Failure.InvalidCredentials.Missing2FA -> LoginEmailAuthenticationResult.MissingSecondFactor
            AuthenticationResult.Failure.InvalidCredentials.Invalid2FA -> LoginEmailAuthenticationResult.InvalidSecondFactor
            AuthenticationResult.Failure.InvalidCredentials.InvalidPasswordIdentityCombination ->
                LoginEmailAuthenticationResult.InvalidCredentials
            AuthenticationResult.Failure.InvalidUserIdentifier -> LoginEmailAuthenticationResult.InvalidIdentifier
            AuthenticationResult.Failure.AccountSuspended -> LoginEmailAuthenticationResult.AccountSuspended
            AuthenticationResult.Failure.AccountPendingActivation -> LoginEmailAuthenticationResult.AccountPendingActivation
            is AuthenticationResult.Failure.Generic -> LoginEmailAuthenticationResult.Failure(result.genericFailure)
        }
    }

    override suspend fun storeSession(session: StoreSessionParam): LoginEmailStoreResult<CoreFailure, UserId> =
        withContext(dispatchers.io()) {
            when (val result = addAuthenticatedUser(session, replace = false)) {
                is AddAuthenticatedUserUseCase.Result.Success -> LoginEmailStoreResult.Success(result.userId)
                is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LoginEmailStoreResult.Failure(result.genericFailure)
                AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
                AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
                AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> LoginEmailStoreResult.UserAlreadyExists
            }
        }

    override suspend fun persistEmailIfNeeded(
        userId: UserId,
        identifier: () -> String,
    ): LoginEmailPersistResult<CoreFailure> =
        withContext(dispatchers.io()) {
            if (!coreLogic.getGlobalScope().validateEmailUseCase(identifier())) return@withContext LoginEmailPersistResult.Success
            when (val result = coreLogic.getSessionScope(userId).users.persistSelfUserEmail(identifier())) {
                PersistSelfUserEmailResult.Success -> LoginEmailPersistResult.Success
                is PersistSelfUserEmailResult.Failure -> LoginEmailPersistResult.Failure(result.coreFailure)
            }
        }

    override suspend fun registerClient(userId: UserId, password: () -> String): LoginEmailClientResult<CoreFailure> =
        withContext(dispatchers.io()) {
            when (val result = loginExtension.registerClient(userId, password())) {
                is RegisterClientResult.Success ->
                    LoginEmailClientResult.Success(loginExtension.isInitialSyncCompleted(userId))
                is RegisterClientResult.E2EICertificateRequired ->
                    LoginEmailClientResult.E2EICertificateRequired(loginExtension.isInitialSyncCompleted(userId))
                RegisterClientResult.Failure.TooManyClients -> LoginEmailClientResult.TooManyDevices
                is RegisterClientResult.Failure.InvalidCredentials -> LoginEmailClientResult.InvalidCredentials
                RegisterClientResult.Failure.PasswordAuthRequired -> LoginEmailClientResult.PasswordRequired
                is RegisterClientResult.Failure.Generic -> LoginEmailClientResult.Failure(result.genericFailure)
            }
        }

    override suspend fun requestSecondFactorCode(
        scope: AuthenticationScope,
        email: String,
    ): LoginEmailVerificationResult<CoreFailure> = when (
        val result = scope.requestSecondFactorVerificationCode(email, VerifiableAction.LOGIN_OR_CLIENT_REGISTRATION)
    ) {
        is RequestSecondFactorVerificationCodeUseCase.Result.Success -> LoginEmailVerificationResult.Sent
        RequestSecondFactorVerificationCodeUseCase.Result.Failure.TooManyRequests -> LoginEmailVerificationResult.TooManyRequests
        is RequestSecondFactorVerificationCodeUseCase.Result.Failure.Generic -> LoginEmailVerificationResult.Failure(result.cause)
        else -> LoginEmailVerificationResult.Failure(
            CoreFailure.Unknown(IllegalStateException("Unsupported verification-code result: $result"))
        )
    }

    override suspend fun revertSession(newSessionUserId: UserId?, previousSessionUserId: UserId?) {
        newSessionUserId?.let {
            coreLogic.getSessionScope(it).logout(LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
            coreLogic.getGlobalScope().deleteSession(it)
        }
        coreLogic.getGlobalScope().session.updateCurrentSession(previousSessionUserId)
    }

    override suspend fun parseBackendConfig(input: String): String? = withContext(dispatchers.io()) {
        input.toBackendConfigUrl()
    }

    override suspend fun configureBackend(request: String): LoginEmailBackendResult<ServerConfig.Links> =
        withContext(dispatchers.io()) {
            when (val result = getServerConfigUseCase?.value?.invoke(request)) {
                is GetServerConfigResult.Success -> {
                    CustomTabsHelper.setBackendWebsiteUrl(result.serverConfigLinks.website)
                    SupportUrlResolver.setBaseUrl(result.serverConfigLinks.website)
                    globalDataStore?.let { BackendSupportConfig.storeFromServerLinks(it.value, result.serverConfigLinks) }
                    LoginEmailBackendResult.Success(result.serverConfigLinks)
                }
                is GetServerConfigResult.Failure.Generic, null -> LoginEmailBackendResult.Failure
            }
        }
}

internal class AndroidLoginEmailTimer(private val timer: CountdownTimer) : LoginEmailTimer {
    override suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit) =
        timer.start(seconds, onUpdate, onFinish)
}

@Suppress("LongParameterList")
class LoginEmailViewModelHostFactory @Inject constructor(
    addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    userDataStoreProvider: UserDataStoreProvider,
    @KaliumCoreLogic coreLogic: CoreLogic,
    dispatchers: DispatcherProvider,
    private val defaultServerConfig: ServerConfig.Links,
    @DefaultWebSocketEnabledByDefault defaultWebSocketEnabledByDefault: Boolean,
    @Named("isDefaultBackendConfigured") private val isDefaultBackendConfigured: Boolean,
    getServerConfigUseCase: Lazy<GetServerConfigUseCase>,
    globalDataStore: Lazy<GlobalDataStore>,
) {
    private val gateway = KaliumLoginEmailGateway(
        addAuthenticatedUser,
        coreLogic,
        LoginViewModelExtension(clientScopeProviderFactory, userDataStoreProvider),
        dispatchers,
        defaultWebSocketEnabledByDefault,
        getServerConfigUseCase,
        globalDataStore,
    )

    fun create(loginNavArgs: LoginNavArgs, savedStateHandle: SavedStateHandle): AppLoginEmailViewModel {
        val custom = loginNavArgs.loginPasswordPath?.customServerConfig
        val prefilled = loginNavArgs.userHandle
        return LoginEmailViewModel(
            input = LoginEmailInput(
                serverConfig = custom ?: defaultServerConfig,
                isBackendConfigured = isDefaultBackendConfigured || custom?.api?.isNotBlank() == true,
                preFilledIdentifier = prefilled?.userIdentifier,
                identifierEditable = prefilled?.editable ?: true,
                domainClaimedByOrg = loginNavArgs.loginPasswordPath?.isDomainClaimedByOrg,
            ),
            savedInputStore = SavedStateLoginSavedInputStore(savedStateHandle),
            gateway = gateway,
            resendCodeTimer = AndroidLoginEmailTimer(CountdownTimer()),
        )
    }
}
