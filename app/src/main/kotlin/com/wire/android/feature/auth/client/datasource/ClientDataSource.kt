package com.wire.android.feature.auth.client.datasource

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.remote.ClientRemoteDataSource
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse


class ClientDataSource(private val clientRemoteDataSource: ClientRemoteDataSource) : ClientRepository {

    override suspend fun registerNewClient(authorizationToken: String, client: Client): Either<Failure, ClientResponse> =
        clientRemoteDataSource.registerNewClient(authorizationToken, client)
}
