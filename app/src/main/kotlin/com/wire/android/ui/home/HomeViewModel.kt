package com.wire.android.ui.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Suppress("LongParameterList")
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val getSelf: GetSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val homeSnackbarManager: HomeSnackbarManager
) : ViewModel() {

    val snackbarMessageFlow = homeSnackbarManager.snackbarMessageFlow

    var userAvatar by mutableStateOf(SelfUserData())
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
                            VoyagerNavigationItem.RegisterDevice,
                            BackStackMode.CLEAR_WHOLE
                        )
                    )
                    return@launch
                }
                getSelf().first().handle.isNullOrEmpty() -> { // check if the user handle has been set and open the proper screen if not
                    navigationManager.navigate(
                        NavigationCommand(
                            VoyagerNavigationItem.CreateUsername,
                            BackStackMode.CLEAR_WHOLE
                        )
                    )
                    return@launch
                }
            }
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

    fun navigateTo(item: VoyagerNavigationItem) {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(destination = item))
        }
    }

    fun navigateToUserProfile() = viewModelScope.launch { navigateTo(VoyagerNavigationItem.SelfUserProfile) }
}

data class SelfUserData(
    val avatarAsset: UserAvatarAsset? = null,
    val status: UserAvailabilityStatus = UserAvailabilityStatus.NONE
)

@Singleton
class HomeSnackbarManager @Inject constructor() {
    private val _snackbarMessageFlow = MutableSharedFlow<UIText>()
    val snackbarMessageFlow: Flow<UIText> = _snackbarMessageFlow
    suspend fun showSnackbarMessage(text: UIText) = _snackbarMessageFlow.emit(text)
}
