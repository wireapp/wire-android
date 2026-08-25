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

data class CreateAccountCodeInput<FlowT, LinksT>(
    val flowType: FlowT,
    val customServerConfig: LinksT?,
    val email: String,
    val firstName: String,
    val lastName: String,
    val password: String,
    val teamName: String,
    val isTeam: Boolean,
)

sealed interface CreateAccountRegistrationRequest {
    val firstName: String
    val lastName: String
    val password: String
    val email: String
    val activationCode: () -> String

    data class Personal(
        override val firstName: String,
        override val lastName: String,
        override val password: String,
        override val email: String,
        override val activationCode: () -> String,
    ) : CreateAccountRegistrationRequest

    data class Team(
        override val firstName: String,
        override val lastName: String,
        override val password: String,
        override val email: String,
        override val activationCode: () -> String,
        val teamName: String,
        val teamIcon: String = "default",
    ) : CreateAccountRegistrationRequest
}

interface CreateAccountCodeGateway<LinksT, FailureT, UserT, CredentialsT> {
    suspend fun requestActivationCode(serverConfig: LinksT, email: String): ActivationCodeRequestResult<FailureT>
    suspend fun register(serverConfig: LinksT, request: CreateAccountRegistrationRequest): AccountRegistrationResult<FailureT, CredentialsT>
    suspend fun storeSession(credentials: CredentialsT): StoreAccountSessionResult<FailureT, UserT>
    suspend fun registerClient(userId: UserT, password: String): CreateAccountClientResult<FailureT>
}

sealed interface ActivationCodeRequestResult<out FailureT> {
    data object Sent : ActivationCodeRequestResult<Nothing>
    data object AlreadyInUse : ActivationCodeRequestResult<Nothing>
    data object Blacklisted : ActivationCodeRequestResult<Nothing>
    data object DomainBlocked : ActivationCodeRequestResult<Nothing>
    data object InvalidEmail : ActivationCodeRequestResult<Nothing>
    data class Generic<FailureT>(val failure: FailureT) : ActivationCodeRequestResult<FailureT>
    data object AuthScopeUnavailable : ActivationCodeRequestResult<Nothing>
}

sealed interface AccountRegistrationResult<out FailureT, out CredentialsT> {
    data class Success<CredentialsT>(val credentials: CredentialsT) : AccountRegistrationResult<Nothing, CredentialsT>
    data object InvalidActivationCode : AccountRegistrationResult<Nothing, Nothing>
    data object AccountAlreadyExists : AccountRegistrationResult<Nothing, Nothing>
    data object Blacklisted : AccountRegistrationResult<Nothing, Nothing>
    data object DomainBlocked : AccountRegistrationResult<Nothing, Nothing>
    data object InvalidEmail : AccountRegistrationResult<Nothing, Nothing>
    data object TeamMembersLimitReached : AccountRegistrationResult<Nothing, Nothing>
    data object UserCreationRestricted : AccountRegistrationResult<Nothing, Nothing>
    data class Generic<FailureT>(val failure: FailureT) : AccountRegistrationResult<FailureT, Nothing>
    data object AuthScopeUnavailable : AccountRegistrationResult<Nothing, Nothing>
}

sealed interface StoreAccountSessionResult<out FailureT, out UserT> {
    data class Success<UserT>(val userId: UserT) : StoreAccountSessionResult<Nothing, UserT>
    data object UserAlreadyExists : StoreAccountSessionResult<Nothing, Nothing>
    data class Generic<FailureT>(val failure: FailureT) : StoreAccountSessionResult<FailureT, Nothing>
}

sealed interface CreateAccountClientResult<out FailureT> {
    data object Success : CreateAccountClientResult<Nothing>
    data object E2EICertificateRequired : CreateAccountClientResult<Nothing>
    data object TooManyDevices : CreateAccountClientResult<Nothing>
    data class Generic<FailureT>(val failure: FailureT) : CreateAccountClientResult<FailureT>
}

fun interface CreateAccountCodeResendTimer {
    suspend fun start(seconds: Long, onUpdate: (String) -> Unit, onFinish: () -> Unit)
}
