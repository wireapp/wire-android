package com.wire.android.core.crypto.model

data class CryptoSessionId(val userId: UserId, val clientId: ClientId){
    val value: String = "${userId}_${clientId}"
}
