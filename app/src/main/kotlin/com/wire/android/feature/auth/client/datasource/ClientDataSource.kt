package com.wire.android.feature.auth.client.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.LocationResponse

class ClientDataSource : ClientRepository {

    override suspend fun registerNewClient(password: String?): Either<Failure, ClientResponse> =
        Either.Right(ClientResponse("", "", "", LocationResponse("", "", ""), "", "", "", "", ""))
}
