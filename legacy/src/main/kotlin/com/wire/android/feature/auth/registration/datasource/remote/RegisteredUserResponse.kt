package com.wire.android.feature.auth.registration.datasource.remote

import com.google.gson.annotations.SerializedName
import com.wire.android.shared.asset.datasources.remote.AssetResponse

data class RegisteredUserResponse(
    @SerializedName("email") val email: String? = null,
    @SerializedName("handle") val handle: String? = null,
    @SerializedName("service") val service: ServiceRef? = null,
    @SerializedName("accent_id") val accentId: Int? = null,
    @SerializedName("name") val name: String,
    @SerializedName("team") val team: String? = null,
    @SerializedName("id") val id: String,
    @SerializedName("deleted") val deleted: Boolean? = null,
    @SerializedName("assets") val assets: List<AssetResponse>
)

data class ServiceRef(
    @SerializedName("id") val id: String,
    @SerializedName("provider") val provider: String
)
