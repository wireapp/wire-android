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

package com.wire.android.ui.home.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.AppLockSource
import com.wire.android.feature.DisableAppLockUseCase
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.ui.home.toFeatureFlagState
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.message.TeamSelfDeleteTimer
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.e2ei.usecase.FinalizeEnrollmentResult
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.E2EIRequiredResult
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class FeatureFlagNotificationViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
    private val globalDataStore: Lazy<GlobalDataStore>,
    private val disableAppLockUseCase: Lazy<DisableAppLockUseCase>,
) : ViewModel() {

    var featureFlagState by mutableStateOf(FeatureFlagState())
        private set

    private var currentUserId by mutableStateOf<UserId?>(null)

    init {
        viewModelScope.launch {
            // Load local initial app lock state before collecting updates
            val initialAppLockSet = globalDataStore.get().isAppLockPasscodeSetFlow().first()
            featureFlagState = featureFlagState.copy(isUserAppLockSet = initialAppLockSet)

            coroutineScope {
                launch { initialSync() }
                launch { isAppLockSet() }
            }
        }
    }

    /**
     * The FeatureFlagNotificationViewModel is an attempt to encapsulate the logic regarding the different user feature flags, like for
     * example the file sharing one. This means that this VM could be invoked as an extension from outside the general app lifecycle (for
     * example when trying to share a file from an external app into Wire).
     *
     * This method is therefore called to check whether the user has a valid session or not. If the user does have a valid one, it observes
     * it until the sync state is live. Once the sync state is live, it sets whether the file sharing feature is enabled or not on the VM
     * state.
     */
    private suspend fun initialSync() {
        currentSessionFlow.get().invoke()
            .distinctUntilChanged()
            .collectLatest { currentSessionResult ->
                when {
                    currentSessionResult is CurrentSessionResult.Failure -> {
                        currentUserId = null
                        appLogger.i("$TAG: Failure while getting current session")
                        featureFlagState = FeatureFlagState( // no session, clear feature flag state to default and set NO_USER
                            isFileSharingState = FeatureFlagState.FileSharingState.NoUser
                        )
                    }

                    currentSessionResult is CurrentSessionResult.Success && !currentSessionResult.accountInfo.isValid() -> {
                        appLogger.i("$TAG: Invalid current session")
                        featureFlagState = FeatureFlagState( // invalid session, clear feature flag state to default and set NO_USER
                            isFileSharingState = FeatureFlagState.FileSharingState.NoUser
                        )
                    }

                    currentSessionResult is CurrentSessionResult.Success && currentSessionResult.accountInfo.isValid() -> {
                        featureFlagState = FeatureFlagState() // new session, clear feature flag state to default and wait until synced
                        currentSessionResult.accountInfo.userId.let { userId ->
                            currentUserId = userId
                            coreLogic.get().getSessionScope(userId).observeSyncState()
                                .firstOrNull { it == SyncState.Live }?.let {
                                    observeStatesAfterInitialSync(userId)
                                }
                        }
                    }
                }
            }
    }

    private suspend fun observeStatesAfterInitialSync(userId: UserId) {
        coroutineScope {
            launch { setFileSharingState(userId) }
            launch { observeTeamSettingsSelfDeletionStatus(userId) }
            launch { setGuestRoomLinkFeatureFlag(userId) }
            launch { setE2EIRequiredState(userId) }
            launch { setTeamAppLockFeatureFlag(userId) }
            launch { observeCallEndedBecauseOfConversationDegraded(userId) }
            launch { observeShouldNotifyForRevokedCertificate(userId) }
        }
    }

    private suspend fun observeShouldNotifyForRevokedCertificate(userId: UserId) {
        coreLogic.get().getSessionScope(userId).observeShouldNotifyForRevokedCertificate().collect {
            featureFlagState = featureFlagState.copy(shouldShowE2eiCertificateRevokedDialog = it)
        }
    }

    private suspend fun setFileSharingState(userId: UserId) {
        coreLogic.get().getSessionScope(userId).observeFileSharingStatus().collect { fileSharingStatus ->
            val state: FeatureFlagState.FileSharingState = fileSharingStatus.state.toFeatureFlagState()
            featureFlagState = featureFlagState.copy(
                isFileSharingState = state,
                showFileSharingDialog = fileSharingStatus.isStatusChanged ?: false
            )
        }
    }

    private suspend fun setGuestRoomLinkFeatureFlag(userId: UserId) {
        coreLogic.get().getSessionScope(userId).observeGuestRoomLinkFeatureFlag()
            .collect { guestRoomLinkStatus ->
                guestRoomLinkStatus.isGuestRoomLinkEnabled?.let {
                    featureFlagState = featureFlagState.copy(isGuestRoomLinkEnabled = it)
                }
                guestRoomLinkStatus.isStatusChanged?.let {
                    featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = it)
                }
            }
    }

    private suspend fun setTeamAppLockFeatureFlag(userId: UserId) {
        coreLogic.get().getSessionScope(userId).appLockTeamFeatureConfigObserver()
            .distinctUntilChanged()
            .collectLatest { appLockConfig ->
                appLockConfig?.isStatusChanged?.let { isStatusChanged ->
                    val shouldBlockApp = if (isStatusChanged) {
                        true
                    } else {
                        (!featureFlagState.isUserAppLockSet && appLockConfig.isEnforced)
                    }

                    featureFlagState = featureFlagState.copy(
                        isTeamAppLockEnabled = appLockConfig.isEnforced,
                        shouldShowTeamAppLockDialog = shouldBlockApp
                    )
                }
            }
    }

    private suspend fun observeTeamSettingsSelfDeletionStatus(userId: UserId) {
        coreLogic.get().getSessionScope(userId).observeTeamSettingsSelfDeletionStatus()
            .collect { teamSettingsSelfDeletingStatus ->
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

    private suspend fun setE2EIRequiredState(userId: UserId) {
        coreLogic.get().getSessionScope(userId).observeE2EIRequired().collect { result ->
            val state = when (result) {
                E2EIRequiredResult.NoGracePeriod.Create -> FeatureFlagState.E2EIRequired.NoGracePeriod.Create
                E2EIRequiredResult.NoGracePeriod.Renew -> FeatureFlagState.E2EIRequired.NoGracePeriod.Renew
                is E2EIRequiredResult.WithGracePeriod.Create -> FeatureFlagState.E2EIRequired.WithGracePeriod.Create(
                    result.timeLeft
                )

                is E2EIRequiredResult.WithGracePeriod.Renew -> FeatureFlagState.E2EIRequired.WithGracePeriod.Renew(
                    result.timeLeft
                )

                E2EIRequiredResult.NotRequired -> null
            }
            featureFlagState = featureFlagState.copy(e2EIRequired = state)
        }
    }

    private suspend fun observeCallEndedBecauseOfConversationDegraded(userId: UserId) =
        coreLogic.get().getSessionScope(userId).calls.observeEndCallDueToDegradationDialog().collect {
            featureFlagState = featureFlagState.copy(showCallEndedBecauseOfConversationDegraded = true)
        }

    fun dismissSelfDeletingMessagesDialog() {
        featureFlagState = featureFlagState.copy(shouldShowSelfDeletingMessagesDialog = false)
        viewModelScope.launch {
            currentUserId?.let {
                coreLogic.get().getSessionScope(it).markSelfDeletingMessagesAsNotified()
            }
        }
    }

    fun dismissE2EICertificateRevokedDialog() {
        featureFlagState = featureFlagState.copy(shouldShowE2eiCertificateRevokedDialog = false)
        currentUserId?.let {
            viewModelScope.launch {
                coreLogic.get().getSessionScope(it).markNotifyForRevokedCertificateAsNotified()
            }
        }
    }

    fun dismissFileSharingDialog() {
        featureFlagState = featureFlagState.copy(showFileSharingDialog = false)
        viewModelScope.launch {
            currentUserId?.let { coreLogic.get().getSessionScope(it).markFileSharingStatusAsNotified() }
        }
    }

    fun dismissGuestRoomLinkDialog() {
        viewModelScope.launch {
            currentUserId?.let {
                coreLogic.get().getSessionScope(it).markGuestLinkFeatureFlagAsNotChanged()
            }
        }
        featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = false)
    }

    fun dismissTeamAppLockDialog() {
        featureFlagState = featureFlagState.copy(shouldShowTeamAppLockDialog = false)
    }

    fun markTeamAppLockStatusAsNot() {
        viewModelScope.launch {
            currentUserId?.let {
                coreLogic.get().getSessionScope(it).markTeamAppLockStatusAsNotified()
            }
        }
    }

    fun confirmAppLockNotEnforced() {
        viewModelScope.launch {
            when (globalDataStore.get().getAppLockSource()) {
                AppLockSource.Manual -> {}

                AppLockSource.TeamEnforced -> disableAppLockUseCase.get().invoke()
            }
        }
    }

    private suspend fun isAppLockSet() = globalDataStore.get().isAppLockPasscodeSetFlow().collect { isSet ->
        featureFlagState = featureFlagState.copy(isUserAppLockSet = isSet)
    }

    fun enrollE2EICertificate() {
        featureFlagState = featureFlagState.copy(isE2EILoading = true, startGettingE2EICertificate = true)
    }

    fun handleE2EIEnrollmentResult(result: FinalizeEnrollmentResult) {
        val e2eiRequired = featureFlagState.e2EIRequired
        featureFlagState = when (result) {
            is FinalizeEnrollmentResult.Failure -> {
                featureFlagState.copy(
                    isE2EILoading = false,
                    startGettingE2EICertificate = false,
                    e2EIRequired = null,
                    e2EIResult = e2eiRequired?.let { FeatureFlagState.E2EIResult.Failure(e2eiRequired) }
                )
            }

            is FinalizeEnrollmentResult.Success -> {
                featureFlagState.copy(
                    isE2EILoading = false,
                    e2EIRequired = null,
                    startGettingE2EICertificate = false,
                    e2EIResult = FeatureFlagState.E2EIResult.Success(result.certificate)
                )
            }
        }
    }

    fun snoozeE2EIdRequiredDialog(result: FeatureFlagState.E2EIRequired.WithGracePeriod) {
        featureFlagState = featureFlagState.copy(
            e2EIRequired = null,
            e2EIResult = null,
            e2EISnoozeInfo = FeatureFlagState.E2EISnooze(result.timeLeft)
        )
        currentUserId?.let { userId ->
            viewModelScope.launch {
                coreLogic.get().getSessionScope(userId).markE2EIRequiredAsNotified(result.timeLeft)
            }
        }
    }

    fun dismissSnoozeE2EIdRequiredDialog() {
        featureFlagState = featureFlagState.copy(e2EISnoozeInfo = null)
    }

    fun dismissCallEndedBecauseOfConversationDegraded() {
        featureFlagState = featureFlagState.copy(showCallEndedBecauseOfConversationDegraded = false)
    }

    fun dismissSuccessE2EIdDialog() {
        featureFlagState = featureFlagState.copy(e2EIResult = null)
    }

    companion object {
        private const val TAG = "FeatureFlagNotificationViewModel"
    }
}
