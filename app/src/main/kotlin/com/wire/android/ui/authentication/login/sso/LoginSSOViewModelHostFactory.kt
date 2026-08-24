/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.authentication.login.sso

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import com.wire.android.appLogger
import com.wire.android.config.DefaultServerConfig
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.SavedStateLoginSavedInputStore
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.DomainLookupUseCase
import com.wire.kalium.logic.feature.auth.ValidateEmailUseCase
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.FetchSSOSettingsUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.backup.RestoreCryptoStateResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import dev.zacsweers.metro.Inject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

typealias AppLoginSSOViewModel =
    LoginSSOViewModel<ServerConfig.Links, CoreFailure, UserId, com.wire.android.util.deeplink.SSOFailureCodes, StoreSessionParam>
typealias AppLoginSSOState =
    LoginSSOState<ServerConfig.Links, CoreFailure, UserId, com.wire.android.util.deeplink.SSOFailureCodes>

@Suppress("TooManyFunctions")
internal class KaliumLoginSSOGateway(
    private val validateEmailUseCase: ValidateEmailUseCase,
    private val coreLogic: CoreLogic,
    private val loginExtension: LoginViewModelExtension,
    private val ssoExtension: LoginSSOViewModelExtension,
    private val dispatchers: DispatcherProvider,
) : LoginSSOGateway<ServerConfig.Links, CoreFailure, UserId, StoreSessionParam> {

    override fun isEmail(value: String): Boolean = validateEmailUseCase(value)

    override suspend fun initiateSSO(
        serverConfig: ServerConfig.Links,
        ssoCode: String,
        cookieLabel: String?,
    ): LoginSSOInitiationResult<CoreFailure> {
        var mapped: LoginSSOInitiationResult<CoreFailure>? = null
        ssoExtension.initiateSSO(
            serverConfig = serverConfig,
            ssoCode = ssoCode,
            cookieLabel = cookieLabel,
            onAuthScopeFailure = { mapped = LoginSSOInitiationResult.Failure(it.toFeatureFailure()) },
            onSSOInitiateFailure = { mapped = it.toFeatureResult() },
            onSuccess = { mapped = LoginSSOInitiationResult.Success(it) },
        )
        return checkNotNull(mapped)
    }

    override suspend fun lookupDomain(
        email: () -> String,
    ): LoginSSODomainLookupResult<ServerConfig.Links, CoreFailure> {
        val authScope = when (val result = coreLogic.versionedAuthenticationScope(DefaultServerConfig)(null)) {
            is AutoVersionAuthScopeUseCase.Result.Success -> result.authenticationScope
            is AutoVersionAuthScopeUseCase.Result.Failure -> return LoginSSODomainLookupResult.AuthenticationUnavailable
        }
        return when (val result = authScope.domainLookup(email())) {
            is DomainLookupUseCase.Result.Success -> LoginSSODomainLookupResult.Success(result.serverLinks)
            is DomainLookupUseCase.Result.Failure -> LoginSSODomainLookupResult.Failure(result.coreFailure)
        }
    }

    override suspend fun fetchDefaultSSOCode(serverConfig: ServerConfig.Links): LoginSSODefaultCodeResult<CoreFailure> {
        var mapped: LoginSSODefaultCodeResult<CoreFailure>? = null
        ssoExtension.fetchDefaultSSOCode(
            serverConfig = serverConfig,
            onAuthScopeFailure = { mapped = LoginSSODefaultCodeResult.Failure(it.toFeatureFailure()) },
            onFetchSSOSettingsFailure = { mapped = LoginSSODefaultCodeResult.Unavailable },
            onSuccess = { mapped = LoginSSODefaultCodeResult.Success(it) },
        )
        return checkNotNull(mapped)
    }

    override suspend fun establishSession(
        cookie: String,
        serverConfigId: String,
        consumeNomadServiceUrl: () -> String?,
        consumeCookieLabel: () -> String?,
    ): LoginSSOSessionResult<CoreFailure, UserId, StoreSessionParam> {
        var mapped: LoginSSOSessionResult<CoreFailure, UserId, StoreSessionParam>? = null
        ssoExtension.establishSSOSession(
            cookie = cookie,
            serverConfigId = serverConfigId,
            consumeNomadServiceUrl = consumeNomadServiceUrl,
            consumeCookieLabel = consumeCookieLabel,
            onAuthScopeFailure = { mapped = LoginSSOSessionResult.Failure(it.toFeatureFailure()) },
            onSSOLoginFailure = { mapped = it.toFeatureResult() },
            onAddAuthenticatedUserFailure = { result ->
                mapped = when (result) {
                    is AddAuthenticatedUserUseCase.Result.Failure.Generic ->
                        LoginSSOSessionResult.Failure(LoginSSOFailure.Generic(result.genericFailure))
                    AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
                    AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
                    AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation ->
                        LoginSSOSessionResult.UserAlreadyExists
                }
            },
            onSsoIdentityChanged = { session ->
                mapped = LoginSSOSessionResult.IdentityChanged(session, session.nomadServiceUrl != null)
            },
            onSuccess = { mapped = LoginSSOSessionResult.Success(it) },
        )
        return checkNotNull(mapped)
    }

    override suspend fun replaceRetainedSession(
        session: StoreSessionParam,
    ): LoginSSOReplaceSessionResult<CoreFailure, UserId> =
        when (val result = ssoExtension.replaceRetainedSsoSession(session)) {
            is ReplaceRetainedSsoSessionResult.Success -> LoginSSOReplaceSessionResult.Success(result.userId)
            is ReplaceRetainedSsoSessionResult.Failure -> when (val cause = result.cause) {
                is AddAuthenticatedUserUseCase.Result.Failure.Generic -> LoginSSOReplaceSessionResult.Failure(cause.genericFailure)
                AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
                AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
                AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation ->
                    LoginSSOReplaceSessionResult.UserAlreadyExists
            }
        }

    override fun logSessionContinuation(isNomadFlow: Boolean) {
        if (isNomadFlow) {
            appLogger.i("$TAG Nomad flow, attempting crypto state restore")
        } else {
            appLogger.i("$TAG Not a nomad flow, proceeding with regular login")
        }
    }

    override suspend fun registerClient(
        userId: UserId,
        setLastDeviceIdOnSuccess: Boolean,
    ): LoginSSORegisterClientResult<CoreFailure> {
        val result = withContext(dispatchers.io()) {
            loginExtension.registerClient(userId, password = null)
        }
        return when (result) {
            is RegisterClientResult.Success -> {
                if (setLastDeviceIdOnSuccess) {
                    coreLogic.getSessionScope(userId).backup.setLastDeviceId(result.client.id.value)
                }
                LoginSSORegisterClientResult.Success(loginExtension.isInitialSyncCompleted(userId))
            }
            is RegisterClientResult.E2EICertificateRequired ->
                LoginSSORegisterClientResult.E2EICertificateRequired(loginExtension.isInitialSyncCompleted(userId))
            RegisterClientResult.Failure.TooManyClients -> LoginSSORegisterClientResult.TooManyDevices
            is RegisterClientResult.Failure.InvalidCredentials -> LoginSSORegisterClientResult.InvalidCredentials
            RegisterClientResult.Failure.PasswordAuthRequired -> LoginSSORegisterClientResult.PasswordRequired
            is RegisterClientResult.Failure.Generic -> LoginSSORegisterClientResult.Failure(result.genericFailure)
        }
    }

    @Suppress("ThrowsCount", "TooGenericExceptionCaught")
    override suspend fun restoreCryptoState(userId: UserId): LoginSSORestoreResult<CoreFailure> {
        val restoreResult = try {
            withContext(dispatchers.io()) { coreLogic.getSessionScope(userId).backup.restoreCryptoState() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            when (exception) {
                is IllegalStateException, is IOException, is SQLiteException -> {
                    if (isSessionStillValid(userId)) throw exception
                    appLogger.w("$TAG Crypto restore interrupted by concurrent logout: ${exception.message}")
                    return LoginSSORestoreResult.SessionUnavailable
                }
                else -> throw exception
            }
        }
        return when (restoreResult) {
            RestoreCryptoStateResult.Success ->
                LoginSSORestoreResult.Success(loginExtension.isInitialSyncCompleted(userId))
            RestoreCryptoStateResult.NoBackupAvailable -> LoginSSORestoreResult.NoBackupAvailable
            RestoreCryptoStateResult.Failure -> {
                appLogger.e("$TAG Failed to restore crypto state during SSO login")
                LoginSSORestoreResult.Failure(CoreFailure.Unknown(Exception("Failed to restore crypto state")))
            }
        }
    }

    @Suppress("ThrowsCount", "TooGenericExceptionCaught")
    override suspend fun revertSession(userId: UserId) {
        try {
            coreLogic.getSessionScope(userId).logout(LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
            coreLogic.getGlobalScope().deleteSession(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            when (exception) {
                is IllegalStateException, is IOException, is SQLiteException -> {
                    if (isSessionStillValid(userId)) throw exception
                    appLogger.w("$TAG Failed to revert SSO session, may have been already logged out: ${exception.message}")
                }
                else -> throw exception
            }
        }
    }

    private suspend fun isSessionStillValid(userId: UserId): Boolean =
        (coreLogic.getGlobalScope().doesValidSessionExist(userId) as? DoesValidSessionExistResult.Success)
            ?.doesValidSessionExist == true

    private fun AutoVersionAuthScopeUseCase.Result.Failure.toFeatureFailure(): LoginSSOFailure<CoreFailure> = when (this) {
        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> LoginSSOFailure.Generic(genericFailure)
        AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> LoginSSOFailure.ClientUpdateRequired
        AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> LoginSSOFailure.ServerVersionNotSupported
    }

    private fun SSOInitiateLoginResult.Failure.toFeatureResult(): LoginSSOInitiationResult<CoreFailure> = when (this) {
        SSOInitiateLoginResult.Failure.InvalidCodeFormat -> LoginSSOInitiationResult.InvalidCodeFormat
        SSOInitiateLoginResult.Failure.InvalidCode -> LoginSSOInitiationResult.InvalidCode
        is SSOInitiateLoginResult.Failure.Generic -> LoginSSOInitiationResult.Failure(LoginSSOFailure.Generic(genericFailure))
        SSOInitiateLoginResult.Failure.InvalidRedirect -> LoginSSOInitiationResult.Failure(
            LoginSSOFailure.Generic(CoreFailure.Unknown(IllegalArgumentException("Invalid Redirect")))
        )
    }

    private fun SSOLoginSessionResult.Failure.toFeatureResult(): LoginSSOSessionResult<CoreFailure, UserId, StoreSessionParam> =
        when (this) {
            SSOLoginSessionResult.Failure.InvalidCookie -> LoginSSOSessionResult.InvalidCookie
            is SSOLoginSessionResult.Failure.Generic ->
                LoginSSOSessionResult.Failure(LoginSSOFailure.Generic(genericFailure))
        }

    private companion object {
        const val TAG = "[LoginSSOViewModel]"
    }
}

class LoginSSOViewModelHostFactory @Inject constructor(
    validateEmailUseCase: ValidateEmailUseCase,
    @KaliumCoreLogic coreLogic: CoreLogic,
    addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    userDataStoreProvider: UserDataStoreProvider,
    defaultServerConfig: ServerConfig.Links,
    @DefaultWebSocketEnabledByDefault defaultWebSocketEnabledByDefault: Boolean,
    dispatchers: DispatcherProvider,
) {
    private val defaultLinks = defaultServerConfig
    private val gateway = KaliumLoginSSOGateway(
        validateEmailUseCase = validateEmailUseCase,
        coreLogic = coreLogic,
        loginExtension = LoginViewModelExtension(clientScopeProviderFactory, userDataStoreProvider),
        ssoExtension = LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic, defaultWebSocketEnabledByDefault),
        dispatchers = dispatchers,
    )

    fun create(loginNavArgs: LoginNavArgs, savedStateHandle: SavedStateHandle): AppLoginSSOViewModel =
        LoginSSOViewModel(
            input = LoginSSOInput(
                serverConfig = loginNavArgs.loginPasswordPath?.customServerConfig ?: defaultLinks,
                pendingNomadServiceUrl = loginNavArgs.ssoCodeAutoLogin?.nomadServiceUrl,
                pendingCookieLabel = loginNavArgs.ssoCodeAutoLogin?.cookieLabel,
            ),
            savedInputStore = SavedStateLoginSavedInputStore(savedStateHandle),
            gateway = gateway,
        )
}
