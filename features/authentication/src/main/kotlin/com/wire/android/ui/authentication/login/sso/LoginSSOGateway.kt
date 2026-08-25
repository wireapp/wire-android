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

data class LoginSSOInput<LinksT>(
    val serverConfig: LinksT,
    val pendingNomadServiceUrl: String? = null,
    val pendingCookieLabel: String? = null,
)

data class LoginSSOWebRequest<LinksT>(val url: String, val serverConfig: LinksT)

sealed interface LoginSSOFailure<out FailureT> {
    data class Generic<FailureT>(val failure: FailureT) : LoginSSOFailure<FailureT>
    data object ClientUpdateRequired : LoginSSOFailure<Nothing>
    data object ServerVersionNotSupported : LoginSSOFailure<Nothing>
}

sealed interface LoginSSOInitiationResult<out FailureT> {
    data class Success(val redirectUrl: String) : LoginSSOInitiationResult<Nothing>
    data object InvalidCodeFormat : LoginSSOInitiationResult<Nothing>
    data object InvalidCode : LoginSSOInitiationResult<Nothing>
    data class Failure<FailureT>(val cause: LoginSSOFailure<FailureT>) : LoginSSOInitiationResult<FailureT>
}

sealed interface LoginSSODomainLookupResult<out LinksT, out FailureT> {
    data class Success<LinksT>(val serverConfig: LinksT) : LoginSSODomainLookupResult<LinksT, Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginSSODomainLookupResult<Nothing, FailureT>
    data object AuthenticationUnavailable : LoginSSODomainLookupResult<Nothing, Nothing>
}

sealed interface LoginSSODefaultCodeResult<out FailureT> {
    data class Success(val code: String?) : LoginSSODefaultCodeResult<Nothing>
    data class Failure<FailureT>(val cause: LoginSSOFailure<FailureT>) : LoginSSODefaultCodeResult<FailureT>
    data object Unavailable : LoginSSODefaultCodeResult<Nothing>
}

sealed interface LoginSSOSessionResult<out FailureT, out UserT, out SessionT> {
    data class Success<UserT>(val userId: UserT) : LoginSSOSessionResult<Nothing, UserT, Nothing>
    data class IdentityChanged<SessionT>(
        val session: SessionT,
        val isNomadSession: Boolean,
    ) : LoginSSOSessionResult<Nothing, Nothing, SessionT>

    data object InvalidCookie : LoginSSOSessionResult<Nothing, Nothing, Nothing>
    data object UserAlreadyExists : LoginSSOSessionResult<Nothing, Nothing, Nothing>
    data class Failure<FailureT>(val cause: LoginSSOFailure<FailureT>) : LoginSSOSessionResult<FailureT, Nothing, Nothing>
}

sealed interface LoginSSOReplaceSessionResult<out FailureT, out UserT> {
    data class Success<UserT>(val userId: UserT) : LoginSSOReplaceSessionResult<Nothing, UserT>
    data object UserAlreadyExists : LoginSSOReplaceSessionResult<Nothing, Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginSSOReplaceSessionResult<FailureT, Nothing>
}

sealed interface LoginSSORegisterClientResult<out FailureT> {
    data class Success(val initialSyncCompleted: Boolean) : LoginSSORegisterClientResult<Nothing>
    data class E2EICertificateRequired(val initialSyncCompleted: Boolean) : LoginSSORegisterClientResult<Nothing>
    data object TooManyDevices : LoginSSORegisterClientResult<Nothing>
    data object InvalidCredentials : LoginSSORegisterClientResult<Nothing>
    data object PasswordRequired : LoginSSORegisterClientResult<Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginSSORegisterClientResult<FailureT>
}

sealed interface LoginSSORestoreResult<out FailureT> {
    data class Success(val initialSyncCompleted: Boolean) : LoginSSORestoreResult<Nothing>
    data object NoBackupAvailable : LoginSSORestoreResult<Nothing>
    data object SessionUnavailable : LoginSSORestoreResult<Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginSSORestoreResult<FailureT>
}

interface LoginSSOGateway<LinksT, FailureT, UserT, SessionT> {
    fun isEmail(value: String): Boolean

    suspend fun initiateSSO(
        serverConfig: LinksT,
        ssoCode: String,
        cookieLabel: String?,
    ): LoginSSOInitiationResult<FailureT>

    suspend fun lookupDomain(email: () -> String): LoginSSODomainLookupResult<LinksT, FailureT>

    suspend fun fetchDefaultSSOCode(serverConfig: LinksT): LoginSSODefaultCodeResult<FailureT>

    suspend fun establishSession(
        cookie: String,
        serverConfigId: String,
        consumeNomadServiceUrl: () -> String?,
        consumeCookieLabel: () -> String?,
    ): LoginSSOSessionResult<FailureT, UserT, SessionT>

    suspend fun replaceRetainedSession(session: SessionT): LoginSSOReplaceSessionResult<FailureT, UserT>

    fun logSessionContinuation(isNomadFlow: Boolean)

    suspend fun registerClient(
        userId: UserT,
        setLastDeviceIdOnSuccess: Boolean,
    ): LoginSSORegisterClientResult<FailureT>

    suspend fun restoreCryptoState(userId: UserT): LoginSSORestoreResult<FailureT>

    suspend fun revertSession(userId: UserT)
}
