package com.wire.android.ui.settings.networkSettings

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.debugscreen.PersistentWebSocketService
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkSettingsViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val persistPersistentWebSocketConnectionStatus: PersistPersistentWebSocketConnectionStatusUseCase,
    private val observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase
) : ViewModel() {
    var networkSettingsState by mutableStateOf(NetworkSettingsState())

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    fun observePersistentWebSocketConnection() =
        viewModelScope.launch {
            observePersistentWebSocketConnectionStatus().collect {
                networkSettingsState = networkSettingsState.copy(isPersistentWebSocketConnectionEnabled = it)
            }
        }


    fun setWebSocketState(isEnabled: Boolean, context: Context) {
        persistPersistentWebSocketConnectionStatus(isEnabled)
        observePersistentWebSocketConnection()
        if (isEnabled) {
            context.startService(Intent(context, PersistentWebSocketService::class.java))
        } else {
            val intentStop = Intent(context, PersistentWebSocketService::class.java)
            intentStop.action = PersistentWebSocketService.ACTION_STOP_FOREGROUND
            context.startService(intentStop)
        }
    }


}
