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

package com.wire.android.ui.initialsync

import androidx.compose.animation.core.AnimationConstants.DefaultDurationMillis
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class InitialSyncViewModel @Inject constructor(
    private val observeSyncState: ObserveSyncStateUseCase,
    private val userDataStoreProvider: UserDataStoreProvider,
    @CurrentAccount private val userId: UserId,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    internal var isSyncCompleted: Boolean by mutableStateOf(false)
        private set

    init {
        waitUntilSyncIsCompleted()
    }

    private fun waitUntilSyncIsCompleted() =
        viewModelScope.launch(dispatchers.io()) {
            delay(DefaultDurationMillis.toLong()) // it can be triggered instantly so it's added to keep smooth transitions
            withContext(dispatchers.io()) {
                observeSyncState().firstOrNull { it is SyncState.Live }?.let {
                    userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted()
                }
            }?.let {
                isSyncCompleted = true
            } ?: run {
                appLogger.e("InitialSyncViewModel: SyncState is null")
            }
        }
}
