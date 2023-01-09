package com.wire.android.ui.home.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.FeatureFlagState
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.user.ObserveFileSharingStatusUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureFlagNotificationViewModel @Inject constructor(
    private val observeSyncState: ObserveSyncStateUseCase,
    private val observeFileSharingStatusUseCase: ObserveFileSharingStatusUseCase
) : ViewModel() {

    var featureFlagState by mutableStateOf(FeatureFlagState())
        private set

    init {
        viewModelScope.launch {
            launch { loadSync() }
        }
    }

    private suspend fun loadSync() {
        observeSyncState().collect { newState ->
            if (newState == SyncState.Live) {
                setFileSharingState()
            }
        }
    }

    private fun setFileSharingState() {
        viewModelScope.launch {
            observeFileSharingStatusUseCase().collect {
                if (it.isFileSharingEnabled != null) {
                    featureFlagState = featureFlagState.copy(isFileSharingEnabledState = it.isFileSharingEnabled!!)
                }
                if (it.isStatusChanged != null && it.isStatusChanged!!) {
                    featureFlagState = featureFlagState.copy(showFileSharingDialog = it.isStatusChanged!!)
                }
            }
        }
    }

    fun hideDialogStatus() {
        featureFlagState = featureFlagState.copy(showFileSharingDialog = false)
    }
}
