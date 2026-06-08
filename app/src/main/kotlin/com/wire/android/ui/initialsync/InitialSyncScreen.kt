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

package com.wire.android.ui.initialsync

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.lifecycleScope
import com.ramcosta.composedestinations.generated.app.destinations.HomeScreenDestination
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.getBaseRoute
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.SettingUpWireScreenContent
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireLinearProgressIndicator
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.initialSyncViewModel
import com.wire.android.util.permission.FileType
import com.wire.android.util.permission.rememberChooseSingleFileFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@WireRootDestination
@Composable
fun InitialSyncScreen(
    navigator: Navigator,
    viewModel: InitialSyncViewModel = initialSyncViewModel()
) {
    val activity = LocalActivity.current
    val syncCompletionState = viewModel.syncCompletionState
    val chooseImportFileFlow = rememberChooseSingleFileFlow(
        fileType = FileType.Any,
        onFileBrowserItemPicked = viewModel::onBackupRootKeyImportFileSelected,
        onPermissionDenied = { viewModel.onBackupRootKeyImportFileSelected(null) },
        onPermissionPermanentlyDenied = { viewModel.onBackupRootKeyImportFileSelected(null) },
    )

    SettingUpWireScreenContent {
        InitialSyncBackupRestoreStatus(viewModel.backupRestoreState)
    }

    if (viewModel.showBackupRootKeyUnavailableDialog) {
        WireDialog(
            title = stringResource(R.string.initial_sync_backup_root_key_unavailable_dialog_title),
            text = stringResource(R.string.initial_sync_backup_root_key_unavailable_dialog_message),
            onDismiss = viewModel::onBackupRootKeyDialogCancel,
            dismissButtonProperties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = viewModel::onBackupRootKeyDialogCancel,
                type = WireDialogButtonType.Secondary,
            ),
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.initial_sync_backup_root_key_unavailable_dialog_try_again),
                onClick = viewModel::onBackupRootKeyDialogTryAgain,
                type = WireDialogButtonType.Primary,
            ),
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.initial_sync_backup_root_key_unavailable_dialog_import_key),
                onClick = chooseImportFileFlow::launch,
                type = WireDialogButtonType.Secondary,
            ),
            buttonsHorizontalAlignment = false,
        )
    }

    if (viewModel.showImportBackupRootKeyPasswordDialog) {
        WireDialog(
            title = stringResource(R.string.initial_sync_import_backup_root_key_dialog_title),
            text = stringResource(R.string.initial_sync_import_backup_root_key_dialog_message),
            onDismiss = viewModel::onImportBackupRootKeyPasswordDialogDismiss,
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                onClick = viewModel::onImportBackupRootKeyPasswordDialogDismiss,
                type = WireDialogButtonType.Secondary,
                state = if (viewModel.isImportingBackupRootKey) WireButtonState.Disabled else WireButtonState.Default,
            ),
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.initial_sync_backup_root_key_unavailable_dialog_import_key),
                onClick = viewModel::onImportBackupRootKey,
                type = WireDialogButtonType.Primary,
                loading = viewModel.isImportingBackupRootKey,
                state = if (viewModel.isImportingBackupRootKey) WireButtonState.Disabled else WireButtonState.Default,
            ),
        ) {
            WirePasswordTextField(
                textState = viewModel.importBackupRootKeyPasswordState,
                labelText = stringResource(R.string.login_password_label),
                state = if (viewModel.isImportingBackupRootKey) WireTextFieldState.Disabled else WireTextFieldState.Default,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions().spacing16x),
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.restoreErrorToast.collect { messageResId ->
            Toast.makeText(activity, activity.getString(messageResId), Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(syncCompletionState) {
        syncCompletionState ?: return@LaunchedEffect

        if (syncCompletionState.shouldMoveToBackground) {
            activity.lifecycleScope.launch {
                navigator.navController.currentBackStackEntryFlow
                    .map { it.destination.route?.getBaseRoute() }
                    .first { it == HomeScreenDestination.baseRoute }
                activity.moveTaskToBack(false)
            }
        }

        navigator.navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
    }
}

@Composable
private fun InitialSyncBackupRestoreStatus(
    state: InitialSyncBackupRestoreState,
    modifier: Modifier = Modifier,
) {
    if (state is InitialSyncBackupRestoreState.None) {
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = MaterialTheme.wireDimensions.topBarShadowElevation,
        color = MaterialTheme.wireColorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.wireDimensions.spacing16x)
        ) {
            Text(
                text = when (state) {
                    InitialSyncBackupRestoreState.Checking -> stringResource(R.string.initial_sync_checking_backup)
                    InitialSyncBackupRestoreState.None -> ""
                    is InitialSyncBackupRestoreState.Restoring -> stringResource(R.string.backup_dialog_restoring_backup_title)
                },
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state is InitialSyncBackupRestoreState.Restoring) {
                val progress by animateFloatAsState(targetValue = state.progress)
                WireLinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.wireDimensions.spacing16x),
                )
            }
        }
    }
}
