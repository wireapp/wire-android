package com.wire.android.feature.auth.registration

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.personal.PersonalAccountRegistrationResult

interface RegistrationRepository {
    suspend fun registerPersonalAccount(name: String, email: String, username: String, password: String, activationCode: String):
            Either<Failure, PersonalAccountRegistrationResult>
}
