package com.wire.android.ui.drawer

import androidx.lifecycle.ViewModel
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val navigationManager: NavigationManager
) : ViewModel() {

    suspend fun navigateTo(item: NavigationItem) {
        navigationManager.navigate(item)
        navigationManager.drawerState(false)
    }

}
