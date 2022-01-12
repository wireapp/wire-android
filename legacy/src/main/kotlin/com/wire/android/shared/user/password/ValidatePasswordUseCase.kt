package com.wire.android.shared.user.password

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase

class ValidatePasswordUseCase(private val lengthConfig: PasswordLengthConfig) : UseCase<Unit, ValidatePasswordParams> {

    //TODO: should we differentiate error types (NoDigitFound, TooShort, etc...)?
    override suspend fun run(params: ValidatePasswordParams): Either<Failure, Unit> =
        if (passwordValid(params.password)) Either.Right(Unit)
        else Either.Left(InvalidPasswordFailure)

    fun minLength() = lengthConfig.minLength()

    private fun passwordValid(password: String) =
        passwordPattern(lengthConfig.minLength(), lengthConfig.maxLength()).toRegex().matches(password)

    companion object {
        private const val REGEX_DIGIT = "(?=.*[0-9])"
        private const val REGEX_LOWERCASE = "(?=.*[a-z])"
        private const val REGEX_UPPERCASE = "(?=.*[A-Z])"
        private const val REGEX_SPECIAL_CHAR = "(?=.*[^a-zA-Z0-9])"

        private fun passwordPattern(minLength: Int, maxLength: Int) =
            "^$REGEX_DIGIT$REGEX_LOWERCASE$REGEX_UPPERCASE$REGEX_SPECIAL_CHAR.{$minLength,$maxLength}$"
    }
}

data class ValidatePasswordParams(val password: String)

object InvalidPasswordFailure : FeatureFailure()
