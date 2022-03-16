package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.create.CreateAccountUsernameFlowType
import com.wire.kalium.logic.feature.asset.GetPublicAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.sync.ListenToEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val listenToEvents: ListenToEventsUseCase,
    private val dataStore: UserDataStore,
    private val getPublicAsset: GetPublicAssetUseCase,
    private val getSelf: GetSelfUserUseCase,
    private val commonManager: HomeCommonManager
) : ViewModel() {

    var userAvatar by mutableStateOf<ByteArray?>(null)
        private set

    init {
        commonManager.onViewModelInit()
    }

    private val scrollBridge = commonManager.scrollBridge!!

    override fun onCleared() {
        commonManager.onViewModelCleared()
        super.onCleared()
    }

    val scrollDownFlow: Flow<Boolean> = scrollBridge.scrollDownFlow

    suspend fun navigateToUserProfile() = navigateTo(NavigationItem.UserProfile, MY_USER_PROFILE_SUBROUTE)

    fun loadUserAvatar() {
        viewModelScope.launch {
            try {
                dataStore.avatarAssetId.first()?.let {
                    userAvatar = (getPublicAsset(it) as PublicAssetResult.Success).asset
                }
            } catch (_: ClassCastException) {}
        }
    }

    suspend fun navigateTo(item: NavigationItem, extraRouteId: String = "") {
        navigationManager.navigate(NavigationCommand(destination = item.getRouteWithArgs(listOf(extraRouteId))))
    }

    init {
        //listen for the WebSockets updates and update DB accordingly
        viewModelScope.launch {
            listenToEvents()
        }

        //check if the user set the handle and open the corresponding screen if not
        viewModelScope.launch {
            getSelf().collect {
                if(it.handle.isNullOrEmpty())
                    navigationManager.navigate(NavigationCommand(
                            NavigationItem.CreateUsername.getRouteWithArgs(listOf(CreateAccountUsernameFlowType.AppStart)),
                        BackStackMode.CLEAR_WHOLE
                    ))
            }
        }
    }

    companion object {
        const val MY_USER_PROFILE_SUBROUTE = "myUserProfile"
    }
}
