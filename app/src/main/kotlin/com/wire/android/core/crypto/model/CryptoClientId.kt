package com.wire.android.core.crypto.model

// TODO: Maybe replace by ClientId that is being developed in order to register a client?
data class CryptoClientId(val value: String) {
    override fun toString() = value
}
