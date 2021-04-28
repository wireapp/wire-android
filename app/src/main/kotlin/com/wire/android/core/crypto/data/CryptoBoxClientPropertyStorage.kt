package com.wire.android.core.crypto.data

import android.content.Context
import android.content.SharedPreferences
import com.wire.android.core.crypto.model.UserId

class CryptoBoxClientPropertyStorage(private val context: Context) {

    private fun <R> sharedPreferencesForUser(userId: UserId, apply: SharedPreferences.() -> R): R {
        val prefs = context.getSharedPreferences(
            PREF_FILE_PREFIX + userId.toString(),
            Context.MODE_PRIVATE
        )
        return prefs.apply()
    }

    fun updateLastPreKeyId(userId: UserId, preKeyId: Int) = sharedPreferencesForUser(userId) {
        edit().putInt(LAST_PRE_KEY_PREF_ID, preKeyId).apply()
    }

    fun lastPreKeyId(userId: UserId): Int? = sharedPreferencesForUser(userId) {
        getInt(LAST_PRE_KEY_PREF_ID, Int.MIN_VALUE)
            .takeUnless { it == Int.MIN_VALUE }
    }

    companion object {
        private const val PREF_FILE_PREFIX = "preKeyPrefs_"
        private const val LAST_PRE_KEY_PREF_ID = "lastPreKeyID"
    }
}
