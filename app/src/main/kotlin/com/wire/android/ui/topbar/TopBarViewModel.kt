package com.wire.android.ui.topbar

import androidx.lifecycle.ViewModel
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TopBarViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    suspend fun openUserProfile() = navigationManager.navigate(NavigationItem.UserProfile)

    suspend fun goBack() = navigationManager.navigateBack()

    suspend fun openDrawer() = navigationManager.drawerState(true)

}
