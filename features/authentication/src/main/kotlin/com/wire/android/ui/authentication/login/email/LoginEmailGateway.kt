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

data class LoginEmailInput<LinksT, DomainClaimT>(
    val serverConfig: LinksT,
    val isBackendConfigured: Boolean,
    val preFilledIdentifier: String?,
    val identifierEditable: Boolean,
    val domainClaimedByOrg: DomainClaimT?,
)

data class LoginEmailProxyCredentials(val identifier: String, val password: String)

sealed interface LoginEmailScopeResult<out FailureT, out ScopeT> {
    data class Success<ScopeT>(val scope: ScopeT) : LoginEmailScopeResult<Nothing, ScopeT>
    data object UnknownServerVersion : LoginEmailScopeResult<Nothing, Nothing>
    data object ClientUpdateRequired : LoginEmailScopeResult<Nothing, Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginEmailScopeResult<FailureT, Nothing>
}

sealed interface LoginEmailAuthenticationResult<out FailureT, out SessionT> {
    data class Success<SessionT>(val session: SessionT) : LoginEmailAuthenticationResult<Nothing, SessionT>
    data object ProxyError : LoginEmailAuthenticationResult<Nothing, Nothing>
    data object InvalidCredentials : LoginEmailAuthenticationResult<Nothing, Nothing>
    data object InvalidIdentifier : LoginEmailAuthenticationResult<Nothing, Nothing>
    data object AccountSuspended : LoginEmailAuthenticationResult<Nothing, Nothing>
    data object AccountPendingActivation : LoginEmailAuthenticationResult<Nothing, Nothing>
    data object MissingSecondFactor : LoginEmailAuthenticationResult<Nothing, Nothing>
    data object InvalidSecondFactor : LoginEmailAuthenticationResult<Nothing, Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginEmailAuthenticationResult<FailureT, Nothing>
}

sealed interface LoginEmailStoreResult<out FailureT, out UserT> {
    data class Success<UserT>(val userId: UserT) : LoginEmailStoreResult<Nothing, UserT>
    data object UserAlreadyExists : LoginEmailStoreResult<Nothing, Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginEmailStoreResult<FailureT, Nothing>
}

sealed interface LoginEmailPersistResult<out FailureT> {
    data object Success : LoginEmailPersistResult<Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginEmailPersistResult<FailureT>
}

sealed interface LoginEmailClientResult<out FailureT> {
    data class Success(val initialSyncCompleted: Boolean) : LoginEmailClientResult<Nothing>
    data class E2EICertificateRequired(val initialSyncCompleted: Boolean) : LoginEmailClientResult<Nothing>
    data object TooManyDevices : LoginEmailClientResult<Nothing>
    data object InvalidCredentials : LoginEmailClientResult<Nothing>
    data object PasswordRequired : LoginEmailClientResult<Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginEmailClientResult<FailureT>
}

sealed interface LoginEmailVerificationResult<out FailureT> {
    data object Sent : LoginEmailVerificationResult<Nothing>
    data object TooManyRequests : LoginEmailVerificationResult<Nothing>
    data class Failure<FailureT>(val failure: FailureT) : LoginEmailVerificationResult<FailureT>
}

sealed interface LoginEmailBackendResult<out LinksT> {
    data class Success<LinksT>(val serverConfig: LinksT) : LoginEmailBackendResult<LinksT>
    data object Failure : LoginEmailBackendResult<Nothing>
}

interface LoginEmailTimer {
    suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit)
}

interface LoginEmailGateway<LinksT, FailureT, UserT, ScopeT, SessionT, BackendRequestT> {
    fun isProxyAuthRequired(serverConfig: LinksT): Boolean
    fun isEmail(value: String): Boolean
    suspend fun currentValidSession(): UserT?
    suspend fun resolveScope(
        serverConfig: LinksT,
        proxyCredentials: () -> LoginEmailProxyCredentials?,
    ): LoginEmailScopeResult<FailureT, ScopeT>
    suspend fun authenticate(
        scope: ScopeT,
        identifier: () -> String,
        password: () -> String,
        secondFactorCode: String,
    ): LoginEmailAuthenticationResult<FailureT, SessionT>
    suspend fun storeSession(session: SessionT): LoginEmailStoreResult<FailureT, UserT>
    suspend fun persistEmailIfNeeded(userId: UserT, identifier: () -> String): LoginEmailPersistResult<FailureT>
    suspend fun registerClient(userId: UserT, password: () -> String): LoginEmailClientResult<FailureT>
    suspend fun requestSecondFactorCode(scope: ScopeT, email: String): LoginEmailVerificationResult<FailureT>
    suspend fun revertSession(newSessionUserId: UserT?, previousSessionUserId: UserT?)
    suspend fun parseBackendConfig(input: String): BackendRequestT?
    suspend fun configureBackend(request: BackendRequestT): LoginEmailBackendResult<LinksT>
}
