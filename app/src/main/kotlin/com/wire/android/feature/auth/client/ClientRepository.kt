package com.wire.android.feature.auth.client

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface ClientRepository {
    suspend fun registerNewClient(authorizationToken: String, userId: String, password: String): Either<Failure, Client>
    suspend fun saveLocally(client: Client): Either<Failure, Unit>
}
