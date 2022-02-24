package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.sync.ListenToEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val listenToEvents: ListenToEventsUseCase,
    private val commonManager: HomeCommonManager
) : ViewModel() {

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

    suspend fun navigateTo(item: NavigationItem, extraRouteId: String = "") {
        navigationManager.navigate(NavigationCommand(destination = item.getRouteWithArgs(listOf(extraRouteId))))
    }

    init {
        //listen for the WebSockets updates and update DB accordingly
        viewModelScope.launch {
            listenToEvents()
        }
    }

    companion object {
        const val MY_USER_PROFILE_SUBROUTE = "myUserProfile"
    }
}
