package com.wire.android.feature.auth.activation.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.ActivationRepository

class ActivationDataSource : ActivationRepository {
    //todo send an actual network request
    override suspend fun sendEmailActivationCode(email: String): Either<Failure, Unit> = Either.Right(Unit)
}
