package com.wire.android.crypto.storage

interface PreKeyRepository {
    fun updateLastPreKeyID(preKeyId: Int)

    fun lastPreKeyId(): Int?
}
