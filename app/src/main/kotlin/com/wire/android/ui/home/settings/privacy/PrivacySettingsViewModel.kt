package com.wire.android.ui.home.settings.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    fun navigateBack() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }
}
