package com.wire.android.feature.auth.client

import com.wire.android.core.crypto.model.PreKey

data class Client(
    val id: String,
    val type: String,
    val label: String,
    val password: String,
    val model: String,
    val deviceType: String,
    val signalingKey: SignalingKey,
    val preKeys: List<PreKey>,
    val lastKey: PreKey
)


data class SignalingKey(
    val encryptionKey: String,
    val macKey: String
)

enum class ClientType(val type: String) {
    PERMANENT("permanent"),
    TEMPORARY("temporary"),
    LEGALHOLD("legalhold")
}
