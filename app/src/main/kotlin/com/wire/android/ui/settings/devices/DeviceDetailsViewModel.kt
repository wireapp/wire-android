package com.wire.android.ui.settings.devices

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.navigation.EXTRA_DEVICE_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.settings.devices.model.DeviceDetailsState
import com.wire.kalium.logic.data.conversation.ClientId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager

) : SavedStateViewModel(savedStateHandle) {

    private val deviceId: ClientId = ClientId(
        savedStateHandle.get<String>(EXTRA_DEVICE_ID)!!
    )

    var state: DeviceDetailsState by mutableStateOf(
        DeviceDetailsState(device = Device("pixel 3a", ClientId(deviceId.value)), isCurrentDevice = false)
    )
        private set

    init {
        appLogger.d(">>> DeviceDetailsViewModel extra: ${savedStateHandle.get<String>(EXTRA_DEVICE_ID)!!}")
        appLogger.d(">>> DeviceDetailsViewModel $deviceId")

        state = DeviceDetailsState(device = Device("pixel 3a", deviceId, "1676509508296", true), isCurrentDevice = false)
    }

    fun navigateBack() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }
}
