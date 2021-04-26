package com.wire.android.core.crypto.storage

interface PreKeyRepository {
    fun updateLastPreKeyID(preKeyId: Int)

    fun lastPreKeyId(): Int?
}
