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

data class NewLoginInput<LinksT>(
    val defaultServerConfig: LinksT,
    val initialCustomServerConfig: LinksT?,
    val emptyServerConfig: LinksT,
    val isDefaultServerConfigured: Boolean,
    val isInitialCustomServerConfigured: Boolean,
    val showBackendConfigSuccess: Boolean,
    val preFilledIdentifier: String?,
    val managedSsoCode: String?,
    val pendingNomadServiceUrl: String?,
    val pendingCookieLabel: String?,
)

data class NewLoginNavigationInput<LinksT>(
    val customServerConfig: LinksT?,
    val isCustomServerConfigured: Boolean,
    val showBackendConfigSuccess: Boolean,
    val preFilledIdentifier: String?,
    val pendingNomadServiceUrl: String?,
    val pendingCookieLabel: String?,
)

enum class NewLoginIdentifierValidation { Invalid, Email, SsoCode }

sealed interface NewLoginFailure<out FailureT> {
    data class Generic<FailureT>(val failure: FailureT) : NewLoginFailure<FailureT>
    data object ClientUpdateRequired : NewLoginFailure<Nothing>
    data object ServerVersionNotSupported : NewLoginFailure<Nothing>
}

sealed interface NewLoginEnterpriseResult<out LinksT, out FailureT> {
    data object NotSupported : NewLoginEnterpriseResult<Nothing, Nothing>
    data class Password(val isCloudAccountCreationPossible: Boolean, val claimedDomain: String? = null) :
        NewLoginEnterpriseResult<Nothing, Nothing>
    data class Sso(val code: String, val identityProviderId: String?) : NewLoginEnterpriseResult<Nothing, Nothing>
    data class CustomBackend<LinksT>(val serverConfig: LinksT) : NewLoginEnterpriseResult<LinksT, Nothing>
    data class Failure<FailureT>(val cause: NewLoginFailure<FailureT>) : NewLoginEnterpriseResult<Nothing, FailureT>
}

sealed interface NewLoginSsoInitiationResult<out FailureT> {
    data class Success(val redirectUrl: String) : NewLoginSsoInitiationResult<Nothing>
    data object InvalidCodeFormat : NewLoginSsoInitiationResult<Nothing>
    data object InvalidCode : NewLoginSsoInitiationResult<Nothing>
    data class Failure<FailureT>(val cause: NewLoginFailure<FailureT>) : NewLoginSsoInitiationResult<FailureT>
}

sealed interface NewLoginDefaultSsoCodeResult<out FailureT> {
    data class Success(val code: String?) : NewLoginDefaultSsoCodeResult<Nothing>
    data class Failure<FailureT>(val cause: NewLoginFailure<FailureT>) : NewLoginDefaultSsoCodeResult<FailureT>
}

sealed interface NewLoginSsoCallback<out SsoFailureT> {
    data class Success(val cookie: String, val serverConfigId: String) : NewLoginSsoCallback<Nothing>
    data class Failure<SsoFailureT>(val failure: SsoFailureT) : NewLoginSsoCallback<SsoFailureT>
}

sealed interface NewLoginSessionResult<out FailureT, out UserT, out SessionT> {
    data class Success<UserT>(val userId: UserT) : NewLoginSessionResult<Nothing, UserT, Nothing>
    data class IdentityChanged<SessionT>(val session: SessionT, val isNomadSession: Boolean) :
        NewLoginSessionResult<Nothing, Nothing, SessionT>
    data object InvalidCookie : NewLoginSessionResult<Nothing, Nothing, Nothing>
    data object UserAlreadyExists : NewLoginSessionResult<Nothing, Nothing, Nothing>
    data class Failure<FailureT>(val cause: NewLoginFailure<FailureT>) : NewLoginSessionResult<FailureT, Nothing, Nothing>
}

sealed interface NewLoginReplaceSessionResult<out FailureT, out UserT> {
    data class Success<UserT>(val userId: UserT) : NewLoginReplaceSessionResult<Nothing, UserT>
    data object UserAlreadyExists : NewLoginReplaceSessionResult<Nothing, Nothing>
    data class Failure<FailureT>(val failure: FailureT) : NewLoginReplaceSessionResult<FailureT, Nothing>
}

sealed interface NewLoginRegisterClientResult<out FailureT> {
    data class Success(val initialSyncCompleted: Boolean) : NewLoginRegisterClientResult<Nothing>
    data object E2EICertificateRequired : NewLoginRegisterClientResult<Nothing>
    data object TooManyDevices : NewLoginRegisterClientResult<Nothing>
    data class Failure<FailureT>(val failure: FailureT) : NewLoginRegisterClientResult<FailureT>
}

sealed interface NewLoginRestoreResult<out FailureT> {
    data class Success(val initialSyncCompleted: Boolean) : NewLoginRestoreResult<Nothing>
    data object NoBackupAvailable : NewLoginRestoreResult<Nothing>
    data object SessionUnavailable : NewLoginRestoreResult<Nothing>
    data class Failure<FailureT>(val failure: FailureT) : NewLoginRestoreResult<FailureT>
}

interface NewLoginGateway<LinksT, FailureT, UserT, SessionT> {
    fun validateIdentifier(input: String): NewLoginIdentifierValidation
    suspend fun enterpriseLogin(serverConfig: LinksT, email: String): NewLoginEnterpriseResult<LinksT, FailureT>
    suspend fun initiateSso(serverConfig: LinksT, code: String, cookieLabel: String?): NewLoginSsoInitiationResult<FailureT>
    suspend fun fetchDefaultSsoCode(serverConfig: LinksT): NewLoginDefaultSsoCodeResult<FailureT>
    suspend fun establishSession(
        cookie: String,
        serverConfigId: String,
        ssoIdentityProviderId: String?,
        consumeNomadServiceUrl: () -> String?,
        consumeCookieLabel: () -> String?,
    ): NewLoginSessionResult<FailureT, UserT, SessionT>
    suspend fun replaceRetainedSession(session: SessionT): NewLoginReplaceSessionResult<FailureT, UserT>
    suspend fun registerClient(userId: UserT, setLastDeviceIdOnSuccess: Boolean): NewLoginRegisterClientResult<FailureT>
    suspend fun restoreCryptoState(userId: UserT): NewLoginRestoreResult<FailureT>
    suspend fun revertSession(userId: UserT)
    fun logSessionContinuation(isNomadFlow: Boolean)
}

sealed interface NewLoginBackendResult<out LinksT> {
    data class Success<LinksT>(val serverConfig: LinksT) : NewLoginBackendResult<LinksT>
    data object Failure : NewLoginBackendResult<Nothing>
}

interface NewLoginBackendGateway<LinksT, BackendRequestT> {
    suspend fun parse(input: String): BackendRequestT?
    suspend fun configure(request: BackendRequestT): NewLoginBackendResult<LinksT>
    fun select(serverConfig: LinksT)
    fun clear()
}

interface NewLoginSavedStateStore {
    var userIdentifier: String?
    var pendingSsoIdentityProviderId: String?
    fun consumePendingSsoIdentityProviderId(): String? = pendingSsoIdentityProviderId.also {
        pendingSsoIdentityProviderId = null
    }
}
