/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.authentication.devices.remove

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.ui.authentication.devices.DeviceItem
import com.wire.android.ui.authentication.devices.common.ClearSessionState
import com.wire.android.ui.authentication.devices.common.ClearSessionViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.register.AuthenticationFailureDialog
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.SurfaceBackgroundWrapper
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dialogs.CancelLoginDialogContent
import com.wire.android.ui.common.dialogs.CancelLoginDialogState
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.conversation.ClientId

@Composable
internal fun RemoveDeviceRouteScreen(
    viewModel: RemoveDeviceViewModel<Device>,
    clearSessionViewModel: ClearSessionViewModel,
    switchAccountActions: com.wire.android.feature.SwitchAccountActions,
    onE2EIRequired: () -> Unit,
    onHomeRequired: () -> Unit,
    onInitialSyncRequired: () -> Unit,
) {
    clearAutofillTree()

    AnimatedContent(
        targetState = viewModel.secondFactorVerificationCodeState.isCodeInputNecessary,
        transitionSpec = {
            TransitionAnimationType.SLIDE.enterTransition.togetherWith(TransitionAnimationType.SLIDE.exitTransition)
        },
        modifier = Modifier.fillMaxSize()
    ) { isCodeNecessary ->
        if (isCodeNecessary) {
            RemoveDeviceVerificationCodeScreen(viewModel)
        } else {
            RemoveDeviceContent(
                state = viewModel.state,
                passwordTextState = viewModel.passwordTextState,
                clearSessionState = clearSessionViewModel.state,
                onItemClicked = { viewModel.onItemClicked(it) },
                onRemoveConfirm = { viewModel.onRemoveConfirmed() },
                onDialogDismiss = viewModel::onDialogDismissed,
                onErrorDialogDismiss = viewModel::clearDeleteClientError,
                onBackButtonClicked = clearSessionViewModel::onBackButtonClicked,
                onCancelLoginClicked = {
                    clearSessionViewModel.onCancelLoginClicked(switchAccountActions)
                },
                onProceedLoginClicked = clearSessionViewModel::onProceedLoginClicked
            )
        }
    }

    if (viewModel.state.error is RemoveDeviceAuthenticationError.InitError) {
        WireDialog(
            properties = wireDialogPropertiesBuilder(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = stringResource(id = R.string.label_general_error),
            text = stringResource(id = R.string.devices_loading_error),
            onDismiss = viewModel::clearDeleteClientError,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = clearSessionViewModel::onBackButtonClicked,
                text = stringResource(id = R.string.label_cancel),
                type = WireDialogButtonType.Secondary,
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = viewModel::retryFetch,
                text = stringResource(id = R.string.label_retry),
                type = WireDialogButtonType.Primary,
            )
        )
    }

    HandleActions(viewModel.actions) { action ->
        when (action) {
            is OnComplete -> when {
                action.isE2EIRequired -> onE2EIRequired()
                action.initialSyncCompleted -> onHomeRequired()
                else -> onInitialSyncRequired()
            }
        }
    }
}

@Composable
private fun RemoveDeviceContent(
    state: RemoveDeviceAuthenticationState<Device>,
    passwordTextState: TextFieldState,
    clearSessionState: ClearSessionState,
    onItemClicked: (Device) -> Unit,
    onRemoveConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onErrorDialogDismiss: () -> Unit,
    onBackButtonClicked: () -> Unit,
    onCancelLoginClicked: () -> Unit,
    onProceedLoginClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackButtonClicked()
    }
    val cancelLoginDialogState = rememberVisibilityState<CancelLoginDialogState>()
    CancelLoginDialogContent(
        dialogState = cancelLoginDialogState,
        onActionButtonClicked = onCancelLoginClicked,
        onProceedButtonClicked = onProceedLoginClicked,
    )
    if (clearSessionState.showCancelLoginDialog) {
        cancelLoginDialogState.show(
            cancelLoginDialogState.savedState ?: CancelLoginDialogState
        )
    } else {
        cancelLoginDialogState.dismiss()
    }

    val lazyListState = rememberLazyListState()
    WireScaffold(
        modifier = modifier,
        topBar = {
            RemoveDeviceTopBar(
                elevation = lazyListState.rememberTopBarElevationState().value,
                onBackButtonClicked = onBackButtonClicked
            )
        }
    ) { internalPadding ->
        Box(modifier = Modifier.padding(internalPadding)) {
            RemoveDeviceItemsList(
                lazyListState = lazyListState,
                items = when (state.isLoadingClientsList) {
                    true -> List(4) { Device(clientId = ClientId("placeholder_$it")) }
                    false -> state.deviceList
                },
                placeholders = state.isLoadingClientsList,
                onItemClicked = onItemClicked
            )
        }
        // TODO handle list loading errors
        val dialogState = state.removeDeviceDialogState
        if (!state.isLoadingClientsList && dialogState is RemoveDeviceAuthenticationDialogState.Visible) {
            RemoveDeviceDialog(
                errorState = if (state.error is RemoveDeviceAuthenticationError.InvalidCredentialsError) {
                    RemoveDeviceError.InvalidCredentialsError
                } else {
                    RemoveDeviceError.None
                },
                state = RemoveDeviceDialogState.Visible(
                    device = dialogState.device,
                    loading = dialogState.loading,
                    removeEnabled = dialogState.removeEnabled,
                ),
                passwordTextState = passwordTextState,
                onDialogDismiss = onDialogDismiss,
                onRemoveConfirm = onRemoveConfirm
            )
        }
    }
    val genericError = state.error as? RemoveDeviceAuthenticationError.GenericError
    if (genericError != null) {
        AuthenticationFailureDialog(genericError.failure, onErrorDialogDismiss)
    }
}

@Composable
private fun RemoveDeviceItemsList(
    lazyListState: LazyListState,
    items: List<Device>,
    placeholders: Boolean,
    onItemClicked: (Device) -> Unit
) {
    SurfaceBackgroundWrapper {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(items) { index, device ->
                DeviceItem(
                    device = device,
                    placeholder = placeholders,
                    onClickAction = onItemClicked,
                    shouldShowVerifyLabel = false,
                    icon = {
                        Icon(
                            painterResource(id = R.drawable.ic_remove),
                            stringResource(R.string.content_description_remove_devices_screen_remove_icon)
                        )
                    }
                )
                if (index < items.lastIndex) WireDivider()
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewRemoveDeviceScreen() = WireTheme {
    RemoveDeviceContent(
        state = RemoveDeviceAuthenticationState(
            List(10) { Device() },
            RemoveDeviceAuthenticationDialogState.Hidden,
            isLoadingClientsList = false
        ),
        passwordTextState = TextFieldState(),
        clearSessionState = ClearSessionState(),
        onItemClicked = {},
        onRemoveConfirm = {},
        onDialogDismiss = {},
        onErrorDialogDismiss = {},
        onBackButtonClicked = {},
        onCancelLoginClicked = {},
        onProceedLoginClicked = {}
    )
}
