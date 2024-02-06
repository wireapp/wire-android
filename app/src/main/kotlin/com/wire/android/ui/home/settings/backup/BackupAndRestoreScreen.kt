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

package com.wire.android.ui.home.settings.backup

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.wire.android.ui.common.scaffold.WireScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.home.settings.backup.dialog.create.CreateBackupDialogFlow
import com.wire.android.ui.home.settings.backup.dialog.restore.RestoreBackupDialogFlow
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.PermissionDenialType

@RootNavGraph
@Destination
@Composable
fun BackupAndRestoreScreen(
    navigator: Navigator,
    viewModel: BackupAndRestoreViewModel = hiltViewModel()
) {
    BackupAndRestoreContent(
        backUpAndRestoreState = viewModel.state,
        onValidateBackupPassword = viewModel::validateBackupCreationPassword,
        onCreateBackup = viewModel::createBackup,
        onSaveBackup = viewModel::saveBackup,
        onShareBackup = viewModel::shareBackup,
        onChooseBackupFile = viewModel::chooseBackupFileToRestore,
        onRestoreBackup = viewModel::restorePasswordProtectedBackup,
        onCancelBackupRestore = viewModel::cancelBackupRestore,
        onCancelBackupCreation = viewModel::cancelBackupCreation,
        onOpenConversations = { navigator.navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE)) },
        onBackPressed = navigator::navigateBack
    )
}

@Composable
fun BackupAndRestoreContent(
    backUpAndRestoreState: BackupAndRestoreState,
    onValidateBackupPassword: (TextFieldValue) -> Unit,
    onCreateBackup: (String) -> Unit,
    onSaveBackup: (Uri) -> Unit,
    onShareBackup: () -> Unit,
    onCancelBackupCreation: () -> Unit,
    onCancelBackupRestore: () -> Unit,
    onChooseBackupFile: (Uri) -> Unit,
    onRestoreBackup: (String) -> Unit,
    onOpenConversations: () -> Unit,
    onBackPressed: () -> Unit
) {
    val backupAndRestoreStateHolder = rememberBackUpAndRestoreStateHolder()
    WireScaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = 0.dp,
            title = stringResource(id = R.string.backup_and_restore_screen_title),
            onNavigationPressed = onBackPressed
        )
    }) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxHeight()
                .padding(internalPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.settings_backup_info),
                    style = MaterialTheme.wireTypography.body01,
                    color = MaterialTheme.wireColorScheme.onBackground,
                    modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)
                )
            }
            Surface(
                color = MaterialTheme.wireColorScheme.background,
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)
                ) {
                    WirePrimaryButton(
                        text = stringResource(id = R.string.settings_backup_create),
                        onClick = backupAndRestoreStateHolder::showBackupDialog,
                        modifier = Modifier.fillMaxWidth()
                    )
                    VerticalSpace.x8()
                    WirePrimaryButton(
                        text = stringResource(id = R.string.settings_backup_restore),
                        onClick = backupAndRestoreStateHolder::showRestoreDialog,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    when (backupAndRestoreStateHolder.dialogState) {
        is BackupAndRestoreDialog.CreateBackup -> {
            CreateBackupDialogFlow(
                backUpAndRestoreState = backUpAndRestoreState,
                onValidateBackupPassword = onValidateBackupPassword,
                onCreateBackup = onCreateBackup,
                onSaveBackup = onSaveBackup,
                onShareBackup = onShareBackup,
                onCancelCreateBackup = {
                    backupAndRestoreStateHolder.dismissDialog()
                    onCancelBackupCreation()
                },
                onPermissionPermanentlyDenied = {
                    if (it == PermissionDenialType.WriteFile) {
                        backupAndRestoreStateHolder.showPermissionPermanentlyDeniedDialog(
                            R.string.app_permission_dialog_title,
                            R.string.save_backup_file_permission_dialog_description
                        )
                    }
                }
            )
        }

        is BackupAndRestoreDialog.RestoreBackup -> {
            RestoreBackupDialogFlow(
                backUpAndRestoreState = backUpAndRestoreState,
                onChooseBackupFile = onChooseBackupFile,
                onRestoreBackup = onRestoreBackup,
                onCancelBackupRestore = {
                    backupAndRestoreStateHolder.dismissDialog()
                    onCancelBackupRestore()
                },
                onOpenConversations = onOpenConversations,
                onPermissionPermanentlyDenied = {
                    if (it == PermissionDenialType.ReadFile) {
                        backupAndRestoreStateHolder.showPermissionPermanentlyDeniedDialog(
                            R.string.app_permission_dialog_title,
                            R.string.restore_backup_permission_dialog_description
                        )
                    }
                }
            )
        }

        BackupAndRestoreDialog.None -> {}
    }

    PermissionPermanentlyDeniedDialog(
        dialogState = backupAndRestoreStateHolder.permissionPermanentlyDeniedDialogState,
        hideDialog = backupAndRestoreStateHolder::hidePermissionPermanentlyDeniedDialog
    )
}

@Preview
@Composable
fun PreviewBackupAndRestoreScreen() {
    BackupAndRestoreContent(
        backUpAndRestoreState = BackupAndRestoreState.INITIAL_STATE,
        onValidateBackupPassword = {},
        onCreateBackup = {},
        onSaveBackup = {},
        onShareBackup = {},
        onCancelBackupCreation = {},
        onCancelBackupRestore = {},
        onChooseBackupFile = {},
        onRestoreBackup = {},
        onOpenConversations = {},
        onBackPressed = {}
    )
}
