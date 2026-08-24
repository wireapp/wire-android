/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.newauthentication.login

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import com.wire.android.appLogger
import com.wire.android.config.ServerConfigProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ClientScopeProvider
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.authentication.login.DomainClaimedByOrg
import com.wire.android.ui.authentication.login.LoginNavArgs
import com.wire.android.ui.authentication.login.LoginPasswordPath
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.LoginViewModelExtension
import com.wire.android.ui.authentication.login.sso.LoginSSOViewModelExtension
import com.wire.android.ui.authentication.login.sso.ReplaceRetainedSsoSessionResult
import com.wire.android.ui.authentication.login.sso.ssoCodeWithPrefix
import com.wire.android.ui.authentication.toBackendConfigUrl
import com.wire.android.util.BackendSupportConfig
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.SupportUrlResolver
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.session.StoreSessionParam
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import com.wire.kalium.logic.feature.auth.EnterpriseLoginResult
import com.wire.kalium.logic.feature.auth.LoginRedirectPath
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.auth.sso.SSOInitiateLoginResult
import com.wire.kalium.logic.feature.auth.sso.SSOLoginSessionResult
import com.wire.kalium.logic.feature.auth.sso.ValidateSSOCodeUseCase.Companion.SSO_CODE_WIRE_PREFIX
import com.wire.kalium.logic.feature.backup.RestoreCryptoStateResult
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

typealias AppNewLoginViewModel =
    NewLoginViewModel<ServerConfig.Links, CoreFailure, UserId, SSOFailureCodes, StoreSessionParam, String>
typealias AppNewLoginScreenState = NewLoginScreenState<ServerConfig.Links, CoreFailure, SSOFailureCodes>
typealias AppNewLoginFlowState = NewLoginFlowState<ServerConfig.Links, CoreFailure, SSOFailureCodes>
typealias AppNewLoginAction = NewLoginAction<ServerConfig.Links, UserId>
typealias AppNewLoginDialogError = NewLoginFlowState.Error.DialogError<CoreFailure, SSOFailureCodes>

internal class SavedStateNewLoginStore(private val savedStateHandle: SavedStateHandle) : NewLoginSavedStateStore {
    override var userIdentifier: String?
        get() = savedStateHandle[USER_IDENTIFIER_KEY]
        set(value) {
            savedStateHandle[USER_IDENTIFIER_KEY] = value
        }
    override var pendingSsoIdentityProviderId: String?
        get() = savedStateHandle[PENDING_SSO_IDENTITY_PROVIDER_ID_KEY]
        set(value) {
            if (value == null) {
                savedStateHandle.remove<String>(PENDING_SSO_IDENTITY_PROVIDER_ID_KEY)
            } else {
                savedStateHandle[PENDING_SSO_IDENTITY_PROVIDER_ID_KEY] = value
            }
        }

    private companion object {
        const val USER_IDENTIFIER_KEY = "user_identifier"
        const val PENDING_SSO_IDENTITY_PROVIDER_ID_KEY = "pending_sso_identity_provider_id"
    }
}

@Suppress("TooManyFunctions")
internal class KaliumNewLoginGateway(
    private val validateEmailOrSsoCode: ValidateEmailOrSSOCodeUseCase,
    private val coreLogic: CoreLogic,
    private val loginExtension: LoginViewModelExtension,
    private val ssoExtension: LoginSSOViewModelExtension,
    private val dispatchers: DispatcherProvider,
) : NewLoginGateway<ServerConfig.Links, CoreFailure, UserId, StoreSessionParam> {
    override fun validateIdentifier(input: String): NewLoginIdentifierValidation = when (validateEmailOrSsoCode(input)) {
        ValidateEmailOrSSOCodeUseCase.Result.InvalidInput -> NewLoginIdentifierValidation.Invalid
        ValidateEmailOrSSOCodeUseCase.Result.ValidEmail -> NewLoginIdentifierValidation.Email
        ValidateEmailOrSSOCodeUseCase.Result.ValidSSOCode -> NewLoginIdentifierValidation.SsoCode
    }

    override suspend fun enterpriseLogin(
        serverConfig: ServerConfig.Links,
        email: String,
    ): NewLoginEnterpriseResult<ServerConfig.Links, CoreFailure> {
        var mapped: NewLoginEnterpriseResult<ServerConfig.Links, CoreFailure>? = null
        withContext(dispatchers.io()) {
            ssoExtension.withAuthenticationScope(
                serverConfig = serverConfig,
                onAuthScopeFailure = { mapped = NewLoginEnterpriseResult.Failure(it.toFeatureFailure()) },
                onSuccess = { authScope ->
                    mapped = when (val result = authScope.getLoginFlowForDomainUseCase(email)) {
                        is EnterpriseLoginResult.Failure.Generic ->
                            NewLoginEnterpriseResult.Failure(NewLoginFailure.Generic(result.coreFailure))
                        EnterpriseLoginResult.Failure.NotSupported -> NewLoginEnterpriseResult.NotSupported
                        is EnterpriseLoginResult.Success -> result.loginRedirectPath.toFeatureResult()
                    }
                },
            )
        }
        return checkNotNull(mapped)
    }

    override suspend fun initiateSso(
        serverConfig: ServerConfig.Links,
        code: String,
        cookieLabel: String?,
    ): NewLoginSsoInitiationResult<CoreFailure> {
        var mapped: NewLoginSsoInitiationResult<CoreFailure>? = null
        withContext(dispatchers.io()) {
            ssoExtension.initiateSSO(
                serverConfig = serverConfig,
                ssoCode = code,
                cookieLabel = cookieLabel,
                onAuthScopeFailure = { mapped = NewLoginSsoInitiationResult.Failure(it.toFeatureFailure()) },
                onSSOInitiateFailure = { mapped = it.toFeatureResult() },
                onSuccess = { mapped = NewLoginSsoInitiationResult.Success(it) },
            )
        }
        return checkNotNull(mapped)
    }

    override suspend fun fetchDefaultSsoCode(serverConfig: ServerConfig.Links): NewLoginDefaultSsoCodeResult<CoreFailure> {
        var mapped: NewLoginDefaultSsoCodeResult<CoreFailure>? = null
        withContext(dispatchers.io()) {
            ssoExtension.fetchDefaultSSOCode(
                serverConfig = serverConfig,
                onAuthScopeFailure = {
                    appLogger.e("$TAG Failed to create auth scope for SSO settings: $it")
                    mapped = NewLoginDefaultSsoCodeResult.Failure(it.toFeatureFailure())
                },
                onFetchSSOSettingsFailure = {
                    appLogger.e("$TAG Failed to fetch SSO settings: $it")
                    mapped = NewLoginDefaultSsoCodeResult.Failure(NewLoginFailure.Generic(it.coreFailure))
                },
                onSuccess = { mapped = NewLoginDefaultSsoCodeResult.Success(it) },
            )
        }
        return checkNotNull(mapped)
    }

    override suspend fun establishSession(
        cookie: String,
        serverConfigId: String,
        ssoIdentityProviderId: String?,
        consumeNomadServiceUrl: () -> String?,
        consumeCookieLabel: () -> String?,
    ): NewLoginSessionResult<CoreFailure, UserId, StoreSessionParam> {
        var mapped: NewLoginSessionResult<CoreFailure, UserId, StoreSessionParam>? = null
        withContext(dispatchers.io()) {
            ssoExtension.establishSSOSession(
                cookie = cookie,
                serverConfigId = serverConfigId,
                ssoIdentityProviderId = ssoIdentityProviderId,
                consumeNomadServiceUrl = consumeNomadServiceUrl,
                consumeCookieLabel = consumeCookieLabel,
                onAuthScopeFailure = { mapped = NewLoginSessionResult.Failure(it.toFeatureFailure()) },
                onSSOLoginFailure = { mapped = it.toFeatureResult() },
                onAddAuthenticatedUserFailure = { mapped = it.toFeatureResult() },
                onSsoIdentityChanged = {
                    mapped = NewLoginSessionResult.IdentityChanged(it, isNomadSession = it.nomadServiceUrl != null)
                },
                onSuccess = { mapped = NewLoginSessionResult.Success(it) },
            )
        }
        return checkNotNull(mapped)
    }

    override suspend fun replaceRetainedSession(
        session: StoreSessionParam,
    ): NewLoginReplaceSessionResult<CoreFailure, UserId> = withContext(dispatchers.io()) {
        when (val result = ssoExtension.replaceRetainedSsoSession(session)) {
            is ReplaceRetainedSsoSessionResult.Success -> NewLoginReplaceSessionResult.Success(result.userId)
            is ReplaceRetainedSsoSessionResult.Failure -> when (val failure = result.cause) {
                is AddAuthenticatedUserUseCase.Result.Failure.Generic -> NewLoginReplaceSessionResult.Failure(failure.genericFailure)
                AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged -> NewLoginReplaceSessionResult.SsoIdentityChanged
                AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
                AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> NewLoginReplaceSessionResult.UserAlreadyExists
            }
        }
    }

    override suspend fun registerClient(
        userId: UserId,
        setLastDeviceIdOnSuccess: Boolean,
    ): NewLoginRegisterClientResult<CoreFailure> = withContext(dispatchers.io()) {
        when (val result = loginExtension.registerClient(userId, null)) {
            is RegisterClientResult.Success -> {
                if (setLastDeviceIdOnSuccess) coreLogic.getSessionScope(userId).backup.setLastDeviceId(result.client.id.value)
                NewLoginRegisterClientResult.Success(loginExtension.isInitialSyncCompleted(userId))
            }
            is RegisterClientResult.E2EICertificateRequired -> NewLoginRegisterClientResult.E2EICertificateRequired
            RegisterClientResult.Failure.TooManyClients -> NewLoginRegisterClientResult.TooManyDevices
            is RegisterClientResult.Failure.Generic -> NewLoginRegisterClientResult.Failure(result.genericFailure)
            is RegisterClientResult.Failure.InvalidCredentials,
            RegisterClientResult.Failure.PasswordAuthRequired -> NewLoginRegisterClientResult.Failure(
                CoreFailure.Unknown(IllegalStateException(result::class.simpleName ?: "Unknown"))
            )
        }
    }

    @Suppress("ReturnCount")
    override suspend fun restoreCryptoState(userId: UserId): NewLoginRestoreResult<CoreFailure> {
        val restoreResult = try {
            withContext(dispatchers.io()) { coreLogic.getSessionScope(userId).backup.restoreCryptoState() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IllegalStateException) {
            return cryptoRestoreUnavailableAfterConcurrentLogout(userId, exception)
        } catch (exception: IOException) {
            return cryptoRestoreUnavailableAfterConcurrentLogout(userId, exception)
        } catch (exception: SQLiteException) {
            return cryptoRestoreUnavailableAfterConcurrentLogout(userId, exception)
        }
        return when (restoreResult) {
            RestoreCryptoStateResult.Success ->
                NewLoginRestoreResult.Success(loginExtension.isInitialSyncCompleted(userId))
            RestoreCryptoStateResult.NoBackupAvailable -> NewLoginRestoreResult.NoBackupAvailable
            RestoreCryptoStateResult.Failure -> {
                appLogger.e("$TAG Failed to restore crypto state during SSO login")
                NewLoginRestoreResult.Failure(CoreFailure.Unknown(Exception("Failed to restore crypto state")))
            }
        }
    }

    override suspend fun revertSession(userId: UserId) {
        try {
            coreLogic.getSessionScope(userId).logout(LogoutReason.SELF_HARD_LOGOUT, waitUntilCompletes = true)
            coreLogic.getGlobalScope().deleteSession(userId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IllegalStateException) {
            ignoreConcurrentLogout(userId, exception)
        } catch (exception: IOException) {
            ignoreConcurrentLogout(userId, exception)
        } catch (exception: SQLiteException) {
            ignoreConcurrentLogout(userId, exception)
        }
    }

    override fun logSessionContinuation(isNomadFlow: Boolean) {
        if (isNomadFlow) {
            appLogger.i("$TAG Nomad flow, attempting crypto state restore")
        } else {
            appLogger.i("$TAG Not a nomad flow, proceeding with regular login")
        }
    }

    private suspend fun isSessionStillValid(userId: UserId): Boolean =
        (coreLogic.getGlobalScope().doesValidSessionExist(userId) as? DoesValidSessionExistResult.Success)
            ?.doesValidSessionExist == true

    private suspend fun cryptoRestoreUnavailableAfterConcurrentLogout(
        userId: UserId,
        exception: Exception,
    ): NewLoginRestoreResult<CoreFailure> {
        if (isSessionStillValid(userId)) throw exception
        appLogger.w("$TAG Crypto restore interrupted by concurrent logout: ${exception.message}")
        return NewLoginRestoreResult.SessionUnavailable
    }

    private suspend fun ignoreConcurrentLogout(userId: UserId, exception: Exception) {
        if (isSessionStillValid(userId)) throw exception
        appLogger.w("$TAG Failed to revert SSO session, may have been already logged out: ${exception.message}")
    }

    private fun LoginRedirectPath.toFeatureResult(): NewLoginEnterpriseResult<ServerConfig.Links, CoreFailure> = when (this) {
        is LoginRedirectPath.SSO -> NewLoginEnterpriseResult.Sso(
            code = ssoCode.ssoCodeWithPrefix(),
            identityProviderId = ssoCode.removePrefix(SSO_CODE_WIRE_PREFIX),
        )
        is LoginRedirectPath.CustomBackend -> NewLoginEnterpriseResult.CustomBackend(serverLinks)
        is LoginRedirectPath.Default -> NewLoginEnterpriseResult.Password(isCloudAccountCreationPossible)
        is LoginRedirectPath.NoRegistration -> NewLoginEnterpriseResult.Password(isCloudAccountCreationPossible)
        is LoginRedirectPath.ExistingAccountWithClaimedDomain ->
            NewLoginEnterpriseResult.Password(isCloudAccountCreationPossible, claimedDomain = domain)
    }

    private fun AutoVersionAuthScopeUseCase.Result.Failure.toFeatureFailure(): NewLoginFailure<CoreFailure> = when (this) {
        is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> NewLoginFailure.Generic(genericFailure)
        AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> NewLoginFailure.ClientUpdateRequired
        AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> NewLoginFailure.ServerVersionNotSupported
    }

    private fun SSOInitiateLoginResult.Failure.toFeatureResult(): NewLoginSsoInitiationResult<CoreFailure> = when (this) {
        SSOInitiateLoginResult.Failure.InvalidCodeFormat -> NewLoginSsoInitiationResult.InvalidCodeFormat
        SSOInitiateLoginResult.Failure.InvalidCode -> NewLoginSsoInitiationResult.InvalidCode
        is SSOInitiateLoginResult.Failure.Generic -> NewLoginSsoInitiationResult.Failure(NewLoginFailure.Generic(genericFailure))
        SSOInitiateLoginResult.Failure.InvalidRedirect -> NewLoginSsoInitiationResult.Failure(
            NewLoginFailure.Generic(CoreFailure.Unknown(IllegalArgumentException("Invalid Redirect")))
        )
    }

    private fun SSOLoginSessionResult.Failure.toFeatureResult(): NewLoginSessionResult<CoreFailure, UserId, StoreSessionParam> =
        when (this) {
            SSOLoginSessionResult.Failure.InvalidCookie -> NewLoginSessionResult.InvalidCookie
            is SSOLoginSessionResult.Failure.Generic -> NewLoginSessionResult.Failure(NewLoginFailure.Generic(genericFailure))
        }

    private fun AddAuthenticatedUserUseCase.Result.Failure.toFeatureResult():
            NewLoginSessionResult<CoreFailure, UserId, StoreSessionParam> = when (this) {
        is AddAuthenticatedUserUseCase.Result.Failure.Generic ->
            NewLoginSessionResult.Failure(NewLoginFailure.Generic(genericFailure))
        AddAuthenticatedUserUseCase.Result.Failure.UserAlreadyExists,
        AddAuthenticatedUserUseCase.Result.Failure.SsoIdentityChanged,
        AddAuthenticatedUserUseCase.Result.Failure.NomadSingleUserViolation -> NewLoginSessionResult.UserAlreadyExists
    }

    private companion object {
        const val TAG = "[NewLoginViewModel]"
    }
}

internal class AndroidNewLoginBackendGateway(
    private val getServerConfigUseCase: Lazy<GetServerConfigUseCase>?,
    private val globalDataStore: Lazy<GlobalDataStore>?,
    private val dispatchers: DispatcherProvider,
) : NewLoginBackendGateway<ServerConfig.Links, String> {
    override suspend fun parse(input: String): String? = withContext(dispatchers.io()) { input.toBackendConfigUrl() }

    override suspend fun configure(request: String): NewLoginBackendResult<ServerConfig.Links> = withContext(dispatchers.io()) {
        when (val result = getServerConfigUseCase?.value?.invoke(request)) {
            is GetServerConfigResult.Success -> {
                select(result.serverConfigLinks)
                globalDataStore?.let { BackendSupportConfig.storeFromServerLinks(it.value, result.serverConfigLinks) }
                NewLoginBackendResult.Success(result.serverConfigLinks)
            }
            is GetServerConfigResult.Failure.Generic -> {
                appLogger.e("[NewLoginViewModel] Failed to load backend config from setup screen: ${result.genericFailure}")
                NewLoginBackendResult.Failure
            }
            null -> NewLoginBackendResult.Failure
        }
    }

    override fun select(serverConfig: ServerConfig.Links) {
        CustomTabsHelper.setBackendWebsiteUrl(serverConfig.website)
        SupportUrlResolver.setBaseUrl(serverConfig.website)
    }

    override fun clear() {
        CustomTabsHelper.setBackendWebsiteUrl(null)
        SupportUrlResolver.setBaseUrl(null)
    }
}

@Suppress("LongParameterList")
class NewLoginViewModelHostFactory @Inject constructor(
    validateEmailOrSSOCode: ValidateEmailOrSSOCodeUseCase,
    @KaliumCoreLogic coreLogic: CoreLogic,
    addAuthenticatedUser: AddAuthenticatedUserUseCase,
    clientScopeProviderFactory: ClientScopeProvider.Factory,
    userDataStoreProvider: UserDataStoreProvider,
    dispatchers: DispatcherProvider,
    private val defaultServerConfig: ServerConfig.Links,
    @Named("ssoCodeConfig") private val defaultSsoCodeConfig: String,
    @Named("isDefaultBackendConfigured") private val isDefaultBackendConfigured: Boolean,
    @DefaultWebSocketEnabledByDefault defaultWebSocketEnabledByDefault: Boolean,
    getServerConfigUseCase: Lazy<GetServerConfigUseCase>,
    globalDataStore: Lazy<GlobalDataStore>,
) {
    private val loginExtension = LoginViewModelExtension(clientScopeProviderFactory, userDataStoreProvider)
    private val gateway = KaliumNewLoginGateway(
        validateEmailOrSSOCode,
        coreLogic,
        loginExtension,
        LoginSSOViewModelExtension(addAuthenticatedUser, coreLogic, defaultWebSocketEnabledByDefault),
        dispatchers,
    )
    private val backendGateway = AndroidNewLoginBackendGateway(getServerConfigUseCase, globalDataStore, dispatchers)

    fun create(loginNavArgs: LoginNavArgs, savedStateHandle: SavedStateHandle): AppNewLoginViewModel {
        val custom = loginNavArgs.loginPasswordPath?.customServerConfig
        return NewLoginViewModel(
            input = NewLoginInput(
                defaultServerConfig = defaultServerConfig,
                initialCustomServerConfig = custom,
                emptyServerConfig = ServerConfigProvider.EmptyServerConfig,
                isDefaultServerConfigured = isDefaultBackendConfigured && defaultServerConfig.api.isNotBlank(),
                isInitialCustomServerConfigured = custom?.api?.isNotBlank() == true,
                showBackendConfigSuccess = loginNavArgs.showBackendConfigSuccess,
                preFilledIdentifier = loginNavArgs.userHandle?.userIdentifier,
                managedSsoCode = defaultSsoCodeConfig.takeIf(String::isNotEmpty)?.ssoCodeWithPrefix(),
                pendingNomadServiceUrl = loginNavArgs.ssoCodeAutoLogin?.nomadServiceUrl,
                pendingCookieLabel = loginNavArgs.ssoCodeAutoLogin?.cookieLabel,
            ),
            gateway = gateway,
            backendGateway = backendGateway,
            savedStateStore = SavedStateNewLoginStore(savedStateHandle),
        )
    }
}

internal fun LoginNavArgs.toNewLoginNavigationInput(): NewLoginNavigationInput<ServerConfig.Links> {
    val custom = loginPasswordPath?.customServerConfig
    return NewLoginNavigationInput(
        customServerConfig = custom,
        isCustomServerConfigured = custom?.api?.isNotBlank() == true,
        showBackendConfigSuccess = showBackendConfigSuccess,
        preFilledIdentifier = userHandle?.userIdentifier,
        pendingNomadServiceUrl = ssoCodeAutoLogin?.nomadServiceUrl,
        pendingCookieLabel = ssoCodeAutoLogin?.cookieLabel,
    )
}

internal fun DeepLinkResult.SSOLogin.toNewLoginSsoCallback(): NewLoginSsoCallback<SSOFailureCodes> = when (this) {
    is DeepLinkResult.SSOLogin.Success -> NewLoginSsoCallback.Success(cookie, serverConfigId)
    is DeepLinkResult.SSOLogin.Failure -> NewLoginSsoCallback.Failure(ssoError)
}

internal fun AppNewLoginDialogError.toLoginStateDialogError(): LoginState.Error.DialogError<CoreFailure, SSOFailureCodes> =
    when (this) {
        NewLoginFlowState.Error.DialogError.ServerVersionNotSupported -> LoginState.Error.DialogError.ServerVersionNotSupported
        NewLoginFlowState.Error.DialogError.ClientUpdateRequired -> LoginState.Error.DialogError.ClientUpdateRequired
        is NewLoginFlowState.Error.DialogError.SSOResultFailure -> LoginState.Error.DialogError.SSOResultError(result)
        NewLoginFlowState.Error.DialogError.InvalidSSOCode -> LoginState.Error.DialogError.InvalidSSOCodeError
        NewLoginFlowState.Error.DialogError.InvalidSSOCookie -> LoginState.Error.DialogError.InvalidSSOCookie
        NewLoginFlowState.Error.DialogError.UserAlreadyExists -> LoginState.Error.DialogError.UserAlreadyExists
        is NewLoginFlowState.Error.DialogError.GenericError -> LoginState.Error.DialogError.GenericError(failure)
    }

internal fun NewLoginAction.EmailPassword<ServerConfig.Links>.toLoginPasswordPath() = LoginPasswordPath(
    customServerConfig = serverConfig,
    isCloudAccountCreationPossible = isCloudAccountCreationPossible,
    isDomainClaimedByOrg = claimedDomain?.let(DomainClaimedByOrg::Claimed) ?: DomainClaimedByOrg.NotClaimed,
)
