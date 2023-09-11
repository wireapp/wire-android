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

package com.wire.android.ui.userprofile.self

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.mapper.OtherAccountMapper
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.ui.userprofile.self.dialog.StatusDialogData
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Suppress for now after removing mockMethodForAvatar it should not complain
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class SelfUserProfileViewModel @Inject constructor(
    @CurrentAccount private val selfUserId: UserId,
    private val dataStore: UserDataStore,
    private val getSelf: GetSelfUserUseCase,
    private val getSelfTeam: GetSelfTeamUseCase,
    private val observeValidAccounts: ObserveValidAccountsUseCase,
    private val updateStatus: UpdateSelfAvailabilityStatusUseCase,
    private val logout: LogoutUseCase,
    private val dispatchers: DispatcherProvider,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val selfServerLinks: SelfServerConfigUseCase,
    private val otherAccountMapper: OtherAccountMapper,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val accountSwitch: AccountSwitchUseCase,
    private val endCall: EndCallUseCase,
    private val isReadOnlyAccount: IsReadOnlyAccountUseCase,
    private val notificationChannelsManager: NotificationChannelsManager,
    private val notificationManager: WireNotificationManager
) : ViewModel() {

    var userProfileState by mutableStateOf(SelfUserProfileState(userId = selfUserId, isAvatarLoading = true))
        private set

    private lateinit var establishedCallsList: StateFlow<List<Call>>

    init {
        viewModelScope.launch {
            fetchSelfUser()
            observeEstablishedCall()
            fetchIsReadOnlyAccount()
        }
    }

    private suspend fun fetchIsReadOnlyAccount() {
        val isReadOnlyAccount = isReadOnlyAccount()
        userProfileState = userProfileState.copy(isReadOnlyAccount = isReadOnlyAccount)
    }

    private fun observeEstablishedCall() {
        viewModelScope.launch {
            establishedCallsList = observeEstablishedCalls()
                .distinctUntilChanged()
                .flowOn(dispatchers.io())
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }
    }

    fun isUserInCall(): Boolean = establishedCallsList.value.isNotEmpty()

    private suspend fun fetchSelfUser() {
        viewModelScope.launch {
            val self = getSelf().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))
            val selfTeam = getSelfTeam().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))
            val validAccounts =
                observeValidAccounts().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))
            combine(
                self,
                selfTeam,
                validAccounts
            ) { selfUser: SelfUser, team: Team?, list: List<Pair<SelfUser, Team?>> ->
                Triple(
                    selfUser,
                    team,
                    list.filter { it.first.id != selfUser.id }
                        .map { (selfUser, team) -> otherAccountMapper.toOtherAccount(selfUser, team) }
                )
            }
                .distinctUntilChanged()
                .collect { (selfUser, selfTeam, otherAccounts) ->
                    with(selfUser) {
                        // Load user avatar raw image data
                        completePicture?.let { updateUserAvatar(it) }

                        // Update user data state
                        userProfileState = userProfileState.copy(
                            status = availabilityStatus,
                            fullName = name.orEmpty(),
                            userName = handle.orEmpty(),
                            teamName = selfTeam?.name,
                            otherAccounts = otherAccounts,
                            avatarAsset = userProfileState.avatarAsset,
                            isAvatarLoading = false,
                        )
                    }
                }
        }
    }

    private fun showErrorMessage() {
        userProfileState = userProfileState.copy(errorMessageCode = ErrorCodes.DownloadUserInfoError)
    }

    private fun showLoadingAvatar(show: Boolean) {
        userProfileState = userProfileState.copy(isAvatarLoading = show)
    }

    private fun updateUserAvatar(avatarAssetId: UserAssetId) {
        if (avatarAssetId == userProfileState.avatarAsset?.userAssetId) {
            return
        }

        // We try to download the user avatar on a separate thread so that we don't block the display of the user's info
        viewModelScope.launch {
            showLoadingAvatar(true)
            try {
                userProfileState = userProfileState.copy(
                    avatarAsset = UserAvatarAsset(wireSessionImageLoader, avatarAssetId)
                )
                // Update avatar asset id on user data store
                // TODO: obtain the asset id through a useCase once we also store assets ids
                withContext(dispatchers.io()) { dataStore.updateUserAvatarAssetId(avatarAssetId.toString()) }
            } catch (e: ClassCastException) {
                appLogger.e("There was an error while downloading the user avatar", e)
                // Show error snackbar if avatar download fails
                showErrorMessage()
            }

            showLoadingAvatar(false)
        }
    }

    fun logout(wipeData: Boolean, switchAccountActions: SwitchAccountActions) {
        viewModelScope.launch {
            userProfileState = userProfileState.copy(isLoggingOut = true)
            launch {
                establishedCallsList.value.forEach { call ->
                    endCall(call.conversationId)
                }
            }.join()

            val logoutReason = if (wipeData) LogoutReason.SELF_HARD_LOGOUT else LogoutReason.SELF_SOFT_LOGOUT
            logout(logoutReason)
            if (wipeData) {
                // TODO this should be moved to some service that will clear all the data in the app
                dataStore.clear()
            }

            notificationManager.stopObservingOnLogout(selfUserId)
            notificationChannelsManager.deleteChannelGroup(selfUserId)
            accountSwitch(SwitchAccountParam.TryToSwitchToNextAccount)
                .callAction(switchAccountActions)
        }
    }

    fun switchAccount(userId: UserId, switchAccountActions: SwitchAccountActions) {
        viewModelScope.launch {
            accountSwitch(SwitchAccountParam.SwitchToAccount(userId))
                .callAction(switchAccountActions)
        }
    }

    fun tryToInitAddingAccount(onSucceeded: () -> Unit) {
        viewModelScope.launch {
            // the total number of accounts is otherAccounts + 1 for the current account
            val canAddNewAccounts: Boolean = (userProfileState.otherAccounts.size + 1) < BuildConfig.MAX_ACCOUNTS

            if (!canAddNewAccounts) {
                userProfileState = userProfileState.copy(maxAccountsReached = true)
                return@launch
            }

            val selfServerLinks: ServerConfig.Links =
                when (val result = selfServerLinks()) {
                    is SelfServerConfigUseCase.Result.Failure -> return@launch
                    is SelfServerConfigUseCase.Result.Success -> result.serverLinks.links
                }
            authServerConfigProvider.updateAuthServer(selfServerLinks)
            onSucceeded()
        }
    }

    fun dismissStatusDialog() {
        userProfileState = userProfileState.copy(statusDialogData = null)
    }

    fun changeStatus(status: UserAvailabilityStatus) {
        setNotShowStatusRationaleAgainIfNeeded(status)
        viewModelScope.launch { updateStatus(status) }
        dismissStatusDialog()
    }

    fun dialogCheckBoxStateChanged(isChecked: Boolean) {
        userProfileState.run {
            userProfileState = copy(statusDialogData = statusDialogData?.changeCheckBoxState(isChecked))
        }
    }

    fun changeStatusClick(status: UserAvailabilityStatus) {
        if (userProfileState.status == status) return

        viewModelScope.launch {
            if (shouldShowStatusRationaleDialog(status)) {
                val statusDialogInfo = when (status) {
                    UserAvailabilityStatus.AVAILABLE -> StatusDialogData.StateAvailable()
                    UserAvailabilityStatus.BUSY -> StatusDialogData.StateBusy()
                    UserAvailabilityStatus.AWAY -> StatusDialogData.StateAway()
                    UserAvailabilityStatus.NONE -> StatusDialogData.StateNone()
                }
                userProfileState = userProfileState.copy(statusDialogData = statusDialogInfo)
            } else {
                changeStatus(status)
            }
        }
    }

    fun onMaxAccountReachedDialogDismissed() {
        userProfileState = userProfileState.copy(maxAccountsReached = false)
    }

    private fun setNotShowStatusRationaleAgainIfNeeded(status: UserAvailabilityStatus) {
        userProfileState.statusDialogData.let { dialogState ->
            if (dialogState?.isCheckBoxChecked == true) {
                viewModelScope.launch { dataStore.dontShowStatusRationaleAgain(status) }
            }
        }
    }

    private suspend fun shouldShowStatusRationaleDialog(status: UserAvailabilityStatus): Boolean =
        dataStore.shouldShowStatusRationaleFlow(status).first()

    fun clearErrorMessage() {
        userProfileState = userProfileState.copy(errorMessageCode = null)
    }

    sealed class ErrorCodes {
        object DownloadUserInfoError : ErrorCodes()
    }
}
