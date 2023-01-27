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

package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONNECTION_IGNORED_USER_NAME
import com.wire.android.navigation.EXTRA_GROUP_DELETED_NAME
import com.wire.android.navigation.EXTRA_LEFT_GROUP
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.navigation.getBackNavArg
import com.wire.android.util.LogFileWriter
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val globalDataStore: GlobalDataStore,
    private val navigationManager: NavigationManager,
    private val getSelf: GetSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    logFileWriter: LogFileWriter
) : SavedStateViewModel(savedStateHandle) {

    var homeState by mutableStateOf(
        HomeState(
            logFilePath = logFileWriter.activeLoggingFile.absolutePath
        )
    )
        private set

    init {
        loadUserAvatar()
    }

    fun checkRequirements() {
        viewModelScope.launch {
            when {
                needsToRegisterClient() -> { // check if the client has been registered and open the proper screen if not
                    navigationManager.navigate(
                        NavigationCommand(
                            NavigationItem.RegisterDevice.getRouteWithArgs(),
                            BackStackMode.CLEAR_WHOLE
                        )
                    )
                    return@launch
                }
                getSelf().first().handle.isNullOrEmpty() -> { // check if the user handle has been set and open the proper screen if not
                    navigationManager.navigate(
                        NavigationCommand(
                            NavigationItem.CreateUsername.getRouteWithArgs(),
                            BackStackMode.CLEAR_WHOLE
                        )
                    )
                    return@launch
                }
                shouldDisplayWelcomeToARScreen() -> {
                    homeState = homeState.copy(shouldDisplayWelcomeMessage = true)
                }
            }
        }
    }

    private suspend fun shouldDisplayWelcomeToARScreen() =
        globalDataStore.isMigrationCompleted() && !globalDataStore.isWelcomeScreenPresented()

    fun checkPendingSnackbarState(): HomeSnackbarState? {
        return with(savedStateHandle) {
            getBackNavArg<String>(EXTRA_CONNECTION_IGNORED_USER_NAME)
                ?.let { HomeSnackbarState.SuccessConnectionIgnoreRequest(it) }
                ?: getBackNavArg<String>(EXTRA_GROUP_DELETED_NAME)
                    ?.let { HomeSnackbarState.DeletedConversationGroupSuccess(it) }
                ?: getBackNavArg<Boolean>(EXTRA_LEFT_GROUP)
                    ?.let { if (it) HomeSnackbarState.LeftConversationSuccess else null }
        }
    }

    private fun loadUserAvatar() {
        viewModelScope.launch {
            getSelf().collect { selfUser ->
                homeState = HomeState(
                    selfUser.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                    selfUser.availabilityStatus,
                    homeState.logFilePath
                )
            }
        }
    }

    fun dismissWelcomeMessage() {
        viewModelScope.launch {
            globalDataStore.setWelcomeScreenPresented()
            homeState = homeState.copy(shouldDisplayWelcomeMessage = false)
        }
    }

    fun navigateTo(item: NavigationItem) {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(destination = item.getRouteWithArgs()))
        }
    }

    fun navigateToSelfUserProfile() = viewModelScope.launch { navigateTo(NavigationItem.SelfUserProfile) }
}

data class HomeState(
    val avatarAsset: UserAvatarAsset? = null,
    val status: UserAvailabilityStatus = UserAvailabilityStatus.NONE,
    val logFilePath: String,
    val shouldDisplayWelcomeMessage: Boolean = false,
)

// TODO change to extend [SnackBarMessage]
sealed class HomeSnackbarState {
    object None : HomeSnackbarState()
    data class ClearConversationContentSuccess(val isGroup: Boolean) : HomeSnackbarState()
    data class ClearConversationContentFailure(val isGroup: Boolean) : HomeSnackbarState()

    class SuccessConnectionIgnoreRequest(val userName: String) : HomeSnackbarState()
    object MutingOperationError : HomeSnackbarState()
    object BlockingUserOperationError : HomeSnackbarState()
    data class BlockingUserOperationSuccess(val userName: String) : HomeSnackbarState()
    object UnblockingUserOperationError : HomeSnackbarState()
    data class DeletedConversationGroupSuccess(val groupName: String) : HomeSnackbarState()
    object DeleteConversationGroupError : HomeSnackbarState()
    object LeftConversationSuccess : HomeSnackbarState()
    object LeaveConversationError : HomeSnackbarState()
}
