package com.wire.android.shared.session.datasources.remote

import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST

interface SessionApi {

    @POST(ACCESS)
    suspend fun accessToken(@Header(HEADER_KEY_REFRESH_TOKEN) refreshTokenHeader: String): Response<AccessTokenResponse>

    companion object {
        private const val HEADER_KEY_REFRESH_TOKEN = "Cookie"
        private const val ACCESS = "/access"
    }
}
