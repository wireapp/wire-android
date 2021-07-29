package com.wire.android.core.crypto.model

data class CryptoSessionId(val userId: UserId, val cryptoClientId: CryptoClientId) {
    val value: String = "${userId}_${cryptoClientId}"
}
