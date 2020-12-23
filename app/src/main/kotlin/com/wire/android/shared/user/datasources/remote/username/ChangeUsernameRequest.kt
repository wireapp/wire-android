package com.wire.android.shared.user.datasources.remote.username

import com.google.gson.annotations.SerializedName

data class ChangeUsernameRequest(
    @SerializedName("handle") val handle: String
)

data class CheckHandlesExistRequest(
    @SerializedName("return") val handlesReturned: Int = 10,
    @SerializedName("handles") val handles: List<String>
)
