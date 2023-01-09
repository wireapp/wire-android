package com.wire.android.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.wire.kalium.logic.data.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SyncStateObserver(private val stateFlow : StateFlow<SyncState?> = MutableStateFlow(null)) {
    val isSynced get() = stateFlow.value == SyncState.Live
}

val LocalSyncStateObserver = staticCompositionLocalOf { SyncStateObserver() }
