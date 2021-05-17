package com.wire.android.feature.auth.client.datasource.remote.api

import com.google.gson.annotations.SerializedName

data class ClientRegistrationRequest(
    @SerializedName("cookie") val refreshToken : String,
    @SerializedName("lastKey") val lastKey: PreKeyRequest,
    @SerializedName("prekeys") val preKeys: List<PreKeyRequest>,
    @SerializedName("sigkeys") val signalingKey: SignalingKeyRequest,
    @SerializedName("type") val type: String,
    @SerializedName("class") val deviceType: String,
    @SerializedName("model") val model: String?,
    @SerializedName("password") val password: String?,
    @SerializedName("label") val label: String?
)

data class PreKeyRequest(
    @SerializedName("id") val id : Int,
    @SerializedName("key") val key: String
)

data class SignalingKeyRequest(
    @SerializedName("enckey") val encryptionKey: String = "",
    @SerializedName("mackey") val macKey: String = ""
)
