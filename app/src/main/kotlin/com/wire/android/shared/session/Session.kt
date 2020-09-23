package com.wire.android.shared.session

import com.wire.android.core.extension.EMPTY

data class Session(
    val userId: String,
    val accessToken: String? = null,
    val tokenType: String? = null,
    val refreshToken: String
) {
    companion object {
        val EMPTY = Session(String.EMPTY, null, null, String.EMPTY)
    }
}
