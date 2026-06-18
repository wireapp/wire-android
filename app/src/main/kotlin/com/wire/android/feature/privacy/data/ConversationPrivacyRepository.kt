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

    fun observeAll(): Flow<Map<ConversationId, ConversationPrivacySettings>>

    fun observe(id: ConversationId): Flow<ConversationPrivacySettings>

    fun observeEffective(id: ConversationId): Flow<EffectivePrivacyLevel>

    fun observeEffectiveAll(): Flow<Map<ConversationId, EffectivePrivacyLevel>>

    suspend fun effectiveLevelOnce(id: ConversationId): EffectivePrivacyLevel

    fun observePanicMode(): Flow<Boolean>

    suspend fun get(id: ConversationId): ConversationPrivacySettings

    suspend fun setLevel(id: ConversationId, level: ConversationPrivacyLevel)

    suspend fun setAutoLock(id: ConversationId, timeout: AutoLockTimeout)

    suspend fun setPanicProtected(id: ConversationId, protected: Boolean)
}

@Inject
class ConversationPrivacyRepositoryImpl(
    private val conversationPrivacyStore: ConversationPrivacyStore,
    private val panicModeManager: PanicModeManager,
) : ConversationPrivacyRepository {

    override fun observeAll(): Flow<Map<ConversationId, ConversationPrivacySettings>> = conversationPrivacyStore.observeMap()

    override fun observe(id: ConversationId): Flow<ConversationPrivacySettings> = conversationPrivacyStore.observe(id)

    override fun observeEffective(id: ConversationId): Flow<EffectivePrivacyLevel> =
        combine(conversationPrivacyStore.observe(id), panicModeManager.state) { settings, panic ->
            PrivacyResolver.resolve(settings.level, panic.isActive, settings.panicProtected)
        }.distinctUntilChanged()

    override fun observeEffectiveAll(): Flow<Map<ConversationId, EffectivePrivacyLevel>> =
        combine(conversationPrivacyStore.observeMap(), panicModeManager.state) { map, panic ->
            map.mapValues { (_, settings) ->
                PrivacyResolver.resolve(settings.level, panic.isActive, settings.panicProtected)
            }
        }.distinctUntilChanged()

    override suspend fun effectiveLevelOnce(id: ConversationId): EffectivePrivacyLevel =
        get(id).let { PrivacyResolver.resolve(it.level, panicModeManager.isActive, it.panicProtected) }

    override fun observePanicMode(): Flow<Boolean> =
        panicModeManager.state.map { it.isActive }.distinctUntilChanged()

    override suspend fun get(id: ConversationId): ConversationPrivacySettings =
        conversationPrivacyStore.currentMap()[id] ?: ConversationPrivacySettings.DEFAULT

    override suspend fun setLevel(id: ConversationId, level: ConversationPrivacyLevel) =
        conversationPrivacyStore.update(id) { it.copy(level = level) }

    override suspend fun setAutoLock(id: ConversationId, timeout: AutoLockTimeout) =
        conversationPrivacyStore.update(id) { it.copy(autoLock = timeout) }

    override suspend fun setPanicProtected(id: ConversationId, protected: Boolean) =
        conversationPrivacyStore.update(id) { it.copy(panicProtected = protected) }
}
