/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.FeatureFlagState
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.feature.user.ObserveFileSharingStatusUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.MarkGuestLinkFeatureFlagAsNotChangedUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureFlagNotificationViewModel @Inject constructor(
    private val observeSyncState: ObserveSyncStateUseCase,
    private val observeFileSharingStatus: ObserveFileSharingStatusUseCase,
    private val observeGuestRoomLinkFeatureFlag: ObserveGuestRoomLinkFeatureFlagUseCase,
    private val markGuestLinkFeatureFlagAsNotChanged: MarkGuestLinkFeatureFlagAsNotChangedUseCase
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
                setGuestRoomLinkFeatureFlag()
            }
        }
    }

    private fun setFileSharingState() {
        viewModelScope.launch {
            observeFileSharingStatus().collect {
                if (it.isFileSharingEnabled != null) {
                    featureFlagState = featureFlagState.copy(isFileSharingEnabledState = it.isFileSharingEnabled!!)
                }
                if (it.isStatusChanged != null && it.isStatusChanged!!) {
                    featureFlagState = featureFlagState.copy(showFileSharingDialog = it.isStatusChanged!!)
                }
            }
        }
    }

    private suspend fun setGuestRoomLinkFeatureFlag() {
        observeGuestRoomLinkFeatureFlag().collect { guestRoomLinkStatus ->
            guestRoomLinkStatus.isGuestRoomLinkEnabled?.let {
                featureFlagState = featureFlagState.copy(isGuestRoomLinkEnabled = it)
            }
            guestRoomLinkStatus.isStatusChanged?.let {
                featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = it)
            }
        }
    }

    fun dismissFileSharingDialog() {
        featureFlagState = featureFlagState.copy(showFileSharingDialog = false)
    }

    fun dismissGuestRoomLinkDialog() {
        markGuestLinkFeatureFlagAsNotChanged()
        featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = false)
    }
}
