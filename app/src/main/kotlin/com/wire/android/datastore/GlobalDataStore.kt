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
import androidx.datastore.preferences.preferencesDataStore
import com.wire.android.BuildConfig
import com.wire.android.migration.failure.UserMigrationStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class GlobalDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val PREFERENCES_NAME = "global_data"

        // keys
        private val MIGRATION_COMPLETED = booleanPreferencesKey("migration_completed")
        private val WELCOME_SCREEN_PRESENTED = booleanPreferencesKey("welcome_screen_presented")
        private val IS_LOGGING_ENABLED = booleanPreferencesKey("is_logging_enabled")
        private val IS_ENCRYPTED_PROTEUS_STORAGE_ENABLED = booleanPreferencesKey("is_encrypted_proteus_storage_enabled")
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)
        private fun userMigrationStatusKey(userId: String): Preferences.Key<Int> = intPreferencesKey("user_migration_status_$userId")
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    private fun getBooleanPreference(key: Preferences.Key<Boolean>, defaultValue: Boolean): Flow<Boolean> =
        context.dataStore.data.map { it[key] ?: defaultValue }

    fun isMigrationCompletedFlow(): Flow<Boolean> = getBooleanPreference(MIGRATION_COMPLETED, false)

    suspend fun isMigrationCompleted(): Boolean = isMigrationCompletedFlow().firstOrNull() ?: false

    fun isLoggingEnabled(): Flow<Boolean> = getBooleanPreference(IS_LOGGING_ENABLED, BuildConfig.LOGGING_ENABLED)

    suspend fun setLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_LOGGING_ENABLED] = enabled }
    }

    fun isEncryptedProteusStorageEnabled(): Flow<Boolean> =
        getBooleanPreference(IS_ENCRYPTED_PROTEUS_STORAGE_ENABLED, BuildConfig.ENCRYPT_PROTEUS_STORAGE)

    suspend fun setEncryptedProteusStorageEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_ENCRYPTED_PROTEUS_STORAGE_ENABLED] = enabled }
    }

    suspend fun setMigrationCompleted() {
        context.dataStore.edit { it[MIGRATION_COMPLETED] = true }
    }

    suspend fun isWelcomeScreenPresented(): Boolean =
        getBooleanPreference(WELCOME_SCREEN_PRESENTED, false).firstOrNull() ?: false

    suspend fun setWelcomeScreenPresented() {
        context.dataStore.edit { it[WELCOME_SCREEN_PRESENTED] = true }
    }

    suspend fun setWelcomeScreenNotPresented() {
        context.dataStore.edit { it[WELCOME_SCREEN_PRESENTED] = false }
    }

    suspend fun setUserMigrationStatus(userId: String, status: UserMigrationStatus) {
        context.dataStore.edit { it[userMigrationStatusKey(userId)] = status.value }
    }

    /**
     * Returns the migration status of the user with the given [userId].
     * If there is no status stored, the status will be [UserMigrationStatus.NoNeed]
     * meaning that the user does not need to be migrated.
     */
    fun getUserMigrationStatus(userId: String): Flow<UserMigrationStatus?> =
        context.dataStore.data.map { it[userMigrationStatusKey(userId)]?.let { status -> UserMigrationStatus.fromInt(status) } }
}
