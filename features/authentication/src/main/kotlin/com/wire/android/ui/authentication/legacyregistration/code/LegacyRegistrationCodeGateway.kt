/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.code

data class LegacyRegistrationCodeInput<LinksT>(val customServerConfig: LinksT?, val email: String, val name: String, val password:
    String)
data class LegacyPersonalRegistrationRequest(val name: String, val password: String, val email: String, val activationCode: () ->
    String)

interface LegacyRegistrationCodeGateway<LinksT, FailureT, UserT, CredentialsT> {
    suspend fun requestActivationCode(serverConfig: LinksT, email: String): LegacyCodeActivationResult<FailureT>
    suspend fun register(serverConfig: LinksT, request: LegacyPersonalRegistrationRequest): LegacyRegistrationResult<FailureT,
    CredentialsT>
    suspend fun storeSession(credentials: CredentialsT): LegacyStoreSessionResult<FailureT, UserT>
    suspend fun registerClient(userId: UserT, password: String): LegacyRegisterClientResult<FailureT>
    fun onCodeVerificationShown()
    fun onCodeVerificationFailed()
}

sealed interface LegacyCodeActivationResult<out FailureT> { data object Sent : LegacyCodeActivationResult<Nothing>; data object
    AlreadyInUse : LegacyCodeActivationResult<Nothing>; data object Blacklisted : LegacyCodeActivationResult<Nothing>; data object
    DomainBlocked : LegacyCodeActivationResult<Nothing>; data object InvalidEmail : LegacyCodeActivationResult<Nothing>; data class
    Generic<FailureT>(val failure: FailureT) : LegacyCodeActivationResult<FailureT>; data object AuthScopeUnavailable :
    LegacyCodeActivationResult<Nothing> }
sealed interface LegacyRegistrationResult<out FailureT, out CredentialsT> { data class Success<CredentialsT>(val credentials:
    CredentialsT) : LegacyRegistrationResult<Nothing, CredentialsT>; data object InvalidActivationCode :
    LegacyRegistrationResult<Nothing, Nothing>; data object AccountAlreadyExists : LegacyRegistrationResult<Nothing, Nothing>; data
    object Blacklisted : LegacyRegistrationResult<Nothing, Nothing>; data object DomainBlocked : LegacyRegistrationResult<Nothing,
    Nothing>; data object InvalidEmail : LegacyRegistrationResult<Nothing, Nothing>; data object TeamMembersLimitReached :
    LegacyRegistrationResult<Nothing, Nothing>; data object UserCreationRestricted : LegacyRegistrationResult<Nothing, Nothing>;
    data class Generic<FailureT>(val failure: FailureT) : LegacyRegistrationResult<FailureT, Nothing>; data object
    AuthScopeUnavailable : LegacyRegistrationResult<Nothing, Nothing> }
sealed interface LegacyStoreSessionResult<out FailureT, out UserT> { data class Success<UserT>(val userId: UserT) :
    LegacyStoreSessionResult<Nothing, UserT>; data object UserAlreadyExists : LegacyStoreSessionResult<Nothing, Nothing>; data class
    Generic<FailureT>(val failure: FailureT) : LegacyStoreSessionResult<FailureT, Nothing> }
sealed interface LegacyRegisterClientResult<out FailureT> { data object Success : LegacyRegisterClientResult<Nothing>; data object
    E2EICertificateRequired : LegacyRegisterClientResult<Nothing>; data object TooManyDevices : LegacyRegisterClientResult<Nothing>;
    data class Generic<FailureT>(val failure: FailureT) : LegacyRegisterClientResult<FailureT> }
