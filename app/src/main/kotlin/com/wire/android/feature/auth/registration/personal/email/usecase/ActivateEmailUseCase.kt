package com.wire.android.feature.auth.registration.personal.email.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.activation.ActivationRepository

class ActivateEmailUseCase(private val activationRepository: ActivationRepository) : UseCase<Unit, ActivateEmailParams> {
    override suspend fun run(params: ActivateEmailParams): Either<Failure, Unit> =
        activationRepository.activateEmail(params.email, params.code).fold({
            when (it) {
                is NotFound -> Either.Left(InvalidEmailCode)
                else -> Either.Left(it)
            }
        }) { Either.Right(it) }!!
}

data class ActivateEmailParams(val email: String, val code: String)

sealed class ActivateEmailFailure : FeatureFailure()
object InvalidEmailCode : ActivateEmailFailure()
