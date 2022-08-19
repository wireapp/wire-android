package com.wire.android.ui.home.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject constructor(private val navigationManager: NavigationManager) : ViewModel() {
    fun navigateTo(item: NavigationItem) = viewModelScope.launch { navigationManager.navigate(NavigationCommand(item.getRouteWithArgs())) }
}
