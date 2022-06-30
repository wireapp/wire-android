package com.wire.android.ui.home.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.HomeState
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.user.FileSharingStatusFlowUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncStateViewModel @Inject constructor(
    private val observeSyncState: ObserveSyncStateUseCase,
    private val fileSharingStatusFlowUseCase: FileSharingStatusFlowUseCase
) : ViewModel() {

    var syncState by mutableStateOf(SyncViewState.WAITING)
    var homeState by mutableStateOf(HomeState())
        private set

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
                SyncState.Live -> {
                    SyncViewState.LIVE.also { setFileSharingState() }
                }

                SyncState.SlowSync -> SyncViewState.SLOW_SYNC
                SyncState.Waiting -> SyncViewState.WAITING
            }
        }
    }

    private fun setFileSharingState() {
        viewModelScope.launch {
            fileSharingStatusFlowUseCase().collect {
                if (it.isFileSharingEnabled != null) {
                    homeState = homeState.copy(isFileSharingEnabledState = it.isFileSharingEnabled!!)
                }
                if (it.isStatusChanged != null && it.isStatusChanged!!) {
                    homeState = homeState.copy(showFileSharingDialog = it.isStatusChanged!!)
                }
            }
        }
    }

    fun hideDialogStatus() {
        homeState = homeState.copy(showFileSharingDialog = false)
    }
}
