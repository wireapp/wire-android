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
package com.wire.android.feature.privacy.panic

import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.ApplicationScope
import com.wire.android.feature.privacy.model.PanicDuration
import com.wire.android.feature.privacy.model.PanicModeState
import com.wire.android.util.CurrentTimeProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Global, device-level Panic Mode switch.
 *
 * Activation is a single [StateFlow] flip: every consumer (effective-level resolver, notification
 * mapper, secure-window controller, auto-lock) subscribes to [state], so turning Panic Mode on
 * instantly escalates every sensitive conversation with no extra plumbing — that is the
 * "protect everything immediately" requirement realised as one emission.
 *
 * The expiry deadline is persisted (not a live countdown), so Panic Mode survives process death and
 * self-heals: a deadline already in the past on cold start resolves to [PanicModeState.Inactive].
 */
@SingleIn(AppScope::class)
class
PanicModeManager @Inject constructor(
    @ApplicationScope private val appCoroutineScope: CoroutineScope,
    private val globalDataStore: GlobalDataStore,
    private val currentTime: CurrentTimeProvider,
) {
    private val _state = MutableStateFlow<PanicModeState>(PanicModeState.Inactive)
    val state: StateFlow<PanicModeState> = _state.asStateFlow()

    private var expiryJob: Job? = null

    init {
        // Restore persisted state asynchronously (no runBlocking in init).
        appCoroutineScope.launch {
            val active = globalDataStore.isPanicModeActive().first()
            if (active) {
                val expiresAt = globalDataStore.panicModeExpiresAt().first()
                applyActivation(expiresAt, persist = false)
            }
        }
    }

    val isActive: Boolean get() = _state.value.isActive

    /** Turn Panic Mode on for the given [duration]. Idempotent — re-activating resets the timer. */
    suspend fun activate(duration: PanicDuration) {
        val expiresAt = duration.duration?.let { currentTime().toEpochMilliseconds() + it.inWholeMilliseconds }
        applyActivation(expiresAt, persist = true)
    }

    suspend fun deactivate() {
        expiryJob?.cancel()
        expiryJob = null
        _state.value = PanicModeState.Inactive
        globalDataStore.setPanicMode(active = false, expiresAtEpochMs = null)
        appLogger.i("$TAG deactivated")
    }

    private suspend fun applyActivation(expiresAtEpochMs: Long?, persist: Boolean) {
        expiryJob?.cancel()
        expiryJob = null

        val now = currentTime().toEpochMilliseconds()
        if (expiresAtEpochMs != null && expiresAtEpochMs <= now) {
            // Already expired (e.g. restored after the app was dead past the deadline).
            deactivate()
            return
        }

        if (persist) globalDataStore.setPanicMode(active = true, expiresAtEpochMs = expiresAtEpochMs)
        _state.value = PanicModeState.Active(expiresAtEpochMs)
        appLogger.i("$TAG activated, expiresAt=$expiresAtEpochMs")

        if (expiresAtEpochMs != null) {
            val remaining = expiresAtEpochMs - now
            expiryJob = appCoroutineScope.launch {
                delay(remaining)
                deactivate()
            }
        }
    }

    companion object {
        private const val TAG = "PanicModeManager"
    }
}
