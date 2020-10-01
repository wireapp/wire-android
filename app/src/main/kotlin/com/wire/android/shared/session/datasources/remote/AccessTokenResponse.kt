package com.wire.android.shared.session.datasources.remote

import com.google.gson.annotations.SerializedName

data class AccessTokenResponse(
    @SerializedName("expires_in") val expiresIn: Long,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("user") val userId: String,
    @SerializedName("token_type") val tokenType: String
)
