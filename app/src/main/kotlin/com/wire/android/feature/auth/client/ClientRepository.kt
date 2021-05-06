package com.wire.android.feature.auth.client

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse

interface ClientRepository {
    suspend fun registerNewClient(authorizationToken: String, client: Client): Either<Failure, ClientResponse>
}
