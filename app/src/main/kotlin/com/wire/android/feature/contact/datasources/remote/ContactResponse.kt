package com.wire.android.feature.contact.datasources.remote

import com.google.gson.annotations.SerializedName
import com.wire.android.shared.asset.datasources.remote.AssetResponse

data class ContactResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("handle") val handle: String,
    @SerializedName("locale") val locale: String,
    @SerializedName("accent_id") val accentId: String,
    @SerializedName("assets") val assets: List<AssetResponse>,
)
