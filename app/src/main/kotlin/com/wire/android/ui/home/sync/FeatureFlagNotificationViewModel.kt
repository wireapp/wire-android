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

import android.content.Context
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
import com.wire.android.feature.e2ei.GetE2EICertificateUseCase
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.message.TeamSelfDeleteTimer
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.E2EIRequiredResult
import com.wire.kalium.logic.functional.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class FeatureFlagNotificationViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentSessionFlow: CurrentSessionFlowUseCase,
    private val globalDataStore: GlobalDataStore,
    private val disableAppLockUseCase: DisableAppLockUseCase,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    var featureFlagState by mutableStateOf(FeatureFlagState())
        private set

    private var currentUserId by mutableStateOf<UserId?>(null)

    init {
        viewModelScope.launch { initialSync() }
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
        currentSessionFlow()
            .distinctUntilChanged()
            .collectLatest { currentSessionResult ->
                when {
                    currentSessionResult is CurrentSessionResult.Failure -> {
                        currentUserId = null
                        appLogger.i("$TAG: Failure while getting current session")
                        featureFlagState = FeatureFlagState( // no session, clear feature flag state to default and set NO_USER
                            fileSharingRestrictedState = FeatureFlagState.SharingRestrictedState.NO_USER
                        )
                    }
                    currentSessionResult is CurrentSessionResult.Success && !currentSessionResult.accountInfo.isValid() -> {
                        appLogger.i("$TAG: Invalid current session")
                        featureFlagState = FeatureFlagState( // invalid session, clear feature flag state to default and set NO_USER
                            fileSharingRestrictedState = FeatureFlagState.SharingRestrictedState.NO_USER
                        )
                    }
                    currentSessionResult is CurrentSessionResult.Success && currentSessionResult.accountInfo.isValid() -> {
                        featureFlagState = FeatureFlagState() // new session, clear feature flag state to default and wait until synced
                        currentSessionResult.accountInfo.userId.let { userId ->
                            currentUserId = userId
                            coreLogic.getSessionScope(userId).observeSyncState()
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
        }
    }

    private suspend fun setFileSharingState(userId: UserId) {
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

    private suspend fun setGuestRoomLinkFeatureFlag(userId: UserId) {
            coreLogic.getSessionScope(userId).observeGuestRoomLinkFeatureFlag()
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
            coreLogic.getSessionScope(userId).appLockTeamFeatureConfigObserver()
                .distinctUntilChanged()
                .collectLatest { appLockConfig ->
                    appLockConfig?.isStatusChanged?.let { isStatusChanged ->
                        val shouldBlockApp = if (isStatusChanged) {
                            true
                        } else {
                            (!isUserAppLockSet() && appLockConfig.isEnforced)
                        }

                        featureFlagState = featureFlagState.copy(
                            isTeamAppLockEnabled = appLockConfig.isEnforced,
                            shouldShowTeamAppLockDialog = shouldBlockApp
                        )
                    }
                }
        }

    private suspend fun observeTeamSettingsSelfDeletionStatus(userId: UserId) {
        coreLogic.getSessionScope(userId).observeTeamSettingsSelfDeletionStatus()
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
        coreLogic.getSessionScope(userId).observeE2EIRequired().collect { result ->
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
        coreLogic.getSessionScope(userId).calls.observeEndCallDialog().collect {
            featureFlagState = featureFlagState.copy(showCallEndedBecauseOfConversationDegraded = true)
        }

    fun dismissSelfDeletingMessagesDialog() {
        featureFlagState = featureFlagState.copy(shouldShowSelfDeletingMessagesDialog = false)
        viewModelScope.launch {
            currentUserId?.let {
                coreLogic.getSessionScope(it).markSelfDeletingMessagesAsNotified()
            }
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
            currentUserId?.let {
                coreLogic.getSessionScope(it).markGuestLinkFeatureFlagAsNotChanged()
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
                coreLogic.getSessionScope(it).markTeamAppLockStatusAsNotified()
            }
        }
    }

    fun confirmAppLockNotEnforced() {
        viewModelScope.launch {
            when (globalDataStore.getAppLockSource()) {
                AppLockSource.Manual -> {}

                AppLockSource.TeamEnforced -> disableAppLockUseCase()
            }
        }
    }

    fun isUserAppLockSet() = globalDataStore.isAppLockPasscodeSet()

    fun getE2EICertificate(e2eiRequired: FeatureFlagState.E2EIRequired, context: Context) {
        featureFlagState = featureFlagState.copy(isE2EILoading = true)
        currentUserId?.let { userId ->
            GetE2EICertificateUseCase(coreLogic.getSessionScope(userId).enrollE2EI, dispatcherProvider).invoke(context) { result ->
                result.fold({
                    featureFlagState = featureFlagState.copy(
                        isE2EILoading = false,
                        e2EIRequired = null,
                        e2EIResult = FeatureFlagState.E2EIResult.Failure(e2eiRequired)
                    )
                }, {
                    if (it is E2EIEnrollmentResult.Finalized) {
                        featureFlagState = featureFlagState.copy(
                            isE2EILoading = false,
                            e2EIRequired = null,
                            e2EIResult = FeatureFlagState.E2EIResult.Success(it.certificate)
                        )
                    } else if (it is E2EIEnrollmentResult.Failed) {
                        featureFlagState = featureFlagState.copy(
                            isE2EILoading = false,
                            e2EIRequired = null,
                            e2EIResult = FeatureFlagState.E2EIResult.Failure(e2eiRequired)
                        )
                    }
                })
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

    fun dismissSuccessE2EIdDialog() {
        featureFlagState = featureFlagState.copy(e2EIResult = null)
    }

    companion object {
        private const val TAG = "FeatureFlagNotificationViewModel"
    }
}
