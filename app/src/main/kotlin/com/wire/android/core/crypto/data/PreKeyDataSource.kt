package com.wire.android.core.crypto.data

import com.wire.android.core.crypto.model.UserID

class PreKeyDataSource(private val preKeyLocalDataSource: PreKeyLocalDataSource) : PreKeyRepository {

    override fun updateLastPreKeyIDForUser(userID: UserID, preKeyId: Int) {
        preKeyLocalDataSource.updateLastPreKeyID(userID, preKeyId)
    }

    override fun lastPreKeyIdForUser(userID: UserID): Int? {
        return preKeyLocalDataSource.lastPreKeyId(userID)
    }

}
