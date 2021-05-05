package com.wire.android.core.crypto.model

data class SessionId(val userId: UserId, val clientId: ClientId){
    val value: String = "${userId}_${clientId}"

    override fun toString() = value
}
