/* Wire Copyright (C) 2026 Wire Swiss GmbH */
package com.wire.android.ui.authentication.legacyregistration.details

data class LegacyRegistrationDetailsState<FailureT>(
    val privacyPolicyAccepted: Boolean = false,
    val termsDialogVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val continueEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: DetailsError = DetailsError.None,
    val success: Boolean = false,
) {
    sealed class DetailsError {
        data object None : DetailsError()
        sealed class PasswordError : DetailsError() {
            data object InvalidPasswordError : PasswordError()
            data object PasswordsNotMatchingError : PasswordError()
        }
        sealed class EmailFieldError : DetailsError() {
            data object InvalidEmailError : EmailFieldError()
            data object BlacklistedEmailError : EmailFieldError()
            data object AlreadyInUseError : EmailFieldError()
            data object DomainBlockedError : EmailFieldError()
        }
        data class GenericError(val failure: Any?) : DetailsError()
        fun isPasswordError() = this is PasswordError
        fun isEmailError() = this is EmailFieldError
    }
}
