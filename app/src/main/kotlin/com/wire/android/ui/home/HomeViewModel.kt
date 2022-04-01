package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.UserDataStore
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.asset.GetPublicAssetUseCase
import com.wire.kalium.logic.feature.asset.PublicAssetResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.sync.ListenToEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    var userAvatar by mutableStateOf<ByteArray?>(null)
        private set

    init {
        //listen for the WebSockets updates and update DB accordingly
        viewModelScope.launch {
            listenToEvents()
        }

        //check if the user set the handle and open the corresponding screen if not
        viewModelScope.launch {
            getSelf().collect {
                if (it.handle.isNullOrEmpty())
                    navigationManager.navigate(
                        NavigationCommand(
                            NavigationItem.CreateUsername.getRouteWithArgs(),
                            BackStackMode.CLEAR_WHOLE
                        )
                    )
            }
        }

        loadUserAvatar()
    }

    private fun loadUserAvatar() {
        viewModelScope.launch {
            try {
                dataStore.avatarAssetId.first()?.let {
                    userAvatar = (getPublicAsset(it) as PublicAssetResult.Success).asset
                }
            } catch (e: ClassCastException) {
                appLogger.e("There was an error loading the user avatar", e)
            }
        }
    }

    suspend fun navigateTo(item: NavigationItem, extraRouteId: String = "") {
        navigationManager.navigate(NavigationCommand(destination = item.getRouteWithArgs(listOf(extraRouteId))))
    }

    fun navigateToUserProfile() = viewModelScope.launch { navigateTo(NavigationItem.SelfUserProfile, MY_USER_PROFILE_SUBROUTE) }

    companion object {
        const val MY_USER_PROFILE_SUBROUTE = "myUserProfile"
    }
}
