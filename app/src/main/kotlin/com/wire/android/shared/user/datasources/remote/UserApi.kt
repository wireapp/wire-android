package com.wire.android.shared.user.datasources.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface UserApi {

    @GET(SELF)
    suspend fun selfUser(@Header(HEADER_KEY_AUTHORIZATION) authorizationHeader: String): Response<SelfUserResponse>

    companion object {
        private const val HEADER_KEY_AUTHORIZATION = "Authorization"
        private const val SELF = "/self"
    }
}
