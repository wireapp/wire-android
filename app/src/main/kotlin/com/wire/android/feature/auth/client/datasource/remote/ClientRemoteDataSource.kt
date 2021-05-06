package com.wire.android.feature.auth.client.datasource.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.datasource.remote.api.ClientApi
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.mapper.ClientMapper

class ClientRemoteDataSource(
    override val networkHandler: NetworkHandler,
    private val clientApi: ClientApi,
    private val clientMapper: ClientMapper
) : ApiService() {

    suspend fun registerNewClient(authorizationToken: String, client: Client): Either<Failure, ClientResponse> =
        request {
            clientApi.registerClient(authorizationToken, clientMapper.toClientRegistrationRequest(client))
        }
}
