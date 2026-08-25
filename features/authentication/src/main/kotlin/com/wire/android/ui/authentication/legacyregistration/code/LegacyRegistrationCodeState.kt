/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.code

data class LegacyRegistrationCodeState<UserT, FailureT>(
    val codeLength: Int = 6,
    val email: String = "",
    val loading: Boolean =
    false,
        val result: Result<UserT, FailureT> = Result.None
) {
    sealed interface Result<out UserT, out FailureT> {
        data object None : Result<Nothing, Nothing>
        data class Success<UserT>(val userId: UserT) : Result<UserT, Nothing>
        data object InvalidActivationCode : Result<Nothing, Nothing>
        data object AccountAlreadyExists : Result<Nothing, Nothing>
        data object Blacklisted : Result<Nothing, Nothing>
        data object DomainBlocked : Result<Nothing, Nothing>
        data object InvalidEmail : Result<Nothing, Nothing>
        data object TeamMembersLimit : Result<Nothing, Nothing>
        data object CreationRestricted : Result<Nothing, Nothing>
        data object UserAlreadyExists : Result<Nothing, Nothing>
        data class Generic<FailureT>(val failure: FailureT) : Result<Nothing, FailureT>
        data class TooManyDevices<UserT>(val userId: UserT) : Result<UserT, Nothing>
    }
}
