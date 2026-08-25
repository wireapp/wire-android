/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.initialsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/** Feature-owned initial-sync state machine. A completed sync is a terminal event per attempt. */
class InitialSyncViewModel(
    private val gateway: InitialSyncGateway,
) : ViewModel() {
    var state: InitialSyncState by mutableStateOf(InitialSyncState.Loading)
        private set

    init {
        load()
    }

    fun retry() {
        if (state is InitialSyncState.Loading) return
        state = InitialSyncState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            state = when (val result = gateway.awaitInitialSync()) {
                is InitialSyncGatewayResult.Completed -> InitialSyncState.Completed(result.shouldMoveToBackground)
                InitialSyncGatewayResult.Unavailable -> InitialSyncState.Unavailable
            }
        }
    }
}
