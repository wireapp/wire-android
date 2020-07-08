package com.wire.android.feature.auth.activation.datasource.remote

import com.google.gson.annotations.SerializedName
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

data class SendEmailActivationCodeRequest(@SerializedName("email") val email: String)

data class EmailActivationRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("dryrun") val dryrun: Boolean
)
