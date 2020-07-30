package com.wire.android.feature.auth.registration.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.RegistrationRepository

class RegistrationDataSource : RegistrationRepository {
    override suspend fun registerPersonalAccountWithEmail(
        name: String, email: String, password: String, activationCode: String
    ): Either<Failure, Unit> {
        //TODO: implement
        return Either.Right(Unit)
    }
}
