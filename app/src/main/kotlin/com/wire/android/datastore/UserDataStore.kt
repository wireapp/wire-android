package com.wire.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    private fun getStatusKey(status: UserAvailabilityStatus) = when (status) {
        UserAvailabilityStatus.AVAILABLE -> SHOW_STATUS_RATIONALE_AVAILABLE
        UserAvailabilityStatus.BUSY -> SHOW_STATUS_RATIONALE_BUSY
        UserAvailabilityStatus.AWAY -> SHOW_STATUS_RATIONALE_AWAY
        UserAvailabilityStatus.NONE -> SHOW_STATUS_RATIONALE_NONE
    }

    val initialSyncCompleted: Flow<Boolean> = context.dataStore.data.map { it[INITIAL_SYNC_COMPLETED] ?: false }

    suspend fun setInitialSyncCompleted() { context.dataStore.edit { it[INITIAL_SYNC_COMPLETED] = true } }

    companion object {
        private const val PREFERENCES_NAME = "user_data"

        // keys
        private val SHOW_STATUS_RATIONALE_AVAILABLE = booleanPreferencesKey("show_status_rationale_available")
        private val SHOW_STATUS_RATIONALE_BUSY = booleanPreferencesKey("show_status_rationale_busy")
        private val SHOW_STATUS_RATIONALE_AWAY = booleanPreferencesKey("show_status_rationale_away")
        private val SHOW_STATUS_RATIONALE_NONE = booleanPreferencesKey("show_status_rationale_none")
        private val USER_AVATAR_ASSET_ID = stringPreferencesKey("user_avatar_asset_id")
        private val INITIAL_SYNC_COMPLETED = booleanPreferencesKey("initial_sync_completed")
    }

}
