package com.wire.android.feature.auth.login.email.datasource.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApi {

    @POST(LOGIN)
    suspend fun loginWithEmail(@Body body: LoginWithEmailRequest): Response<LoginWithEmailResponse>

    companion object {
        private const val LOGIN = "/login"
    }
}
