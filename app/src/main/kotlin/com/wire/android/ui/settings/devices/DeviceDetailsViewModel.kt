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
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.GetClientDetailsResult
import com.wire.kalium.logic.feature.client.GetClientDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val deleteClient: DeleteClientUseCase,
    private val getClientDetails: GetClientDetailsUseCase
) : SavedStateViewModel(savedStateHandle) {

    private val deviceId: ClientId = ClientId(
        savedStateHandle.get<String>(EXTRA_DEVICE_ID)!!
    )

    var state: DeviceDetailsState? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            when (val result = getClientDetails(deviceId)) {
                is GetClientDetailsResult.Failure.Generic -> {
                    appLogger.e("Error getting self clients $result")
                    navigateBack()
                }
                is GetClientDetailsResult.Success -> {
                    state = DeviceDetailsState(Device(result.client), isCurrentDevice = result.isCurrentClient)
                }
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }
}
