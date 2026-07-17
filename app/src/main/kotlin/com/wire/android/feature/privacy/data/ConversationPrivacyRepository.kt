/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.android.feature.privacy.model.AutoLockTimeout
import com.wire.android.feature.privacy.model.ConversationPrivacyLevel
import com.wire.android.feature.privacy.model.ConversationPrivacySettings
import com.wire.android.feature.privacy.model.EffectivePrivacyLevel
import com.wire.android.feature.privacy.model.PrivacyResolver
import com.wire.android.feature.privacy.panic.PanicModeManager
import com.wire.kalium.logic.data.id.ConversationId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Single entry point for conversation privacy. Combines the per-conversation base level (local store)
 * with the global Panic Mode state into the [EffectivePrivacyLevel] that all consumers read.
 */
interface ConversationPrivacyRepository {

    /** All non-NORMAL conversation settings (NORMAL is implicit). Cheap; intended for the list. */
    fun observeAll(): Flow<Map<ConversationId, ConversationPrivacySettings>>

    /** Base settings for one conversation (defaults to NORMAL). */
    fun observe(id: ConversationId): Flow<ConversationPrivacySettings>

    /** Base level combined with live Panic Mode — the value consumers actually want. */
    fun observeEffective(id: ConversationId): Flow<EffectivePrivacyLevel>

    /** Live map of effective levels for all configured conversations (for the list). */
    fun observeEffectiveAll(): Flow<Map<ConversationId, EffectivePrivacyLevel>>

    /** One-shot effective level (for the notification pipeline). */
    suspend fun effectiveLevelOnce(id: ConversationId): EffectivePrivacyLevel

    /** Whether Panic Mode is currently active (global). */
    fun observePanicMode(): Flow<Boolean>

    suspend fun get(id: ConversationId): ConversationPrivacySettings

    suspend fun setLevel(id: ConversationId, level: ConversationPrivacyLevel)

    suspend fun setAutoLock(id: ConversationId, timeout: AutoLockTimeout)

    /** Add/remove the conversation from the global Panic list. */
    suspend fun setPanicProtected(id: ConversationId, protected: Boolean)
}

@Inject
class ConversationPrivacyRepositoryImpl(
    private val store: ConversationPrivacyStore,
    private val panicModeManager: PanicModeManager,
) : ConversationPrivacyRepository {

    override fun observeAll(): Flow<Map<ConversationId, ConversationPrivacySettings>> = store.observeMap()

    override fun observe(id: ConversationId): Flow<ConversationPrivacySettings> = store.observe(id)

    override fun observeEffective(id: ConversationId): Flow<EffectivePrivacyLevel> =
        combine(store.observe(id), panicModeManager.state) { settings, panic ->
            PrivacyResolver.resolve(settings.level, panic.isActive, settings.panicProtected)
        }.distinctUntilChanged()

    override fun observeEffectiveAll(): Flow<Map<ConversationId, EffectivePrivacyLevel>> =
        combine(store.observeMap(), panicModeManager.state) { map, panic ->
            map.mapValues { (_, settings) ->
                PrivacyResolver.resolve(settings.level, panic.isActive, settings.panicProtected)
            }
        }.distinctUntilChanged()

    override suspend fun effectiveLevelOnce(id: ConversationId): EffectivePrivacyLevel =
        get(id).let { PrivacyResolver.resolve(it.level, panicModeManager.isActive, it.panicProtected) }

    override fun observePanicMode(): Flow<Boolean> =
        panicModeManager.state.map { it.isActive }.distinctUntilChanged()

    override suspend fun get(id: ConversationId): ConversationPrivacySettings =
        store.currentMap()[id] ?: ConversationPrivacySettings.DEFAULT

    override suspend fun setLevel(id: ConversationId, level: ConversationPrivacyLevel) =
        store.update(id) { it.copy(level = level) }

    override suspend fun setAutoLock(id: ConversationId, timeout: AutoLockTimeout) =
        store.update(id) { it.copy(autoLock = timeout) }

    override suspend fun setPanicProtected(id: ConversationId, protected: Boolean) =
        store.update(id) { it.copy(panicProtected = protected) }
}
