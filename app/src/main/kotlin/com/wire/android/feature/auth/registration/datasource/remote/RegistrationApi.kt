package com.wire.android.feature.auth.registration.datasource.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RegistrationApi {

    @POST(REGISTER)
    suspend fun register(@Body body: RegisterPersonalAccountWithEmailRequest): Response<UserResponse>

    companion object {
        private const val REGISTER = "/register"
    }
}
