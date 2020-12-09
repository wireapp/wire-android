package com.wire.android.shared.user.username

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.user.UserRepository

class CheckUsernameExistsUseCase(private val userRepository: UserRepository) :
    UseCase<CheckUsernameSuccess, CheckUsernameExistsParams> {

    override suspend fun run(params: CheckUsernameExistsParams): Either<Failure, CheckUsernameSuccess> =
        userRepository.doesUsernameExist(params.username)
}

data class CheckUsernameExistsParams(val username: String)

object UsernameAlreadyExists : CheckUsernameError()
object UsernameGeneralError : CheckUsernameError()
sealed class CheckUsernameError : FeatureFailure()

object UsernameIsAvailable : CheckUsernameSuccess()
sealed class CheckUsernameSuccess
