package com.wire.android.shared.user.datasources.remote.username

import com.google.gson.annotations.SerializedName

data class ChangeHandleRequest(
    @SerializedName("handle") val handle: String
)
