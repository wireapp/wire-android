package com.wire.android.feature.auth.activation.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.ActivationRepository
import com.wire.android.feature.auth.activation.datasource.remote.ActivationRemoteDataSource

class ActivationDataSource(private val activationRemoteDataSource: ActivationRemoteDataSource) : ActivationRepository {

    override suspend fun sendEmailActivationCode(email: String): Either<Failure, Unit> =
        activationRemoteDataSource.sendEmailActivationCode(email)
}
