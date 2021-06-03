package com.wire.android.feature.auth.client

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface ClientRepository {
    suspend fun registerNewClient(authorizationToken: String, userId: String, password: String): Either<Failure, Unit>
    suspend fun updatePreKeysIfNeeded(authorizationToken: String, clientId: String): Either<Failure, Unit>
}
