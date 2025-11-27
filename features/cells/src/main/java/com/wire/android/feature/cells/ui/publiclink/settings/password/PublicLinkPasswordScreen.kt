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
package com.wire.android.feature.cells.ui.publiclink.settings.password

import android.content.ClipData
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.publiclink.PublicLinkErrorDialog
import com.wire.android.feature.cells.ui.publiclink.settings.RemovePasswordDialog
import com.wire.android.feature.cells.ui.util.PreviewMultipleThemes
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme

@WireDestination(
    navArgsDelegate = PublicLinkPasswordNavArgs::class,
)
@Composable
internal fun PublicLinkPasswordScreen(
    resultNavigator: ResultBackNavigator<Boolean>,
    modifier: Modifier = Modifier,
    viewModel: PublicLinkPasswordScreenViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var showRemoveConfirmationDialog by remember { mutableStateOf(false) }
    var showMissingPasswordDialog by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<PasswordError?>(null) }

    BackHandler {
        resultNavigator.navigateBack(viewModel.isPasswordCreated)
    }

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = {
                    resultNavigator.navigateBack(viewModel.isPasswordCreated)
                },
                title = stringResource(R.string.public_link_setting_password_title),
                navigationIconType = NavigationIconType.Back(),
                elevation = dimensions().spacing0x
            )
        }
    ) { innerPadding ->

        PasswordScreenContent(
            state = state,
            passwordTextState = viewModel.passwordTextState,
            onEnableClick = viewModel::onEnableClick,
            onSetPasswordClick = viewModel::setPassword,
            onResetPasswordClick = viewModel::resetPassword,
            onGeneratePasswordClick = viewModel::generatePassword,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showMissingPasswordDialog) {
        PasswordNotAvailableDialog(
            onResetPassword = {
                showMissingPasswordDialog = false
                viewModel.resetPassword()
            },
            onDismiss = { showMissingPasswordDialog = false },
        )
    }

    if (showRemoveConfirmationDialog) {
        RemovePasswordDialog(
            onResult = { confirmed ->
                showRemoveConfirmationDialog = false
                viewModel.onConfirmPasswordRemoval(confirmed)
            },
        )
    }

    passwordError?.let { error ->
        PublicLinkErrorDialog(
            title = error.title?.let { stringResource(it) },
            message = error.message?.let { stringResource(it) },
            onResult = { tryAgain ->
                passwordError = null
                if (tryAgain) viewModel.retryError(error)
            }
        )
    }

    HandleActions(viewModel.actions) { action ->
        when (action) {
            is CopyPasswordAndClose -> {
                copyPassword(clipboardManager, action.password)
                resultNavigator.navigateBack(true)
            }
            ShowMissingPasswordDialog -> showMissingPasswordDialog = true
            ShowRemoveConfirmationDialog -> showRemoveConfirmationDialog = true
            is ShowPasswordError -> passwordError = action.error
        }
    }
}

@Composable
private fun PasswordScreenContent(
    state: PublicLinkPasswordScreenViewState,
    passwordTextState: TextFieldState,
    modifier: Modifier = Modifier,
    onEnableClick: () -> Unit = {},
    onSetPasswordClick: () -> Unit = {},
    onResetPasswordClick: () -> Unit = {},
    onGeneratePasswordClick: () -> Unit = {},
) {

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier,
    ) {

        EnablePasswordSection(
            checked = state.isEnabled,
            onCheckClick = onEnableClick,
        )

        AnimatedVisibility(
            visible = state.isEnabled,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            PasswordSettingsContent(
                screenState = state.screenState,
                isPasswordValid = state.isPasswordValid,
                showProgress = state.isUpdating,
                passwordTextState = passwordTextState,
                onGeneratePassword = onGeneratePasswordClick,
                onCopyPassword = {
                    copyPassword(clipboardManager, passwordTextState.text.toString())
                },
                onSetPassword = onSetPasswordClick,
                onResetPassword = onResetPasswordClick,
            )
        }
    }
}

private fun copyPassword(clipboardManager: ClipboardManager, password: String) {
    val clipData = ClipData.newPlainText("password", password).apply {
        description.extras = PersistableBundle().apply {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
    }
    clipboardManager.setClip(ClipEntry(clipData))
}

@Composable
private fun PasswordSettingsContent(
    screenState: PasswordScreenState,
    isPasswordValid: Boolean,
    showProgress: Boolean,
    passwordTextState: TextFieldState,
    onGeneratePassword: () -> Unit,
    onCopyPassword: () -> Unit,
    onSetPassword: () -> Unit,
    onResetPassword: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensions().spacing16x)
    ) {

        when (screenState) {
            PasswordScreenState.INITIAL -> {}

            PasswordScreenState.SETUP_PASSWORD ->
                PasswordSetupView(
                    showProgress = showProgress,
                    isPasswordValid = isPasswordValid,
                    passwordTextState = passwordTextState,
                    onGeneratePassword = onGeneratePassword,
                    onSetPassword = onSetPassword,
                    onCopyPassword = onCopyPassword,
                )
            PasswordScreenState.AVAILABLE ->
                PasswordActionsView(
                    onResetPassword = onResetPassword,
                    onCopyPassword = onCopyPassword,
                )
            PasswordScreenState.NOT_AVAILABLE -> {
                PasswordActionsView(
                    isCopyActionEnabled = false,
                    onResetPassword = onResetPassword,
                    onCopyPassword = onCopyPassword,
                )
            }
        }
    }
}

@Composable
private fun EnablePasswordSection(
    checked: Boolean,
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
                    .weight(1f),
                text = stringResource(R.string.public_link_setting_password_title),
                style = typography().body02
            )
            WireSwitch(
                checked = checked,
                onCheckedChange = { onCheckClick() },
            )
        }
        VerticalSpace.x16()
        Text(
            text = stringResource(R.string.public_link_setting_password_description),
            style = typography().body01
        )
    }
}

data class PublicLinkPasswordNavArgs(
    val linkUuid: String,
    val passwordEnabled: Boolean,
)

@PreviewMultipleThemes
@Composable
private fun PreviewPasswordScreen() {
    WireTheme {
        PasswordScreenContent(
            state = PublicLinkPasswordScreenViewState(
                isEnabled = true,
                isPasswordValid = true,
                isUpdating = false,
            ),
            passwordTextState = TextFieldState("password"),
        )
    }
}
