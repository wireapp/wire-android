package com.wire.android.feature.auth.client.datasource.remote

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import com.wire.android.feature.auth.client.datasource.remote.api.ClientApi
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.UpdatePreKeysRequest

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

    suspend fun remainingPreKeys(
        authorizationToken: String,
        clientId: String
    ): Either<Failure, List<Int>> = request {
        clientApi.remainingPreKeys(authorizationToken, clientId)
    }

    suspend fun saveNewPreKeys(
        authorizationToken: String,
        clientId: String,
        updatePreKeysRequest: UpdatePreKeysRequest
    ): Either<Failure, Unit> = request {
        clientApi.updatePreKeys(authorizationToken, clientId, updatePreKeysRequest)
    }
}
