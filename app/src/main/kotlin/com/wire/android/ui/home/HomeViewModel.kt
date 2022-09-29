package com.wire.android.ui.home

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.R
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
import com.wire.android.util.getDeviceId
import com.wire.android.util.sha256
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@Suppress("LongParameterList")
@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getSelf: GetSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val logFileWriter: LogFileWriter
) : SavedStateViewModel(savedStateHandle) {

    var userAvatar by mutableStateOf(SelfUserData())
        private set

    init {
        loadUserAvatar()
    }

    fun logFilePath(): String = logFileWriter.activeLoggingFile.absolutePath

    // TODO(localization): localize if needed
    fun reportBugEmailTemplate(deviceHash: String? = "unavailable"): String = """
        --- DO NOT EDIT---
        App Version: ${BuildConfig.VERSION_NAME}
        Device Hash: $deviceHash
        Device: ${Build.MANUFACTURER} - ${Build.MODEL}
        SDK: ${Build.VERSION.RELEASE}
        Date: ${Date()}
        ------------------

        Please fill in the following

        - Date & Time of when the issue occurred:


        - What happened:


        - Steps to reproduce (if relevant):
        
    """.trimIndent()

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
            }
        }
    }

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
                userAvatar = SelfUserData(
                    selfUser.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                    selfUser.availabilityStatus
                )
            }
        }
    }

    suspend fun navigateTo(item: NavigationItem) {
        navigationManager.navigate(NavigationCommand(destination = item.getRouteWithArgs()))
    }

    fun navigateToUserProfile() = viewModelScope.launch { navigateTo(NavigationItem.SelfUserProfile) }
}

data class SelfUserData(
    val avatarAsset: UserAvatarAsset? = null,
    val status: UserAvailabilityStatus = UserAvailabilityStatus.NONE
)

// TODO change to extend [SnackBarMessage]
sealed class HomeSnackbarState {
    class SuccessConnectionIgnoreRequest(val userName: String) : HomeSnackbarState()
    object MutingOperationError : HomeSnackbarState()
    object BlockingUserOperationError : HomeSnackbarState()
    class BlockingUserOperationSuccess(val userName: String) : HomeSnackbarState()
    object UnblockingUserOperationError : HomeSnackbarState()
    class DeletedConversationGroupSuccess(val groupName: String) : HomeSnackbarState()
    object DeleteConversationGroupError : HomeSnackbarState()
    object LeftConversationSuccess : HomeSnackbarState()
    object LeaveConversationError : HomeSnackbarState()
    object None : HomeSnackbarState()
}
