package com.wire.android.shared.prekey.data.remote

import com.google.gson.annotations.SerializedName

typealias PreKeyListResponse = Map<String, Map<String, Map<String, PreKeyResponse>>>

data class PreKeyResponse(
    @SerializedName("key")
    val key: String,

    @SerializedName("id")
    val id: Int
)
