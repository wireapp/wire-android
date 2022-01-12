package com.wire.android.feature.auth.client.datasource.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ClientApi {

    @POST(CLIENTS)
    suspend fun registerClient(
        @Header(HEADER_KEY_AUTHORIZATION) authorizationHeader: String,
        @Body body: ClientRegistrationRequest
    ): Response<ClientResponse>

    @PUT(CLIENT_BY_ID)
    suspend fun updatePreKeys(
        @Header(HEADER_KEY_AUTHORIZATION) authorizationHeader: String,
        @Path(value = CLIENT_ID) clientId: String,
        @Body body: UpdatePreKeysRequest
    ): Response<Unit>

    @GET(PREKEYS_OF_CLIENT)
    suspend fun remainingPreKeys(
        @Header(HEADER_KEY_AUTHORIZATION) authorizationHeader: String,
        @Path(value = CLIENT_ID) clientId: String
    ): Response<RemainingPreKeysResponse>

    @GET(CLIENTS_OF_USERS)
    suspend fun clientsOfUsers(
        @Header(HEADER_KEY_AUTHORIZATION) authorizationHeader: String,
        @Body body: ClientsOfUsersRequest
    ): Response<ClientsOfUsersResponse>

    companion object {
        private const val CLIENTS = "/clients"
        private const val HEADER_KEY_AUTHORIZATION = "Authorization"
        private const val CLIENT_BY_ID = "/clients/{clientId}"
        private const val CLIENT_ID = "clientId"
        private const val PREKEYS_OF_CLIENT = "$CLIENT_BY_ID/prekeys"
        private const val CLIENTS_OF_USERS = "/users/list-clients/v2"
    }
}
