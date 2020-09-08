package com.wire.android.feature.auth.registration

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface RegistrationRepository {
    suspend fun registerPersonalAccount(name: String, email: String, password: String, activationCode: String): Either<Failure, String>
}
