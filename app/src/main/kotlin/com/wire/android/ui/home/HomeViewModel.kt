package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
import com.wire.android.ui.home.conversations.search.SearchPeopleViewModel
import com.wire.android.util.EMPTY
import com.wire.android.util.LogFileWriter
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    logFileWriter: LogFileWriter
) : SavedStateViewModel(savedStateHandle) {

    var homeState by mutableStateOf(
        HomeState(
            logFilePath = logFileWriter.activeLoggingFile.absolutePath
        )
    )
        private set


    private val mutableSearchQueryFlow = MutableStateFlow("")
    private val searchQueryFlow = mutableSearchQueryFlow
        .asStateFlow()
        .debounce(SearchPeopleViewModel.DEFAULT_SEARCH_QUERY_DEBOUNCE)

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
                homeState = HomeState(
                    selfUser.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                    selfUser.availabilityStatus
                )
            }
        }
    }
//
//    fun searchConversation(searchQuery: TextFieldValue) {
//        val textQueryChanged = searchQueryTextFieldFlow.value.text != searchQuery.text
//        // we set the state with a searchQuery, immediately to update the UI first
//        viewModelScope.launch {
//            searchQueryTextFieldFlow.emit(searchQuery)
//
//            if (textQueryChanged) mutableSearchQueryFlow.emit(searchQuery.text)
//        }
//    }

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
    val logFilePath: String = String.EMPTY
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
