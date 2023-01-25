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
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val PREFERENCES_NAME = "global_data"

        // keys
        private val MIGRATION_COMPLETED = booleanPreferencesKey("migration_completed")
        private val WELCOME_SCREEN_PRESENTED = booleanPreferencesKey("welcome_screen_presented")
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    private fun getBooleanPreference(key: Preferences.Key<Boolean>, defaultValue: Boolean): Flow<Boolean> =
        context.dataStore.data.map { it[key] ?: defaultValue }

    fun isMigrationCompletedFlow(): Flow<Boolean> = getBooleanPreference(MIGRATION_COMPLETED, false)

    suspend fun isMigrationCompleted(): Boolean = isMigrationCompletedFlow().firstOrNull() ?: false


    suspend fun setMigrationCompleted() {
        context.dataStore.edit { it[MIGRATION_COMPLETED] = true }
    }

    suspend fun isWelcomeScreenPresented(): Boolean = getBooleanPreference(WELCOME_SCREEN_PRESENTED, false).firstOrNull() ?: false

    suspend fun setWelcomeScreenPresented() {
        context.dataStore.edit { it[WELCOME_SCREEN_PRESENTED] = true }
    }

    suspend fun setWelcomeScreenNotPresented() {
        context.dataStore.edit { it[WELCOME_SCREEN_PRESENTED] = false }
    }
}
