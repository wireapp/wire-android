package com.wire.android.shared.user

import com.wire.android.core.extension.EMPTY

data class UserSession(
    val userId: String,
    val accessToken: String,
    val tokenType: String,
    val refreshToken: String
) {
    companion object {
        val EMPTY = UserSession(userId = String.EMPTY, accessToken = String.EMPTY, tokenType = String.EMPTY, refreshToken = String.EMPTY)
    }
}
