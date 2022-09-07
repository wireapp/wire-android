package com.wire.android.ui.home.settings.appsettings.networkSettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkSettingsViewModel
@Inject constructor(
    private val servicesManager: ServicesManager,
    private val navigationManager: NavigationManager,
    private val persistPersistentWebSocketConnectionStatus: PersistPersistentWebSocketConnectionStatusUseCase,
    private val observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase
) : ViewModel() {
    var networkSettingsState by mutableStateOf(NetworkSettingsState())

    init {
        observePersistentWebSocketConnection()
    }

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    private fun observePersistentWebSocketConnection() =
        viewModelScope.launch {
            observePersistentWebSocketConnectionStatus().collect {
                networkSettingsState = networkSettingsState.copy(isPersistentWebSocketConnectionEnabled = it)
            }
        }

    fun setWebSocketState(isEnabled: Boolean) {
        persistPersistentWebSocketConnectionStatus(isEnabled)
        networkSettingsState = networkSettingsState.copy(isPersistentWebSocketConnectionEnabled = isEnabled)
        if (isEnabled) {
            servicesManager.startPersistentWebSocketService()
        } else {
            // TODO FIXME when we'll have a multi-accounts, this will stop all the PersistedWebSockets for all accounts!!!
            servicesManager.stopPersistentWebSocketService()
        }
    }
}
