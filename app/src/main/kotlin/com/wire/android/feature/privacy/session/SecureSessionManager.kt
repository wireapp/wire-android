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
package com.wire.android.feature.privacy.session

import com.wire.android.feature.privacy.data.ConversationPrivacyRepository
import com.wire.android.feature.privacy.model.EffectivePrivacyLevel
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ScreenStateObserver
import com.wire.kalium.logic.data.id.ConversationId
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Drives the conversation screen's [ConversationAccessState] by combining the conversation's
 * effective privacy level with global signals (app visibility, screen on/off) and per-conversation
 * runtime state (authenticated, inactivity). Implements the state machine described in the design.
 *
 * Created per conversation screen (unscoped); the global signals it reads are app-scoped singletons.
 */
@Inject
class SecureSessionManager(
    private val privacyRepository: ConversationPrivacyRepository,
    private val cleaner: SecureSessionCleaner,
    currentScreenManager: CurrentScreenManager,
    screenStateObserver: ScreenStateObserver,
) {
    private val appVisible = currentScreenManager.isAppVisibleFlow()
    private val screenOn = screenStateObserver.screenStateFlow

    /** Conversations the user has authenticated this session. */
    private val unlocked = MutableStateFlow<Set<ConversationId>>(emptySet())

    /** Emits on user interaction to reset the inactivity timer. */
    private val activityPing = MutableSharedFlow<ConversationId>(extraBufferCapacity = 16)

    fun observeAccessState(id: ConversationId): Flow<ConversationAccessState> =
        combine(
            privacyRepository.observeEffective(id),
            privacyRepository.observe(id).map { it.autoLock.delay }.distinctUntilChanged(),
            unlocked.map { id in it }.distinctUntilChanged(),
            appVisible,
            screenOn,
        ) { effective, autoLock, isUnlocked, visible, screen ->
            Inputs(effective, autoLock, isUnlocked, visible && screen)
        }
            .flatMapLatest { inputs -> resolve(id, inputs) }
            .distinctUntilChanged()

    /** Record that the user successfully authenticated this conversation. */
    fun markUnlocked(id: ConversationId) = unlocked.update { it + id }

    /** Lock immediately (e.g. a manual lock button). */
    fun manualLock(id: ConversationId) = lock(id)

    /** Signal user interaction so the inactivity timer restarts. */
    fun userActivity(id: ConversationId) {
        activityPing.tryEmit(id)
    }

    private fun resolve(id: ConversationId, inputs: Inputs): Flow<ConversationAccessState> = flow {
        val eff = inputs.effective
        when {
            eff == EffectivePrivacyLevel.NORMAL -> emit(ConversationAccessState.Visible)

            !eff.requiresAuth -> {
                // SENSITIVE family: readable while actively looking, blurred immediately on focus loss
                // (background / screen-off) or after the inactivity timeout. Never authenticates.
                if (!inputs.inFocus) {
                    emit(ConversationAccessState.Concealed)
                    return@flow
                }
                emit(ConversationAccessState.Visible)
                if (inputs.autoLock > Duration.ZERO) {
                    awaitInactivity(id, inputs.autoLock)
                    emit(ConversationAccessState.Concealed)
                }
            }

            else -> {
                // HIGHLY_SENSITIVE family: withhold content until authenticated; auto-lock + purge.
                val forceLock = eff == EffectivePrivacyLevel.HIGHLY_SENSITIVE_PANIC
                if (forceLock || !inputs.isUnlocked) {
                    lock(id)
                    emit(ConversationAccessState.Locked)
                    return@flow
                }
                if (!inputs.inFocus) {
                    // Out of focus: lock after the auto-lock grace period (IMMEDIATELY == now).
                    if (inputs.autoLock <= Duration.ZERO) {
                        lock(id)
                        emit(ConversationAccessState.Locked)
                        return@flow
                    }
                    emit(ConversationAccessState.Visible)
                    val survived = withTimeoutOrNull(inputs.autoLock.inWholeMilliseconds) { neverCompletes() }
                    if (survived == null) {
                        lock(id)
                        emit(ConversationAccessState.Locked)
                    }
                    return@flow
                }
                emit(ConversationAccessState.Visible)
                if (inputs.autoLock > Duration.ZERO) {
                    awaitInactivity(id, inputs.autoLock)
                    lock(id)
                    emit(ConversationAccessState.Locked)
                }
            }
        }
    }

    /** Suspends until [timeout] elapses with no activity ping for [id] (each ping restarts the wait). */
    private suspend fun awaitInactivity(id: ConversationId, timeout: Duration) {
        while (true) {
            val pinged = withTimeoutOrNull(timeout.inWholeMilliseconds) {
                activityPing.first { it == id }
            }
            if (pinged == null) return // timed out
        }
    }

    private suspend fun neverCompletes(): Nothing {
        kotlinx.coroutines.awaitCancellation()
    }

    private fun lock(id: ConversationId) {
        unlocked.update { it - id }
        cleaner.purge(id)
    }

    private data class Inputs(
        val effective: EffectivePrivacyLevel,
        val autoLock: Duration,
        val isUnlocked: Boolean,
        val inFocus: Boolean,
    )
}
