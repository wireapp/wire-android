package com.wire.android.shared.user.datasources.remote

import com.google.gson.annotations.SerializedName
import com.wire.android.feature.auth.registration.datasource.remote.UserAsset

data class SelfUserResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("handle") val handle: String? = null,
    @SerializedName("locale") val locale: String,
    @SerializedName("managed_by") val managedBy: String? = null,
    @SerializedName("accent_id") val accentColorId: Int? = null,
    @SerializedName("deleted") val deleted: Boolean? = null,
    @SerializedName("assets") val assets: List<UserAsset>
)
