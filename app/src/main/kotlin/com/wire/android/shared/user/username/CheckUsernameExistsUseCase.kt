package com.wire.android.shared.user.username

import com.wire.android.core.exception.BadRequest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.user.UserRepository

class CheckUsernameExistsUseCase(private val userRepository: UserRepository) :
    UseCase<String, CheckUsernameExistsParams> {

    override suspend fun run(params: CheckUsernameExistsParams): Either<Failure, String> =
        userRepository.doesUsernameExist(params.username)
            .fold({ handleFailure(it, params.username) }, { handleSuccess() }) ?: Either.Left(UsernameGeneralError)

    private fun handleSuccess(): Either<Failure, String> =
        Either.Left(UsernameAlreadyExists)

    private fun handleFailure(failure: Failure, username: String): Either<Failure, String> =
        when (failure) {
            NotFound -> Either.Right(username)
            BadRequest -> Either.Left(UsernameInvalid)
            else -> Either.Left(failure)
        }

}

data class CheckUsernameExistsParams(val username: String)

object UsernameAlreadyExists : CheckUsernameError()
object UsernameGeneralError : CheckUsernameError()

sealed class CheckUsernameError : FeatureFailure()
