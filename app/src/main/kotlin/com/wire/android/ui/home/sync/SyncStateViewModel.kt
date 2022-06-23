package com.wire.android.ui.home.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncStateViewModel @Inject constructor(
    private val observeSyncState: ObserveSyncStateUseCase
) : ViewModel() {

    var syncState by mutableStateOf(SyncViewState.WAITING)

    init {
        viewModelScope.launch {
            launch { loadSync() }
        }
    }

    private suspend fun loadSync() {
        observeSyncState().collect { newState ->
            syncState = when (newState) {
                is SyncState.Failed -> {
                    if (newState.cause is NetworkFailure.NoNetworkConnection) {
                        SyncViewState.LACK_OF_CONNECTION
                    } else {
                        SyncViewState.UNKNOWN_FAILURE
                    }
                }
                SyncState.GatheringPendingEvents -> SyncViewState.GATHERING_EVENTS
                SyncState.Live -> SyncViewState.LIVE
                SyncState.SlowSync -> SyncViewState.SLOW_SYNC
                SyncState.Waiting -> SyncViewState.WAITING
            }
        }
    }
}
