package com.wire.android.shared.prekey.data.remote

import com.google.gson.annotations.SerializedName

typealias QualifiedPreKeyListResponse = Map<String, Map<String, Map<String, PreKeyResponse>>>

@Deprecated(
        "This data structure does not consider domain, needed for Federation",
        ReplaceWith("QualifiedPreKeyListResponse")
)
typealias PreKeyListResponse = Map<String, Map<String, PreKeyResponse>>

data class PreKeyResponse(
    @SerializedName("key")
    val key: String,

    @SerializedName("id")
    val id: Int
)
