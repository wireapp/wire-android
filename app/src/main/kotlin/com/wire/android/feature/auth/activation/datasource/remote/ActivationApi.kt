package com.wire.android.feature.auth.activation.datasource.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ActivationApi {

    @POST("$ACTIVATE$SEND")
    suspend fun sendActivationCode(@Body name: SendEmailActivationCodeRequest): Response<Unit>

    @POST(ACTIVATE)
    suspend fun activateEmail(@Body name: EmailActivationRequest): Response<Unit>

    companion object {
        private const val ACTIVATE = "/activate"
        private const val SEND = "/send"
    }
}

@Serializable
data class SendEmailActivationCodeRequest(val email: String)

@Serializable
data class EmailActivationRequest(val email: String, val code: String, val dryrun: Boolean)
