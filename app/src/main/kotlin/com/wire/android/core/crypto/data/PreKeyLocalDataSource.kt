package com.wire.android.core.crypto.data

import android.content.Context
import android.content.SharedPreferences
import com.wire.android.core.crypto.model.UserID

class PreKeyLocalDataSource(
    private val context: Context
) {

    private fun <R> sharedPreferencesForUser(userID: UserID, apply: SharedPreferences.() -> R): R {
        val prefs = context.getSharedPreferences(
            PREF_FILE_PREFIX + userID.toString(),
            Context.MODE_PRIVATE
        )
        return prefs.apply()
    }

    fun updateLastPreKeyID(userID: UserID, preKeyId: Int) = sharedPreferencesForUser(userID) {
        edit().putInt(LAST_PRE_KEY_PREF_ID, preKeyId).apply()
    }

    fun lastPreKeyId(userID: UserID): Int? = sharedPreferencesForUser(userID) {
        getInt(LAST_PRE_KEY_PREF_ID, Int.MIN_VALUE)
            .takeUnless { it == Int.MIN_VALUE }
    }

    companion object {
        private const val PREF_FILE_PREFIX = "preKeyPrefs_"
        private const val LAST_PRE_KEY_PREF_ID = "lastPreKeyID"
    }
}
