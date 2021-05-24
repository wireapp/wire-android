package com.wire.android.feature.auth.client.datasource.remote.api

import com.google.gson.annotations.SerializedName
import com.wire.android.core.extension.EMPTY

data class ClientRegistrationRequest(
    @SerializedName("cookie") val refreshToken : String,
    @SerializedName("lastkey") val lastKey: PreKeyRequest,
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
    @SerializedName("enckey") val encryptionKey: String = String.EMPTY,
    @SerializedName("mackey") val macKey: String = String.EMPTY
)
