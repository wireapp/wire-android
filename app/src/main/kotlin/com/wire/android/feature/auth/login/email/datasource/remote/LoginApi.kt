package com.wire.android.feature.auth.login.email.datasource.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface LoginApi {

    /**
     * Authenticates a user to obtain a refresh token and first access token.
     *
     * @param persistToken if true, requests a persistent refresh token instead of a session token
     */
    @POST(LOGIN)
    suspend fun loginWithEmail(
        @Body body: LoginWithEmailRequest,
        @Query(QUERY_PERSIST) persistToken: Boolean = true
    ): Response<LoginWithEmailResponse>

    companion object {
        private const val LOGIN = "/login"
        private const val QUERY_PERSIST = "persist"
    }
}
