/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.code

internal fun <UserT, FailureT> LegacyCodeActivationResult<FailureT>.toResult():
    LegacyRegistrationCodeState.Result<UserT, FailureT> = when (this) {
    LegacyCodeActivationResult.Sent -> LegacyRegistrationCodeState.Result.None
    LegacyCodeActivationResult.AlreadyInUse -> LegacyRegistrationCodeState.Result.AccountAlreadyExists
    LegacyCodeActivationResult.Blacklisted -> LegacyRegistrationCodeState.Result.Blacklisted
    LegacyCodeActivationResult.DomainBlocked -> LegacyRegistrationCodeState.Result.DomainBlocked
    LegacyCodeActivationResult.InvalidEmail -> LegacyRegistrationCodeState.Result.InvalidEmail
    is LegacyCodeActivationResult.Generic -> LegacyRegistrationCodeState.Result.Generic(failure)
    LegacyCodeActivationResult.AuthScopeUnavailable -> error("Handled before mapping")
}

internal fun <UserT, FailureT, CredentialsT> LegacyRegistrationResult<FailureT, CredentialsT>.toResult():
    LegacyRegistrationCodeState.Result<UserT, FailureT> = when (this) {
    LegacyRegistrationResult.InvalidActivationCode -> LegacyRegistrationCodeState.Result.InvalidActivationCode
    LegacyRegistrationResult.AccountAlreadyExists -> LegacyRegistrationCodeState.Result.AccountAlreadyExists
    LegacyRegistrationResult.Blacklisted -> LegacyRegistrationCodeState.Result.Blacklisted
    LegacyRegistrationResult.DomainBlocked -> LegacyRegistrationCodeState.Result.DomainBlocked
    LegacyRegistrationResult.InvalidEmail -> LegacyRegistrationCodeState.Result.InvalidEmail
    LegacyRegistrationResult.TeamMembersLimitReached -> LegacyRegistrationCodeState.Result.TeamMembersLimit
    LegacyRegistrationResult.UserCreationRestricted -> LegacyRegistrationCodeState.Result.CreationRestricted
    is LegacyRegistrationResult.Generic -> LegacyRegistrationCodeState.Result.Generic(failure)
    is LegacyRegistrationResult.Success,
    LegacyRegistrationResult.AuthScopeUnavailable -> error("Handled before mapping")
}

internal fun <UserT, FailureT> LegacyStoreSessionResult<FailureT, UserT>.toResult():
    LegacyRegistrationCodeState.Result<UserT, FailureT> = when (this) {
    is LegacyStoreSessionResult.Success -> error("Handled before mapping")
    LegacyStoreSessionResult.UserAlreadyExists -> LegacyRegistrationCodeState.Result.UserAlreadyExists
    is LegacyStoreSessionResult.Generic -> LegacyRegistrationCodeState.Result.Generic(failure)
}
