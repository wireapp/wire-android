package com.wire.android.core.storage.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class GlobalPreferences(context: Context) {

    //TODO: can be migrated to EncryptedSharedPreferences or Jetpack DataStore
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    var activeUserId: String?
        get() = sharedPreferences.getString(ACTIVE_USER_PREF_KEY, null)
        set(value) = sharedPreferences.edit { putString(ACTIVE_USER_PREF_KEY, value) }

    companion object {
        private const val PREF_FILE_NAME = "globalPreferences"

        private const val ACTIVE_USER_PREF_KEY = "activeUser"
    }
}
