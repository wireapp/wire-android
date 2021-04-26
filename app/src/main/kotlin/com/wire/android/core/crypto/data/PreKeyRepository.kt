package com.wire.android.core.crypto.data

import com.wire.android.core.crypto.model.UserID

interface PreKeyRepository {
    fun updateLastPreKeyIDForUser(userID: UserID, preKeyId: Int)

    fun lastPreKeyIdForUser(userID: UserID): Int?
}
