/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.publiclink

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.destinations.PublicLinkExpirationScreenDestination
import com.wire.android.feature.cells.ui.destinations.PublicLinkPasswordScreenDestination
import com.wire.android.feature.cells.ui.publiclink.settings.PublicLinkSettingsSection
import com.wire.android.feature.cells.ui.publiclink.settings.RemovePublicLinkDialog
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@WireDestination(
    navArgsDelegate = PublicLinkNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun PublicLinkScreen(
    navigator: WireNavigator,
    resultNavigator: ResultBackNavigator<Unit>,
    onPasswordChange: ResultRecipient<PublicLinkPasswordScreenDestination, Boolean>,
    onExpirationChange: ResultRecipient<PublicLinkExpirationScreenDestination, Boolean>,
    modifier: Modifier = Modifier,
    viewModel: PublicLinkViewModel = hiltViewModel(),
) {

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by viewModel.state.collectAsState()

    var showRemoveConfirmationDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<PublicLinkError?>(null) }

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = { resultNavigator.navigateBack() },
                title = if (state.isFolder) {
                    stringResource(R.string.share_folder_via_link)
                } else {
                    stringResource(R.string.share_file_via_link)
                },
                navigationIconType = NavigationIconType.Close(),
                elevation = dimensions().spacing0x
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            EnableLinkSection(
                checked = state.isEnabled,
                isFolder = state.isFolder,
                onCheckClick = {
                    viewModel.onEnabledClick()
                }
            )

            AnimatedVisibility(
                visible = state.isLinkAvailable,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    state.settings?.let {
                        PublicLinkSettingsSection(
                            settings = it,
                            onPasswordClick = viewModel::onPasswordClick,
                            onExpirationClick = {
                                navigator.navigate(NavigationCommand(PublicLinkExpirationScreenDestination()))
                            },
                        )
                    }

                    PublicLinkSection(
                        state = state.linkState,
                        onShareLink = viewModel::shareLink,
                        onCopyLink = viewModel::copyLink,
                    )
                }
            }
        }
    }

    if (showRemoveConfirmationDialog) {
        RemovePublicLinkDialog(
            onResult = { confirmed ->
                showRemoveConfirmationDialog = false
                viewModel.onConfirmRemoval(confirmed)
            }
        )
    }

    showErrorDialog?.let { error ->
        PublicLinkErrorDialog(
            onResult = { tryAgain ->
                showErrorDialog = null
                if (tryAgain) viewModel.retryError(error)
            }
        )
    }

    HandleActions(viewModel.actions) { action ->
        when (action) {
            is ShowError -> {

                Toast.makeText(context, action.message, Toast.LENGTH_SHORT).show()

                if (action.closeScreen) {
                    resultNavigator.navigateBack()
                }
            }

            is CopyLink -> {
                clipboardManager.setText(AnnotatedString(action.url))
                showLinkCopiedToast(context)
            }

            is OpenPasswordSettings ->
                navigator.navigate(
                    NavigationCommand(
                        PublicLinkPasswordScreenDestination(
                            linkUuid = action.linkUuid,
                            passwordEnabled = action.isPasswordEnabled,
                        )
                    )
                )

            ShowRemoveConfirmation -> showRemoveConfirmationDialog = true

            is ShowErrorDialog -> showErrorDialog = action.error
        }
    }

    onPasswordChange.handleNavResult { result ->
        viewModel.onPasswordUpdate(result)
    }

    onExpirationChange.handleNavResult { result ->
        if (result) {
            viewModel.onExpirationUpdate()
        }
    }
}

@Composable
private fun EnableLinkSection(
    checked: Boolean,
    isFolder: Boolean,
    onCheckClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorsScheme().surface)
            .clickable { onCheckClick() }
            .padding(dimensions().spacing16x)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                text = stringResource(R.string.create_public_link),
                style = typography().body02
            )
            WireSwitch(
                checked = checked,
                onCheckedChange = {
                    onCheckClick()
                }
            )
        }
        Spacer(
            modifier = Modifier.height(dimensions().spacing16x)
        )
        Text(
            text = if (isFolder) {
                stringResource(R.string.public_link_message_folder)
            } else {
                stringResource(R.string.public_link_message_file)
            },
            style = typography().body01
        )
    }
}

/**
 * Show a toast message when the link is copied to the clipboard.
 * Only for API levels lower than 33. On new versions, the system will show a clipboard
 * editor overlay.
 */
private fun showLinkCopiedToast(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.copied_to_clipboard_message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun <D : DestinationSpec<*>, R> ResultRecipient<D, R>.handleNavResult(block: (R) -> Unit) {
    onNavResult { result ->
        when (result) {
            is NavResult.Value<R> -> block(result.value)
            NavResult.Canceled -> {}
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewCreatePublicLinkScreen() {
    WireTheme {
        Column {
            EnableLinkSection(
                checked = true,
                isFolder = false,
                onCheckClick = {}
            )
            PublicLinkSection(
                state = PublicLinkState.READY,
                onShareLink = {},
                onCopyLink = {}
            )
        }
    }
}
