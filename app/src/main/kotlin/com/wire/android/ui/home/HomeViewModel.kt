package com.wire.android.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.ViewModel
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    suspend fun navigateTo(item: NavigationItem) {
        navigationManager.navigate(NavigationCommand(item.route))
    }

    suspend fun navigateToUserProfile() = navigateTo(NavigationItem.UserProfile)
}
