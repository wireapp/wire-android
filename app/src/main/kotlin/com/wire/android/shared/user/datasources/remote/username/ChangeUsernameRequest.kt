package com.wire.android.shared.user.datasources.remote.username

import com.google.gson.annotations.SerializedName

data class ChangeUsernameRequest(
    @SerializedName("handle") val handle: String
)