/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.applock.passcode

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wire.android.datastore.EncryptionManager
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.GlobalDataStore.Companion.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val IS_APP_LOCKED_BY_USER = booleanPreferencesKey("is_app_locked_by_user")
private val APP_LOCK_PASSCODE = stringPreferencesKey("app_lock_passcode")

/**
 * returns a flow with decoded passcode
 */
@Suppress("TooGenericExceptionCaught")
fun GlobalDataStore.getAppLockPasscodeFlow(): Flow<String?> =
    context.dataStore.data.map {
        it[APP_LOCK_PASSCODE]?.let { passcode ->
            try {
                EncryptionManager.decrypt(APP_LOCK_PASSCODE.name, passcode)
            } catch (e: Exception) {
                null
            }
        }
    }

/**
 * returns a flow only informing whether the passcode is set, without the need to decode it
 */
fun GlobalDataStore.isAppLockPasscodeSetFlow(): Flow<Boolean> =
    context.dataStore.data.map {
        it.contains(APP_LOCK_PASSCODE)
    }

suspend fun GlobalDataStore.clearAppLockPasscode() {
    context.dataStore.edit {
        it.remove(APP_LOCK_PASSCODE)
    }
}

@Suppress("TooGenericExceptionCaught")
suspend fun GlobalDataStore.setAppLockPasscode(passcode: String) {
    context.dataStore.edit {
        try {
            val encrypted =
                EncryptionManager.encrypt(APP_LOCK_PASSCODE.name, passcode)
            it[APP_LOCK_PASSCODE] = encrypted
        } catch (e: Exception) {
            it.remove(APP_LOCK_PASSCODE)
        }
    }
}

fun GlobalDataStore.isAppLockedByUserFlow(): Flow<Boolean> = getBooleanPreference(IS_APP_LOCKED_BY_USER, false)

suspend fun GlobalDataStore.setAppLockedByUser(isLocked: Boolean) {
    context.dataStore.edit { it[IS_APP_LOCKED_BY_USER] = isLocked }
}