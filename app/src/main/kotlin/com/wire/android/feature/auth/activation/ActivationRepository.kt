package com.wire.android.feature.auth.activation

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface ActivationRepository {
    suspend fun sendEmailActivationCode(email: String): Either<Failure, Unit>
}