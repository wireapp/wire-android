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
import com.google.android.gms.common.util.VisibleForTesting
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.home.FeatureFlagState
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureFlagNotificationViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val getSessions: GetSessionsUseCase,
    private val currentSessionUseCase: CurrentSessionUseCase
) : ViewModel() {

    var featureFlagState by mutableStateOf(FeatureFlagState())
        private set

    private var currentUserId by mutableStateOf<UserId?>(null)

    /**
     * The FeatureFlagNotificationViewModel is an attempt to encapsulate the logic regarding the different user feature flags, like for
     * example the file sharing one. This means that this VM could be invoked as an extension from outside the general app lifecycle (for
     * example when trying to share a file from an external app into Wire).
     *
     * This method is therefore called to check whether the user has a valid session or not. If the user does have a valid one, it observes
     * it until the sync state is live. Once the sync state is live, it sets whether the file sharing feature is enabled or not on the VM
     * state.
     */
    fun loadInitialSync() {
        viewModelScope.launch { initialSync() }
    }

    @VisibleForTesting
    suspend fun initialSync() {
        currentSessionUseCase().let { currentSessionResult ->
            when (currentSessionResult) {
                is CurrentSessionResult.Failure -> {
                    appLogger.e("Failure while getting current session from FeatureFlagNotificationViewModel")
                    println("testing by boris: currentSessionResult failure")
                    featureFlagState = featureFlagState.copy(fileSharingRestrictedState = FeatureFlagState.SharingRestrictedState.NO_USER)
                }

                is CurrentSessionResult.Success -> {
                    println("testing by boris: currentSessionResult success")
                    coreLogic.getSessionScope(currentSessionResult.accountInfo.userId).observeSyncState()
                        .firstOrNull { it == SyncState.Live }?.let {
                            println("testing by boris: observeSyncState")
                            currentUserId = currentSessionResult.accountInfo.userId
                            setFileSharingState(currentSessionResult.accountInfo.userId)
                            setGuestRoomLinkFeatureFlag(currentSessionResult.accountInfo.userId)
                        }
                }
            }
        }
    }

    private fun setFileSharingState(userId: UserId) {
        viewModelScope.launch {
            coreLogic.getSessionScope(userId).observeFileSharingStatus().collect { fileSharingStatus ->
                println("testing by boris: VM isFileSharingEnabled: ${fileSharingStatus.isFileSharingEnabled}")
                fileSharingStatus.isFileSharingEnabled?.let {
                    val fileSharingRestrictedState = if (it) FeatureFlagState.SharingRestrictedState.NONE
                    else FeatureFlagState.SharingRestrictedState.RESTRICTED_IN_TEAM

                    featureFlagState = featureFlagState.copy(
                        fileSharingRestrictedState = fileSharingRestrictedState,
                        isFileSharingEnabledState = it
                    )
                }
                fileSharingStatus.isStatusChanged?.let {
                    featureFlagState = featureFlagState.copy(showFileSharingDialog = it)
                }
                println("testing by boris: observeFileSharingStatus finished")
            }
        }
    }

    private fun setGuestRoomLinkFeatureFlag(userId: UserId) {
        viewModelScope.launch {
            coreLogic.getSessionScope(userId).observeGuestRoomLinkFeatureFlag().collect { guestRoomLinkStatus ->
                guestRoomLinkStatus.isGuestRoomLinkEnabled?.let {
                    featureFlagState = featureFlagState.copy(isGuestRoomLinkEnabled = it)
                }
                guestRoomLinkStatus.isStatusChanged?.let {
                    featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = it)
                }
            }
        }
    }

    fun dismissFileSharingDialog() {
        featureFlagState = featureFlagState.copy(showFileSharingDialog = false)
        viewModelScope.launch {
            currentUserId?.let { coreLogic.getSessionScope(it).markFileSharingStatusAsNotified() }
        }
    }

    private suspend fun checkNumberOfSessions(): Int {
        getSessions().let {
            return when (it) {
                is GetAllSessionsResult.Success -> {
                    it.sessions.filterIsInstance<AccountInfo.Valid>().size
                }

                is GetAllSessionsResult.Failure.Generic -> 0
                GetAllSessionsResult.Failure.NoSessionFound -> 0
            }
        }
    }

    fun dismissGuestRoomLinkDialog() {
        viewModelScope.launch {
            currentUserId?.let { coreLogic.getSessionScope(it).markGuestLinkFeatureFlagAsNotChanged() }
        }
        featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = false)
    }
}
