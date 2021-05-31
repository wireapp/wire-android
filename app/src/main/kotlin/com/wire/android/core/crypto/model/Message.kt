package com.wire.android.core.crypto.model

data class EncryptedMessage(val data: ByteArray) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is EncryptedMessage && other.data.contentEquals(data))

    override fun hashCode() = data.contentHashCode()
}

data class PlainMessage(val data: ByteArray) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is PlainMessage && other.data.contentEquals(data))

    override fun hashCode() = data.contentHashCode()
}
