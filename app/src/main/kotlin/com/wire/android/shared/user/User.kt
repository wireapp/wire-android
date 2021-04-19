package com.wire.android.shared.user

import com.wire.android.shared.asset.Asset

data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val username: String? = null,
    var assetKey: String? = null,
    val profilePicture: Asset? = null
)
