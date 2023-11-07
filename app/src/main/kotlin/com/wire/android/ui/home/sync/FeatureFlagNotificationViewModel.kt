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
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.message.TeamSelfDeleteTimer
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.E2EIRequiredResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureFlagNotificationViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
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

    suspend fun initialSync() {
        currentSessionUseCase().let { currentSessionResult ->
            when (currentSessionResult) {
                is CurrentSessionResult.Failure -> {
                    appLogger.e("Failure while getting current session from FeatureFlagNotificationViewModel")
                    featureFlagState = featureFlagState.copy(fileSharingRestrictedState = FeatureFlagState.SharingRestrictedState.NO_USER)
                }

                is CurrentSessionResult.Success -> {
                    val userId = currentSessionResult.accountInfo.userId
                    coreLogic.getSessionScope(userId).observeSyncState()
                        .firstOrNull { it == SyncState.Live }?.let {
                            currentUserId = userId
                            setFileSharingState(userId)
                            observeTeamSettingsSelfDeletionStatus(userId)
                            setGuestRoomLinkFeatureFlag(userId)
                            setE2EIRequiredState(userId)
                            observeCallEndedBecauseOfConversationDegraded(userId)
                        }
                }
            }
        }
    }

    private fun setFileSharingState(userId: UserId) = viewModelScope.launch {
        coreLogic.getSessionScope(userId).observeFileSharingStatus().collect { fileSharingStatus ->
            fileSharingStatus.state?.let {
                // TODO: handle restriction when sending assets
                val (fileSharingRestrictedState, state) = if (it is FileSharingStatus.Value.EnabledAll) {
                    FeatureFlagState.SharingRestrictedState.NONE to true
                } else {
                    FeatureFlagState.SharingRestrictedState.RESTRICTED_IN_TEAM to false
                }

                featureFlagState = featureFlagState.copy(
                    fileSharingRestrictedState = fileSharingRestrictedState,
                    isFileSharingEnabledState = state
                )
            }
            fileSharingStatus.isStatusChanged?.let {
                featureFlagState = featureFlagState.copy(showFileSharingDialog = it)
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

    private fun observeTeamSettingsSelfDeletionStatus(userId: UserId) {
        viewModelScope.launch {
            coreLogic.getSessionScope(userId).observeTeamSettingsSelfDeletionStatus().collect { teamSettingsSelfDeletingStatus ->
                val areSelfDeletedMessagesEnabled =
                    teamSettingsSelfDeletingStatus.enforcedSelfDeletionTimer !is TeamSelfDeleteTimer.Disabled
                val shouldShowSelfDeletingMessagesDialog =
                    teamSettingsSelfDeletingStatus.hasFeatureChanged ?: false
                val enforcedTimeoutDuration: SelfDeletionDuration =
                    with(teamSettingsSelfDeletingStatus.enforcedSelfDeletionTimer) {
                        when (this) {
                            TeamSelfDeleteTimer.Disabled,
                            TeamSelfDeleteTimer.Enabled -> SelfDeletionDuration.None

                            is TeamSelfDeleteTimer.Enforced -> this.enforcedDuration.toSelfDeletionDuration()
                        }
                    }
                featureFlagState = featureFlagState.copy(
                    areSelfDeletedMessagesEnabled = areSelfDeletedMessagesEnabled,
                    shouldShowSelfDeletingMessagesDialog = shouldShowSelfDeletingMessagesDialog,
                    enforcedTimeoutDuration = enforcedTimeoutDuration
                )
            }
        }
    }

    private fun setE2EIRequiredState(userId: UserId) = viewModelScope.launch {
        coreLogic.getSessionScope(userId).observeE2EIRequired().collect { result ->
            val state = when (result) {
                E2EIRequiredResult.NoGracePeriod.Create -> FeatureFlagState.E2EIRequired.NoGracePeriod.Create
                E2EIRequiredResult.NoGracePeriod.Renew -> FeatureFlagState.E2EIRequired.NoGracePeriod.Renew
                is E2EIRequiredResult.WithGracePeriod.Create -> FeatureFlagState.E2EIRequired.WithGracePeriod.Create(result.timeLeft)
                is E2EIRequiredResult.WithGracePeriod.Renew -> FeatureFlagState.E2EIRequired.WithGracePeriod.Renew(result.timeLeft)
                E2EIRequiredResult.NotRequired -> null
            }
            featureFlagState = featureFlagState.copy(e2EIRequired = state)
        }
    }

    private fun observeCallEndedBecauseOfConversationDegraded(userId: UserId) = viewModelScope.launch {
        coreLogic.getSessionScope(userId).calls.observeEndCallDialog().collect {
            featureFlagState = featureFlagState.copy(showCallEndedBecauseOfConversationDegraded = true)
        }
    }

    fun dismissSelfDeletingMessagesDialog() {
        featureFlagState = featureFlagState.copy(shouldShowSelfDeletingMessagesDialog = false)
        viewModelScope.launch {
            currentUserId?.let { coreLogic.getSessionScope(it).markSelfDeletingMessagesAsNotified() }
        }
    }

    fun dismissFileSharingDialog() {
        featureFlagState = featureFlagState.copy(showFileSharingDialog = false)
        viewModelScope.launch {
            currentUserId?.let { coreLogic.getSessionScope(it).markFileSharingStatusAsNotified() }
        }
    }

    fun dismissGuestRoomLinkDialog() {
        viewModelScope.launch {
            currentUserId?.let { coreLogic.getSessionScope(it).markGuestLinkFeatureFlagAsNotChanged() }
        }
        featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = false)
    }

    fun getE2EICertificate() {
        // TODO do the magic
        featureFlagState = featureFlagState.copy(e2EIRequired = null)
    }

    fun snoozeE2EIdRequiredDialog(result: FeatureFlagState.E2EIRequired.WithGracePeriod) {
        featureFlagState = featureFlagState.copy(
            e2EIRequired = null,
            e2EISnoozeInfo = FeatureFlagState.E2EISnooze(result.timeLeft)
        )
        currentUserId?.let { userId ->
            viewModelScope.launch {
                coreLogic.getSessionScope(userId).markE2EIRequiredAsNotified(result.timeLeft)
            }
        }
    }

    fun dismissSnoozeE2EIdRequiredDialog() {
        featureFlagState = featureFlagState.copy(e2EISnoozeInfo = null)
    }

    fun dismissCallEndedBecauseOfConversationDegraded() {
        featureFlagState = featureFlagState.copy(showCallEndedBecauseOfConversationDegraded = false)
    }
}
