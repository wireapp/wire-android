package com.wire.android.ui.userprofile.self

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.mapper.OtherAccountMapper
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
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
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Suppress for now after removing mockMethodForAvatar it should not complain
@Suppress("TooManyFunctions", "LongParameterList")
@ExperimentalMaterial3Api
@HiltViewModel
class SelfUserProfileViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
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
    private val kaliumConfigs: KaliumConfigs,
    private val otherAccountMapper: OtherAccountMapper,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val accountSwitch: AccountSwitchUseCase,
    private val endCall: EndCallUseCase
) : ViewModel() {

    var userProfileState by mutableStateOf(SelfUserProfileState())
        private set

    private val establishedCallsList: MutableStateFlow<List<Call>> = MutableStateFlow(emptyList())

    init {
        viewModelScope.launch {
            fetchSelfUser()
            observeEstablishedCall()
        }
    }

    private fun observeEstablishedCall() {
        viewModelScope.launch {
            observeEstablishedCalls()
                .flowOn(dispatchers.io())
                .collect {
                    establishedCallsList.emit(it)
                }
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
                        userProfileState = SelfUserProfileState(
                            status = availabilityStatus,
                            fullName = name.orEmpty(),
                            userName = handle.orEmpty(),
                            teamName = selfTeam?.name,
                            otherAccounts = otherAccounts,
                            avatarAsset = userProfileState.avatarAsset
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

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun logout(wipeData: Boolean) {
        viewModelScope.launch {
            launch {
                establishedCallsList.value.forEach { call ->
                    endCall(call.conversationId)
                }
            }
            val logoutReason = if (wipeData) LogoutReason.SELF_HARD_LOGOUT else LogoutReason.SELF_SOFT_LOGOUT
            logout(logoutReason)
            dataStore.clear() // TODO this should be moved to some service that will clear all the data in the app
            accountSwitch(SwitchAccountParam.SwitchToNextAccountOrWelcome)
        }
    }

    fun switchAccount(userId: UserId) {
        viewModelScope.launch {
            accountSwitch(SwitchAccountParam.SwitchToAccount(userId))
        }
    }

    fun addAccount() {
        viewModelScope.launch {
            // the total number of accounts is otherAccounts + 1 for the current account
            val canAddNewAccounts: Boolean = (userProfileState.otherAccounts.size + 1) < kaliumConfigs.maxAccount

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
            navigationManager.navigate(NavigationCommand(NavigationItem.Welcome.getRouteWithArgs()))
        }
    }

    fun editProfile() {
        viewModelScope.launch { // TODO change to "Your Account Settings" when implemented
            navigationManager.navigate(NavigationCommand(NavigationItem.AppSettings.getRouteWithArgs()))
        }
    }

    fun dismissStatusDialog() {
        userProfileState = userProfileState.copy(statusDialogData = null)
    }

    fun changeStatus(status: UserAvailabilityStatus) {
        setNotShowStatusRationaleAgainIfNeeded(status)
        //TODO add the broadcast message to inform everyone about the self user new status
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

    fun onChangeProfilePictureClicked() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.ProfileImagePicker.getRouteWithArgs()))
        }
    }

    sealed class ErrorCodes {
        object DownloadUserInfoError : ErrorCodes()
    }
}
