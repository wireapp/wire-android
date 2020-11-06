package com.wire.android.core.network.auth.accesstoken

import com.wire.android.core.extension.EMPTY
import java.time.Instant

data class RefreshToken(val token: String) {

    val expiryDate by lazy { calculateExpiryDate() }

    private fun calculateExpiryDate(): Instant? {
        val parts = token.split('.')
        val datePart = parts.find { it.contains("d=") }?.drop(2)
        return datePart?.toLong()?.let { Instant.ofEpochSecond(it) }
    }

    companion object {
        val EMPTY = RefreshToken(String.EMPTY)
    }
}

class RefreshTokenMapper {
    fun fromTokenText(tokenText: String) = RefreshToken(tokenText)

    fun toEntity(refreshToken: RefreshToken): String = refreshToken.token
}
