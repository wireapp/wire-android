package com.wire.android.core.crypto.data

import com.wire.android.core.crypto.model.UserId

class PreKeyDataSource(private val preKeyLocalDataSource: PreKeyLocalDataSource) : PreKeyRepository {

    override fun updateLastPreKeyIdForUser(userId: UserId, preKeyId: Int) {
        preKeyLocalDataSource.updateLastPreKeyId(userId, preKeyId)
    }

    override fun lastPreKeyIdForUser(userId: UserId): Int? {
        return preKeyLocalDataSource.lastPreKeyId(userId)
    }

}
