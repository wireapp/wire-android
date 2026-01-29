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
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wire.android.BuildConfig
import com.wire.android.feature.AppLockSource
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.util.sha256
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Singleton
class GlobalDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val PREFERENCES_NAME = "global_data"

        // keys
        private val SHOW_CALLING_DOUBLE_TAP_TOAST =
            booleanPreferencesKey("show_calling_double_tap_toast_")
        private val WELCOME_SCREEN_PRESENTED = booleanPreferencesKey("welcome_screen_presented")
        private val IS_LOGGING_ENABLED = booleanPreferencesKey("is_logging_enabled")
        private val APP_LOCK_PASSCODE = stringPreferencesKey("app_lock_passcode")
        private val APP_LOCK_SOURCE = intPreferencesKey("app_lock_source")
        private val ENTER_TO_SENT = booleanPreferencesKey("enter_to_sent")
        private val ANONYMOUS_REGISTRATION_TRACK_ID = stringPreferencesKey("anonymous_registration_track_id")
        private val IS_ANONYMOUS_REGISTRATION_ENABLED = booleanPreferencesKey("is_anonymous_registration_enabled")

        val APP_THEME_OPTION = stringPreferencesKey("app_theme_option")
        val RECORD_AUDIO_EFFECTS_CHECKBOX = booleanPreferencesKey("record_audio_effects_checkbox")

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

        private fun userDoubleTapToastStatusKey(userId: String): Preferences.Key<Boolean> =
            booleanPreferencesKey("$SHOW_CALLING_DOUBLE_TAP_TOAST$userId")
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    fun getBooleanPreference(key: Preferences.Key<Boolean>, defaultValue: Boolean): Flow<Boolean> =
        context.dataStore.data.map { it[key] ?: defaultValue }

    private fun getStringPreference(
        key: Preferences.Key<String>,
        defaultValue: String
    ): Flow<String> =
        context.dataStore.data.map { it[key] ?: defaultValue }

    fun isLoggingEnabled(): Flow<Boolean> =
        getBooleanPreference(IS_LOGGING_ENABLED, BuildConfig.LOGGING_ENABLED)

    suspend fun setLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_LOGGING_ENABLED] = enabled }
    }

    fun isRecordAudioEffectsCheckboxEnabled(): Flow<Boolean> =
        getBooleanPreference(RECORD_AUDIO_EFFECTS_CHECKBOX, false)

    suspend fun setRecordAudioEffectsCheckboxEnabled(enabled: Boolean) {
        context.dataStore.edit { it[RECORD_AUDIO_EFFECTS_CHECKBOX] = enabled }
    }

    suspend fun isWelcomeScreenPresented(): Boolean =
        getBooleanPreference(WELCOME_SCREEN_PRESENTED, false).firstOrNull() ?: false

    suspend fun setShouldShowDoubleTapToastStatus(userId: String, shouldShow: Boolean) {
        context.dataStore.edit { it[userDoubleTapToastStatusKey(userId)] = shouldShow }
    }

    fun isAnonymousRegistrationEnabled(): Flow<Boolean> =
        getBooleanPreference(IS_ANONYMOUS_REGISTRATION_ENABLED, false)

    suspend fun setAnonymousRegistrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_ANONYMOUS_REGISTRATION_ENABLED] = enabled }
    }

    suspend fun clearAnonymousRegistrationTrackId() {
        context.dataStore.edit {
            it.remove(ANONYMOUS_REGISTRATION_TRACK_ID)
        }
    }

    private suspend fun setAnonymousRegistrationTrackId(trackId: String) {
        context.dataStore.edit {
            it[ANONYMOUS_REGISTRATION_TRACK_ID] = trackId
        }
    }

    suspend fun getAnonymousRegistrationTrackId(): String? {
        return context.dataStore.data.firstOrNull()?.get(ANONYMOUS_REGISTRATION_TRACK_ID)
    }

    suspend fun getOrCreateAnonymousRegistrationTrackId(): String {
        val trackId = context.dataStore.data.first()[ANONYMOUS_REGISTRATION_TRACK_ID]
        if (trackId.isNullOrBlank()) {
            val newTrackId = UUID.randomUUID().toString()
            setAnonymousRegistrationTrackId(newTrackId)
            return newTrackId
        }
        return trackId
    }

    suspend fun getShouldShowDoubleTapToast(userId: String): Boolean =
        getBooleanPreference(userDoubleTapToastStatusKey(userId), true).first()

    /**
     * returns a flow with decoded passcode
     */
    @Suppress("TooGenericExceptionCaught")
    fun getAppLockPasscodeFlow(): Flow<String?> {
        return context.dataStore.data.map {
            it[APP_LOCK_PASSCODE]?.let { passcode ->
                try {
                    EncryptionManager.decrypt(APP_LOCK_PASSCODE.name, passcode)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    /**
     * returns a flow only informing whether the passcode is set, without the need to decode it
     */
    fun isAppLockPasscodeSetFlow(): Flow<Boolean> =
        context.dataStore.data.map {
            it.contains(APP_LOCK_PASSCODE)
        }

    suspend fun clearAppLockPasscode() {
        context.dataStore.edit {
            it.remove(APP_LOCK_PASSCODE)
            it.remove(APP_LOCK_SOURCE)
        }
    }

    suspend fun getAppLockSource(): AppLockSource {
        return context.dataStore.data.map {
            it[APP_LOCK_SOURCE]?.let { source ->
                AppLockSource.fromInt(source)
            }
        }.firstOrNull() ?: AppLockSource.Manual
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun setUserAppLock(
        passcode: String,
        source: AppLockSource
    ) {
        context.dataStore.edit {
            try {
                val hash = passcode.sha256()
                val encrypted =
                    EncryptionManager.encrypt(APP_LOCK_PASSCODE.name, hash)
                it[APP_LOCK_PASSCODE] = encrypted
                it[APP_LOCK_SOURCE] = source.code
            } catch (e: Exception) {
                it.remove(APP_LOCK_PASSCODE)
            }
        }
    }

    suspend fun setThemeOption(option: ThemeOption) {
        context.dataStore.edit { it[APP_THEME_OPTION] = option.toString() }
    }

    fun selectedThemeOptionFlow(): Flow<ThemeOption> =
        getStringPreference(APP_THEME_OPTION, ThemeOption.SYSTEM.toString())
            .map { ThemeOption.valueOf(it) }

    fun enterToSendFlow(): Flow<Boolean> = getBooleanPreference(ENTER_TO_SENT, false)
    suspend fun setEnterToSend(enabled: Boolean) {
        context.dataStore.edit { it[ENTER_TO_SENT] = enabled }
    }
}
