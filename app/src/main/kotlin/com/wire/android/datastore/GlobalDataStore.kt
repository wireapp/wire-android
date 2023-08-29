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
 *
 *
 */

package com.wire.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.wire.android.BuildConfig
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.kalium.logic.configuration.dataStoreFileName
import com.wire.kalium.logic.configuration.getDataStore
import com.wire.kalium.logic.feature.auth.USER_LOGGED_IN
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import javax.inject.Inject
import javax.inject.Singleton


@Suppress("TooManyFunctions")
@Singleton
class GlobalDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val dataStore: DataStore<Preferences> = getDataStore(producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath })

    companion object {
        private const val PREFERENCES_NAME = "global_data"

        // keys
        private val SHOW_CALLING_DOUBLE_TAP_TOAST = booleanPreferencesKey("show_calling_double_tap_toast_")
        private val MIGRATION_COMPLETED = booleanPreferencesKey("migration_completed")
        private val WELCOME_SCREEN_PRESENTED = booleanPreferencesKey("welcome_screen_presented")
        private val IS_LOGGING_ENABLED = booleanPreferencesKey("is_logging_enabled")
        private val IS_ENCRYPTED_PROTEUS_STORAGE_ENABLED = booleanPreferencesKey("is_encrypted_proteus_storage_enabled")
        private fun userMigrationStatusKey(userId: String): Preferences.Key<Int> = intPreferencesKey("user_migration_status_$userId")
        private fun userDoubleTapToastStatusKey(userId: String): Preferences.Key<Boolean> =
            booleanPreferencesKey("$SHOW_CALLING_DOUBLE_TAP_TOAST$userId")

        private fun userLastMigrationAppVersion(userId: String): Preferences.Key<Int> =
            intPreferencesKey("migration_app_version_$userId")
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun getBooleanPreference(key: Preferences.Key<Boolean>, defaultValue: Boolean): Flow<Boolean> =
        dataStore.data.map { it[key] ?: defaultValue }

    private fun getNullableBooleanPreference(key: Preferences.Key<Boolean>): Flow<Boolean?> =
        dataStore.data.map { it[key] }

    fun isMigrationCompletedFlow(): Flow<Boolean> = getBooleanPreference(MIGRATION_COMPLETED, false)
    fun isLoggedIn(): Flow<Boolean?> = getNullableBooleanPreference(USER_LOGGED_IN)

    suspend fun isMigrationCompleted(): Boolean = isMigrationCompletedFlow().firstOrNull() ?: false

    fun isLoggingEnabled(): Flow<Boolean> = getBooleanPreference(IS_LOGGING_ENABLED, BuildConfig.LOGGING_ENABLED)

    suspend fun setLoggingEnabled(enabled: Boolean) {
        dataStore.edit { it[IS_LOGGING_ENABLED] = enabled }
    }

    fun isEncryptedProteusStorageEnabled(): Flow<Boolean> =
        getBooleanPreference(IS_ENCRYPTED_PROTEUS_STORAGE_ENABLED, BuildConfig.ENCRYPT_PROTEUS_STORAGE)

    suspend fun setEncryptedProteusStorageEnabled(enabled: Boolean) {
        dataStore.edit { it[IS_ENCRYPTED_PROTEUS_STORAGE_ENABLED] = enabled }
    }

    suspend fun setMigrationCompleted() {
        dataStore.edit { it[MIGRATION_COMPLETED] = true }
    }

    suspend fun isWelcomeScreenPresented(): Boolean =
        getBooleanPreference(WELCOME_SCREEN_PRESENTED, false).firstOrNull() ?: false

    suspend fun setWelcomeScreenPresented() {
        dataStore.edit { it[WELCOME_SCREEN_PRESENTED] = true }
    }

    suspend fun setWelcomeScreenNotPresented() {
        dataStore.edit { it[WELCOME_SCREEN_PRESENTED] = false }
    }

    suspend fun setUserMigrationStatus(userId: String, status: UserMigrationStatus) {
        dataStore.edit { it[userMigrationStatusKey(userId)] = status.value }
        when (status) {
            UserMigrationStatus.Completed,
            UserMigrationStatus.CompletedWithErrors,
            UserMigrationStatus.Successfully -> setUserMigrationAppVersion(userId, BuildConfig.VERSION_CODE)

            UserMigrationStatus.NoNeed,
            UserMigrationStatus.NotStarted -> {
                /* no-op */
            }
        }
    }

    /**
     * Returns the migration status of the user with the given [userId].
     * If there is no status stored, the status will be [UserMigrationStatus.NoNeed]
     * meaning that the user does not need to be migrated.
     */
    fun getUserMigrationStatus(userId: String): Flow<UserMigrationStatus?> =
        dataStore.data.map { it[userMigrationStatusKey(userId)]?.let { status -> UserMigrationStatus.fromInt(status) } }

    suspend fun setUserMigrationAppVersion(userId: String, version: Int) {
        dataStore.edit { it[userLastMigrationAppVersion(userId)] = version }
    }

    suspend fun getUserMigrationAppVersion(userId: String): Int? =
        dataStore.data.map { it[userLastMigrationAppVersion(userId)] }.firstOrNull()

    suspend fun setShouldShowDoubleTapToastStatus(userId: String, shouldShow: Boolean) {
        dataStore.edit { it[userDoubleTapToastStatusKey(userId)] = shouldShow }
    }

    suspend fun getShouldShowDoubleTapToast(userId: String): Boolean =
        getBooleanPreference(userDoubleTapToastStatusKey(userId), true).first()
}
