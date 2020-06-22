package com.wire.android.feature.auth.activation.usecase

import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.OneShotUseCase
import com.wire.android.feature.auth.activation.ActivationRepository

class SendEmailActivationCodeUseCase(
        private val activationRepository: ActivationRepository
) : OneShotUseCase<SendEmailActivationCodeParams, Unit> {

    override suspend fun run(params: SendEmailActivationCodeParams): Either<Failure, Unit> =
        activationRepository.sendEmailActivationCode(params.email).fold({
            when (it) {
                is Forbidden -> Either.Left(EmailBlacklisted)
                is Conflict -> Either.Left(EmailInUse)
                else -> Either.Left(it)
            }
        }) { Either.Right(it) }!!
}

data class SendEmailActivationCodeParams(val email: String)

sealed class SendEmailActivationCodeFailure : FeatureFailure()
object EmailBlacklisted : SendEmailActivationCodeFailure()
object EmailInUse : SendEmailActivationCodeFailure()

