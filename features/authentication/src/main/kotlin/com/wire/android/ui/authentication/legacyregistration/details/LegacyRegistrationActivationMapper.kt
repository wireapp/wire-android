/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.details

internal sealed interface MappedError {
    data object AuthScopeUnavailable : MappedError
    data class Value(val error: LegacyRegistrationDetailsState.DetailsError) : MappedError
}

internal fun <FailureT> LegacyActivationCodeResult<FailureT>.toError(): MappedError? = when (this) {
    LegacyActivationCodeResult.Sent -> null
    LegacyActivationCodeResult.AuthScopeUnavailable -> MappedError.AuthScopeUnavailable
    LegacyActivationCodeResult.AlreadyInUse -> MappedError.Value(
        LegacyRegistrationDetailsState.DetailsError.EmailFieldError.AlreadyInUseError,
    )

    LegacyActivationCodeResult.Blacklisted -> MappedError.Value(
        LegacyRegistrationDetailsState.DetailsError.EmailFieldError.BlacklistedEmailError,
    )

    LegacyActivationCodeResult.DomainBlocked -> MappedError.Value(
        LegacyRegistrationDetailsState.DetailsError.EmailFieldError.DomainBlockedError,
    )

    LegacyActivationCodeResult.InvalidEmail -> MappedError.Value(
        LegacyRegistrationDetailsState.DetailsError.EmailFieldError.InvalidEmailError,
    )

    is LegacyActivationCodeResult.Generic -> MappedError.Value(
        LegacyRegistrationDetailsState.DetailsError.GenericError(failure),
    )
}
