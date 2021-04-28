package com.wire.android.core.crypto.data

import com.wire.android.core.crypto.model.UserId

interface PreKeyRepository {
    fun updateLastPreKeyIdForUser(userId: UserId, preKeyId: Int)

    fun lastPreKeyIdForUser(userId: UserId): Int?
}
