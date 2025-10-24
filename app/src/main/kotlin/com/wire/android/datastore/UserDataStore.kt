/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Suppress("TooManyFunctions")
class UserDataStore(private val context: Context, userId: UserId) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "${PREFERENCES_NAME}_$userId")

    suspend fun dontShowStatusRationaleAgain(status: UserAvailabilityStatus) {
        context.dataStore.edit { preferences ->
            preferences[getStatusKey(status)] = false
        }
    }

    fun shouldShowStatusRationaleFlow(status: UserAvailabilityStatus): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[getStatusKey(status)] ?: true
        }

    val avatarAssetId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_AVATAR_ASSET_ID]
        }

    suspend fun updateUserAvatarAssetId(newAssetId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AVATAR_ASSET_ID] = newAssetId
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    fun lastBackupDateSeconds(): Flow<Long?> = context.dataStore.data.map { it[LAST_BACKUP_DATE_INSTANT] }

    suspend fun setLastBackupDateSeconds(timeStampSeconds: Long) {
        context.dataStore.edit { it[LAST_BACKUP_DATE_INSTANT] = timeStampSeconds }
    }

    private fun getStatusKey(status: UserAvailabilityStatus) = when (status) {
        UserAvailabilityStatus.AVAILABLE -> SHOW_STATUS_RATIONALE_AVAILABLE
        UserAvailabilityStatus.BUSY -> SHOW_STATUS_RATIONALE_BUSY
        UserAvailabilityStatus.AWAY -> SHOW_STATUS_RATIONALE_AWAY
        UserAvailabilityStatus.NONE -> SHOW_STATUS_RATIONALE_NONE
    }

    val initialSyncCompleted: Flow<Boolean> = context.dataStore.data.map { it[INITIAL_SYNC_COMPLETED] ?: false }

    suspend fun setInitialSyncCompleted() { context.dataStore.edit { it[INITIAL_SYNC_COMPLETED] = true } }

    fun isAnonymousUsageDataEnabled(): Flow<Boolean> = context.dataStore.data.map { it[ANONYMOUS_ANALYTICS] ?: false }

    suspend fun setIsAnonymousAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ANONYMOUS_ANALYTICS] = enabled }
    }

    fun isAnalyticsDialogSeen(): Flow<Boolean> = context.dataStore.data.map { it[ANALYTICS_DIALOG_SEEN] ?: false }

    suspend fun setIsAnalyticsDialogSeen() {
        context.dataStore.edit { it[ANALYTICS_DIALOG_SEEN] = true }
    }

    fun isCreateTeamNoticeRead(): Flow<Boolean> = context.dataStore.data.map {
        it[IS_CREATE_TEAM_NOTICE_READ] ?: false
    }

    suspend fun setIsCreateTeamNoticeRead(isRead: Boolean) {
        context.dataStore.edit { it[IS_CREATE_TEAM_NOTICE_READ] = isRead }
    }

    companion object {
        private const val PREFERENCES_NAME = "user_data"

        // keys
        private val SHOW_STATUS_RATIONALE_AVAILABLE = booleanPreferencesKey("show_status_rationale_available")
        private val SHOW_STATUS_RATIONALE_BUSY = booleanPreferencesKey("show_status_rationale_busy")
        private val SHOW_STATUS_RATIONALE_AWAY = booleanPreferencesKey("show_status_rationale_away")
        private val SHOW_STATUS_RATIONALE_NONE = booleanPreferencesKey("show_status_rationale_none")
        private val USER_AVATAR_ASSET_ID = stringPreferencesKey("user_avatar_asset_id")
        private val INITIAL_SYNC_COMPLETED = booleanPreferencesKey("initial_sync_completed")
        private val LAST_BACKUP_DATE_INSTANT = longPreferencesKey("last_backup_date_instant")
        private val ANONYMOUS_ANALYTICS = booleanPreferencesKey("anonymous_analytics")
        private val ANALYTICS_DIALOG_SEEN = booleanPreferencesKey("analytics_dialog_seen")
        private val IS_CREATE_TEAM_NOTICE_READ = booleanPreferencesKey("is_create_team_notice_read")
    }
}
