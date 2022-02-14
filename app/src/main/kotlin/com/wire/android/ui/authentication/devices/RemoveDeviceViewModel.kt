package com.wire.android.ui.authentication.devices

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wire.android.ui.authentication.devices.mock.mockDevices
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RemoveDeviceViewModel @Inject constructor(

) : ViewModel() {

    var state by mutableStateOf(
        RemoveDeviceState(deviceList = mockDevices) //TODO
    )
        private set
}
