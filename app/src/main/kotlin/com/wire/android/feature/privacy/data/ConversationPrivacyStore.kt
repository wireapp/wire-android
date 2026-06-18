/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.privacy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wire.android.appLogger
import com.wire.android.datastore.EncryptionManager
import com.wire.android.feature.privacy.model.ConversationPrivacySettings
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Per-user local store for conversation privacy settings.
 *
 * Design notes:
 * - One DataStore file per user ("conversation_privacy_<userId>"), mirroring [com.wire.android.datastore.UserDataStore].
 * - The whole settings map is kept under a SINGLE preference key as one JSON blob so the conversation
 *   list can observe every level in one cheap flow (every visible row needs its level to render).
 * - The blob is encrypted at rest with [EncryptionManager] so raw file access cannot enumerate which
 *   conversations are sensitive (metadata confidentiality; message content already lives in SQLCipher).
 * - Conversations absent from the map are implicitly [ConversationPrivacySettings.DEFAULT] (NORMAL), so
 *   existing conversations need no backfill and the map only ever stores non-default entries.
 * - NOT synchronized with the backend or other devices — this is intentional and required by spec.
 */
class ConversationPrivacyStore(private val context: Context, userId: UserId) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "${PREFERENCES_NAME}_$userId")

    private val json = Json { ignoreUnknownKeys = true }

    /** Observe all non-default privacy settings keyed by conversation id. */
    fun observeMap(): Flow<Map<ConversationId, ConversationPrivacySettings>> =
        context.dataStore.data
            .map { prefs -> prefs[PRIVACY_MAP]?.let(::decode) ?: emptyMap() }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()

    /** Observe a single conversation's settings, defaulting to NORMAL when not configured. */
    fun observe(id: ConversationId): Flow<ConversationPrivacySettings> =
        observeMap().map { it[id] ?: ConversationPrivacySettings.DEFAULT }.distinctUntilChanged()

    /** One-shot read of the full map (used by the notification pipeline, which cannot collect a flow). */
    suspend fun currentMap(): Map<ConversationId, ConversationPrivacySettings> = observeMap().first()

    /**
     * Atomically update one conversation's settings. Writing the default value removes the entry to
     * keep the map minimal.
     */
    suspend fun update(
        id: ConversationId,
        transform: (ConversationPrivacySettings) -> ConversationPrivacySettings,
    ) {
        context.dataStore.edit { prefs ->
            val current = prefs[PRIVACY_MAP]?.let(::decode) ?: emptyMap()
            val updated = current.toMutableMap().apply {
                val next = transform(this[id] ?: ConversationPrivacySettings.DEFAULT)
                if (next.isDefault) remove(id) else put(id, next)
            }
            prefs[PRIVACY_MAP] = encode(updated)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(PRIVACY_MAP) }
    }

    private fun encode(map: Map<ConversationId, ConversationPrivacySettings>): String {
        val raw: Map<String, ConversationPrivacySettings> = map.mapKeys { it.key.asKey() }
        return EncryptionManager.encrypt(KEY_ALIAS, json.encodeToString(raw))
    }

    @Suppress("TooGenericExceptionCaught")
    private fun decode(stored: String): Map<ConversationId, ConversationPrivacySettings> =
        try {
            val plain = EncryptionManager.decrypt(KEY_ALIAS, stored)
            json.decodeFromString<Map<String, ConversationPrivacySettings>>(plain)
                .mapKeys { it.key.asConversationId() }
        } catch (e: Exception) {
            // Corrupt/undecryptable blob (e.g. keystore reset): fail closed to "no overrides" rather than crash.
            appLogger.w("$TAG failed to decode privacy map, treating as empty", e)
            emptyMap()
        }

    companion object {
        private const val TAG = "ConversationPrivacyStore"
        private const val PREFERENCES_NAME = "conversation_privacy"
        private const val KEY_ALIAS = "conversation_privacy_v1"
        private val PRIVACY_MAP = stringPreferencesKey("privacy_map")

        private fun ConversationId.asKey(): String = "$value@$domain"

        private fun String.asConversationId(): ConversationId {
            val idx = lastIndexOf('@')
            return if (idx < 0) ConversationId(this, "") else ConversationId(substring(0, idx), substring(idx + 1))
        }
    }
}
