package com.wire.android.feature.auth.registration.personal.email.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase

class ActivateEmailUseCase : UseCase<Unit, ActivateEmailParams> {
    override suspend fun run(params: ActivateEmailParams): Either<Failure, Unit> = Either.Right(Unit)
}

data class ActivateEmailParams(val email: String, val code: String)

sealed class ActivateEmailFailure : FeatureFailure()
object InvalidEmailCode : ActivateEmailFailure()
