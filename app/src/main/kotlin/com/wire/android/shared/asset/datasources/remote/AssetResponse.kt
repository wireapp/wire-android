package com.wire.android.shared.asset.datasources.remote

import com.google.gson.annotations.SerializedName

data class AssetResponse(
    @SerializedName("size") val size: String,
    @SerializedName("key") val key: String,
    @SerializedName("type") val type: String
)
