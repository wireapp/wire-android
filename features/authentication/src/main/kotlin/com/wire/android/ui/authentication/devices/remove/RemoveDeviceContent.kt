/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 */
package com.wire.android.ui.authentication.devices.remove

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.authentication.devices.deviceAuthenticationSlideTransition
import com.wire.android.ui.authentication.devices.register.AuthenticationFailure
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold

/**
 * Generic remove-device presentation. Device rendering and shared dialogs are supplied by the
 * app, so the feature never imports host models, Kalium, app R, or date/fingerprint formatting.
 */
@Composable
fun <DeviceT> RemoveDeviceContent(
    viewModel: RemoveDeviceViewModel<DeviceT>,
    placeholderDevice: (Int) -> DeviceT,
    deviceItem: @Composable (device: DeviceT, placeholder: Boolean, onClick: (DeviceT) -> Unit) -> Unit,
    removeDialog: @Composable (
        dialog: RemoveDeviceAuthenticationDialogState.Visible<DeviceT>,
        password: TextFieldState,
        invalidPassword: Boolean,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit,
    ) -> Unit,
    initialLoadErrorDialog: @Composable (onCancel: () -> Unit, onRetry: () -> Unit) -> Unit,
    genericFailureDialog: @Composable (AuthenticationFailure, onDismiss: () -> Unit) -> Unit,
    cancelDialog: @Composable () -> Unit,
    onBack: () -> Unit,
    onComplete: (OnComplete) -> Unit,
    modifier: Modifier = Modifier,
) {
    val codeRequired = viewModel.secondFactorVerificationCodeState.isCodeInputNecessary
    AnimatedContent(
        targetState = codeRequired,
        transitionSpec = { deviceAuthenticationSlideTransition() },
        modifier = modifier.fillMaxSize(),
    ) { needsCode ->
        if (needsCode) RemoveDeviceVerificationCodeScreen(viewModel)
        else RemoveDeviceListContent(
            state = viewModel.state,
            password = viewModel.passwordTextState,
            placeholderDevice = placeholderDevice,
            deviceItem = deviceItem,
            removeDialog = removeDialog,
            cancelDialog = cancelDialog,
            onBack = onBack,
            onItemClick = viewModel::onItemClicked,
            onDismiss = viewModel::onDialogDismissed,
            onConfirm = viewModel::onRemoveConfirmed,
        )
    }
    when (val error = viewModel.state.error) {
        RemoveDeviceAuthenticationError.InitError -> initialLoadErrorDialog(onBack, viewModel::retryFetch)
        is RemoveDeviceAuthenticationError.GenericError -> genericFailureDialog(error.failure, viewModel::clearDeleteClientError)
        else -> Unit
    }
    HandleActions(viewModel.actions) { action ->
        when (action) {
            is OnComplete -> onComplete(action)
        }
    }
}

@Composable
private fun <DeviceT> RemoveDeviceListContent(
    state: RemoveDeviceAuthenticationState<DeviceT>,
    password: TextFieldState,
    placeholderDevice: (Int) -> DeviceT,
    deviceItem: @Composable (DeviceT, Boolean, (DeviceT) -> Unit) -> Unit,
    removeDialog: @Composable (
        RemoveDeviceAuthenticationDialogState.Visible<DeviceT>,
        TextFieldState,
        Boolean,
        () -> Unit,
        () -> Unit,
    ) -> Unit,
    cancelDialog: @Composable () -> Unit,
    onBack: () -> Unit,
    onItemClick: (DeviceT) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    BackHandler(onBack = onBack)
    cancelDialog()
    val listState = rememberLazyListState()
    WireScaffold(topBar = {
        RemoveDeviceTopBar(listState.rememberTopBarElevationState().value, onBack)
    }) { padding ->
        Box(Modifier.padding(padding)) {
            val placeholders = state.isLoadingClientsList
            val devices = if (placeholders) List(4, placeholderDevice) else state.deviceList
            SurfaceBackgroundWrapper {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(devices) { index, device ->
                        deviceItem(device, placeholders, onItemClick)
                        if (index < devices.lastIndex) WireDivider()
                    }
                }
            }
        }
        val dialog = state.removeDeviceDialogState as? RemoveDeviceAuthenticationDialogState.Visible
        if (!state.isLoadingClientsList && dialog != null) {
            removeDialog(
                dialog,
                password,
                state.error is RemoveDeviceAuthenticationError.InvalidCredentialsError,
                onDismiss,
                onConfirm,
            )
        }
    }
}
