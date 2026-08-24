/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import com.wire.android.di.metro.wireAssistedMetroViewModel
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.register.RegisterDeviceViewModel
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceViewModel

@Composable
fun registerDeviceViewModel(viewModelStoreOwner: ViewModelStoreOwner = authenticationViewModelStoreOwner): RegisterDeviceViewModel<com.wire.navigation.WireSessionId> =
    authenticationViewModel(viewModelStoreOwner)

@Composable
fun removeDeviceViewModel(viewModelStoreOwner: ViewModelStoreOwner = authenticationViewModelStoreOwner): RemoveDeviceViewModel<Device> =
    authenticationViewModel(viewModelStoreOwner)

@Composable
fun clearSessionViewModel(viewModelStoreOwner: ViewModelStoreOwner = authenticationViewModelStoreOwner): ClearSessionViewModel =
    wireAssistedMetroViewModel<ClearSessionViewModel, SessionAuthenticationManualViewModelFactory>(owner = viewModelStoreOwner) {
        clearSessionViewModel(LocalAuthenticationCancelUserId.current)
    }
