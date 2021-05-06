package com.wire.android.feature.auth.client.datasource.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ClientApi {

    @POST(CLIENTS)
    suspend fun registerClient(
        @Header(HEADER_KEY_AUTHORIZATION) authorizationHeader: String,
        @Body body: ClientRegistrationRequest
    ): Response<ClientResponse>

    companion object {
        private const val CLIENTS = "/clients"
        private const val HEADER_KEY_AUTHORIZATION = "Authorization"
    }
}
