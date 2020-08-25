package com.wire.android.feature.auth.login.email.datasource.remote

import com.google.gson.annotations.SerializedName

data class LoginWithEmailRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String, //TODO: prevent this from being logged
    @SerializedName("label") val label: String
)

data class LoginWithEmailResponse(
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("user") val userId: String,
    @SerializedName("token_type") val tokenType: String
)
