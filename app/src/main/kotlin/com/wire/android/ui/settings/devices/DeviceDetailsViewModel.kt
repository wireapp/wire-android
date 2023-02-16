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
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.client.SelfClientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val deleteClient: DeleteClientUseCase,
    private val selfClients: SelfClientsUseCase
) : SavedStateViewModel(savedStateHandle) {

    private val deviceId: String = savedStateHandle.get<String>(EXTRA_DEVICE_ID)!!

    var state: DeviceDetailsState by mutableStateOf(DeviceDetailsState(null, false))
        private set

    init {
        viewModelScope.launch {
            when (val result = selfClients()) {
                is SelfClientsResult.Failure.Generic -> {
                    appLogger.e("Error getting self clients $result")
                    navigateBack()
                }
                is SelfClientsResult.Success -> {
                    val client: Client? = result.clients.firstOrNull {
                        appLogger.d("> comparing ${it.id} with $deviceId")
                        deviceId.contentEquals(it.id.value)
                    }

                    appLogger.d("> client is: $client")
                    state = DeviceDetailsState(Device(client!!), isCurrentDevice = false)
                }
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch { navigationManager.navigateBack() }
    }
}
