package com.wire.android.shared.user.email

import androidx.core.util.PatternsCompat
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.OneShotUseCase

sealed class ValidateEmailError : FeatureFailure()
object EmailTooShort : ValidateEmailError()
object EmailInvalid : ValidateEmailError()

class ValidateEmailUseCase : OneShotUseCase<ValidateEmailParams, Unit> {

    override suspend fun run(params: ValidateEmailParams): Either<Failure, Unit> = when {
        isEmailTooShort(params.email) -> Either.Left(EmailTooShort)
        !emailCharactersValid(params.email) -> Either.Left(EmailInvalid)
        else -> Either.Right(Unit)
    }

    private fun emailCharactersValid(email: String) =
        PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()

    private fun isEmailTooShort(email: String) = email.length < EMAIL_MIN_LENGTH

    companion object {
        private const val EMAIL_MIN_LENGTH = 5
    }
}

data class ValidateEmailParams(val email: String)
