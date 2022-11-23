package com.wire.android.ui.home.settings.appsettings.networkSettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import com.wire.android.services.ServicesManager
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
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
    private val observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase,
    private val currentSession: CurrentSessionUseCase
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

            when (val currentSession = currentSession()) {
                is CurrentSessionResult.Success -> {
                    val userId = currentSession.accountInfo.userId

                    observePersistentWebSocketConnectionStatus().collect {
                        it.map { persistentWebSocketStatus ->
                            if (persistentWebSocketStatus.userId == userId) {
                                networkSettingsState =
                                    networkSettingsState.copy(
                                        isPersistentWebSocketConnectionEnabled = persistentWebSocketStatus.isPersistentWebSocketEnabled
                                    )
                            }
                        }
                        if (it.map { it.isPersistentWebSocketEnabled }.contains(true)) {
                            servicesManager.startPersistentWebSocketService()

                        } else {
                            servicesManager.stopPersistentWebSocketService()
                        }

                    }
                }
                else -> {
                    // NO SESSION - Nothing to do
                }
            }
        }

    fun setWebSocketState(isEnabled: Boolean) {
        viewModelScope.launch {
            persistPersistentWebSocketConnectionStatus(isEnabled)
        }
    }
}
