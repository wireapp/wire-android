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

package com.wire.android.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.wire.kalium.logic.data.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SyncStateObserver(private val stateFlow: StateFlow<SyncState?> = MutableStateFlow(null)) {
    val isSyncing get() = stateFlow.value is SyncState.SlowSync || stateFlow.value is SyncState.GatheringPendingEvents
    val isConnecting get() = stateFlow.value is SyncState.Failed || stateFlow.value is SyncState.Waiting
}

val LocalSyncStateObserver = staticCompositionLocalOf { SyncStateObserver() }
