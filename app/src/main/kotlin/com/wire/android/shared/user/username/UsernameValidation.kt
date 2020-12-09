package com.wire.android.shared.user.username

import com.wire.android.core.exception.FeatureFailure

object UsernameTooLong : ValidateUsernameError()
object UsernameTooShort : ValidateUsernameError()
object UsernameInvalid : ValidateUsernameError()
object UsernameAlreadyExists : ValidateUsernameError()
object UsernameGeneralError : ValidateUsernameError()
object UsernameIsAvailable : ValidateUsernameSuccess()

sealed class ValidateUsernameSuccess
sealed class ValidateUsernameError : FeatureFailure()
