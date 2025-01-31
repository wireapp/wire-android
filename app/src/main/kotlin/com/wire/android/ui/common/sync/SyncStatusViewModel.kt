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
package com.wire.android.ui.common.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelScopedPreview
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@ViewModelScopedPreview
interface SyncStatusViewModel {
    val state: SyncStatusState
        get() = SyncStatusState.SlowSyncCompleted
}

@HiltViewModel
class SyncStatusViewModelImpl @Inject constructor(
    private val observeSyncStateUseCase: ObserveSyncStateUseCase
) : ViewModel(), SyncStatusViewModel {

    override var state: SyncStatusState by mutableStateOf(SyncStatusState.Pending)

    init {
        observeSyncState()
    }

    fun observeSyncState() {
        viewModelScope.launch {
            observeSyncStateUseCase()
                .collect { syncState ->
                    state = when (syncState) {
                        is SyncState.Failed -> SyncStatusState.Failed
                        SyncState.GatheringPendingEvents -> SyncStatusState.SlowSyncCompleted
                        SyncState.Live -> SyncStatusState.SlowSyncCompleted
                        SyncState.SlowSync -> SyncStatusState.Pending
                        SyncState.Waiting -> SyncStatusState.Pending
                    }
                }
        }
    }
}

enum class SyncStatusState {
    Failed,
    SlowSyncCompleted,
    Pending
}

@Serializable
object SyncStatusArgs : ScopedArgs {
    override val key = "SyncStatusArgsKey"
}
