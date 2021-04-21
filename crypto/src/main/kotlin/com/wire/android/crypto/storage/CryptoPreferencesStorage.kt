package com.wire.android.crypto.storage

import android.content.Context
import com.chibatching.kotpref.KotprefModel
import com.wire.android.crypto.model.UserID

class CryptoPreferencesStorage(
    context: Context,
    userID: UserID
) : KotprefModel(context), PreKeyRepository {

    override val kotprefName: String = "$PREF_FILE_PREFIX$userID"

    private var _lastPreKeyID: Int by intPref(key = "lastPreKeyID")

    override fun updateLastPreKeyID(preKeyId: Int) {
        _lastPreKeyID = preKeyId
    }

    override fun lastPreKeyId(): Int? {
        return _lastPreKeyID
    }

    companion object {
        private const val PREF_FILE_PREFIX = "preKeyPrefs_"
    }
}
