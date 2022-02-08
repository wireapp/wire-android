package com.wire.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val PREFERENCES_NAME = "user_data"

        //keys
        private val SHOW_STATUS_RATIONALE_AVAILABLE = booleanPreferencesKey("show_status_rationale_available")
        private val SHOW_STATUS_RATIONALE_BUSY = booleanPreferencesKey("show_status_rationale_busy")
        private val SHOW_STATUS_RATIONALE_AWAY = booleanPreferencesKey("show_status_rationale_away")
        private val SHOW_STATUS_RATIONALE_NONE = booleanPreferencesKey("show_status_rationale_none")
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

    val shouldShowStatusRationaleAvailableFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_STATUS_RATIONALE_AVAILABLE] ?: true
        }

    suspend fun donNotShowStatusRationaleAvailable() {
        context.dataStore.edit { preferences ->
            preferences[SHOW_STATUS_RATIONALE_AVAILABLE] = false
        }
    }

    val shouldShowStatusRationaleBusyFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_STATUS_RATIONALE_BUSY] ?: true
        }

    suspend fun donNotShowStatusRationaleBusy() {
        context.dataStore.edit { preferences ->
            preferences[SHOW_STATUS_RATIONALE_BUSY] = false
        }
    }

    val shouldShowStatusRationaleAwayFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_STATUS_RATIONALE_AWAY] ?: true
        }

    suspend fun donNotShowStatusRationaleAway() {
        context.dataStore.edit { preferences ->
            preferences[SHOW_STATUS_RATIONALE_AWAY] = false
        }
    }

    val shouldShowStatusRationaleNoneFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_STATUS_RATIONALE_NONE] ?: true
        }

    suspend fun donNotShowStatusRationaleNone() {
        context.dataStore.edit { preferences ->
            preferences[SHOW_STATUS_RATIONALE_NONE] = false
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
