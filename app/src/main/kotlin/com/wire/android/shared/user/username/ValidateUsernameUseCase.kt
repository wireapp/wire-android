package com.wire.android.shared.user.username

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase

class ValidateUsernameUseCase : UseCase<String, ValidateUsernameParams> {

    override suspend fun run(params: ValidateUsernameParams): Either<Failure, String> = with(params) {
        if (!areUsernameCharactersValid(username)) {
            Either.Left(UsernameInvalid)
        } else {
            when {
                isUsernameTooLong(username) -> Either.Left(UsernameTooLong)
                isUsernameTooShort(username) -> Either.Left(UsernameTooShort)
                else -> Either.Right(username)
            }
        }
    }

    private fun areUsernameCharactersValid(username: String) =
        username.matches(USERNAME_REGEX)

    private fun isUsernameTooLong(username: String) =
        username.length > USERNAME_MAX_LENGTH

    private fun isUsernameTooShort(username: String) =
        username.length < USERNAME_MIN_LENGTH

    companion object {
        private val USERNAME_REGEX = """^([a-z]|[0-9]|_)*""".toRegex()
        private const val USERNAME_MAX_LENGTH = 256
        private const val USERNAME_MIN_LENGTH = 2
    }
}

sealed class ValidateUsernameError : FeatureFailure()
object UsernameTooLong : ValidateUsernameError()
object UsernameTooShort : ValidateUsernameError()
object UsernameInvalid : ValidateUsernameError()

data class ValidateUsernameParams(val username: String)
