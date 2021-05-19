package com.wire.android.feature.auth.client.datasource.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.feature.auth.client.datasource.remote.api.ClientApi
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse

class ClientRemoteDataSource(
    override val networkHandler: NetworkHandler,
    private val clientApi: ClientApi
) : ApiService() {

    suspend fun registerNewClient(
        authorizationToken: String,
        clientRegistrationRequest: ClientRegistrationRequest
    ): Either<Failure, ClientResponse> = request {
        clientApi.registerClient(authorizationToken, clientRegistrationRequest)
    }
}
