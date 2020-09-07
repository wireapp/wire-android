package com.wire.android.feature.auth.registration.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.feature.auth.registration.datasource.remote.RegistrationRemoteDataSource

class RegistrationDataSource(private val remoteDataSource: RegistrationRemoteDataSource) : RegistrationRepository {

    override suspend fun registerPersonalAccount(
        name: String, email: String, password: String, activationCode: String
    ): Either<Failure, Unit> =
        remoteDataSource.registerPersonalAccount(name = name, email = email, password = password, activationCode = activationCode)
            .map {
                //TODO save user locally, etc.
                Unit
            }
}
