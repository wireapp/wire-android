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
import com.wire.kalium.logic.feature.user.webSocketStatus.EnableWebSocketUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.IsWebSocketEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkSettingsViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val enableWebSocketUseCase: EnableWebSocketUseCase,
    isWebSocketEnabledUseCase: IsWebSocketEnabledUseCase
) : ViewModel() {
    var isWebSocketEnabled by mutableStateOf(isWebSocketEnabledUseCase())

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    fun setWebSocketState(isEnabled: Boolean, context: Context) {
        enableWebSocketUseCase(isEnabled)
        isWebSocketEnabled = isEnabled
        if (isEnabled) {
            context.startService(Intent(context, PersistentWebSocketService::class.java))
        } else {
            val intentStop = Intent(context, PersistentWebSocketService::class.java)
            intentStop.action = PersistentWebSocketService.ACTION_STOP_FOREGROUND
            context.startService(intentStop)
        }
    }


}
