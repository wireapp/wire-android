package com.wire.android.feature.auth.client

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse

interface ClientRepository {
    //TODO return Client instead of ClientResponse
    suspend fun registerNewClient(authorizationToken: String, userId: String, password: String): Either<Failure, ClientResponse>
    suspend fun updatePreKeysIfNeeded(authorizationToken: String, clientId: String): Either<Failure, Unit>
}
