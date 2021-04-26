package com.wire.android.core.crypto.storage

import android.content.Context
import com.wire.android.core.crypto.model.UserID

class CryptoPreferencesStorage(
    context: Context,
    userID: UserID
) : PreKeyRepository {

    private val sharedPreferences =
        context.getSharedPreferences(
            PREF_FILE_PREFIX + userID.toString(),
            Context.MODE_PRIVATE
        )

    override fun updateLastPreKeyID(preKeyId: Int) {
        sharedPreferences.edit().putInt(LAST_PRE_KEY_PREF_ID, preKeyId).apply()
    }

    override fun lastPreKeyId(): Int? {
        return sharedPreferences.getInt(LAST_PRE_KEY_PREF_ID, Int.MIN_VALUE)
            .takeUnless { it == Int.MIN_VALUE }
    }

    companion object {
        private const val PREF_FILE_PREFIX = "preKeyPrefs_"
        private const val LAST_PRE_KEY_PREF_ID = "lastPreKeyID"
    }
}
