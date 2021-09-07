package com.wire.android.shared.session

import com.wire.android.core.extension.EMPTY

data class Session(
    val userId: String,
    val clientId: String?,
    val accessToken: String,
    val tokenType: String,
    val refreshToken: String
) {
    companion object {
        val EMPTY = Session(String.EMPTY, String.EMPTY, String.EMPTY, String.EMPTY, String.EMPTY)
    }
}
