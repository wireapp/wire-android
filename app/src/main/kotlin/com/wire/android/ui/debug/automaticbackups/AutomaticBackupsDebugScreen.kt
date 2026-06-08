/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.debug.automaticbackups

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.snackbar.collectAndShowSnackbar
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.typography
import com.wire.android.ui.debug.automaticBackupsDebugViewModel
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.rememberCreateFileFlow
import com.wire.kalium.logic.feature.backup.BackupRootKeyInfo

@WireRootDestination
@Composable
fun AutomaticBackupsDebugScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AutomaticBackupsDebugViewModel = automaticBackupsDebugViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LocalSnackbarHostState.current.collectAndShowSnackbar(snackbarFlow = viewModel.infoMessage)
    val pendingExport = state.pendingExportedBackupRootKey
    val createExportFileFlow = rememberCreateFileFlow(
        fileName = pendingExport?.fileName ?: DEFAULT_BACKUP_ROOT_KEY_EXPORT_FILE_NAME,
        fileMimeType = BACKUP_ROOT_KEY_EXPORT_MIME_TYPE,
        onFileCreated = viewModel::saveExportedBackupRootKey,
        onFileCreationCancelled = { viewModel.saveExportedBackupRootKey(null) },
        onPermissionDenied = { viewModel.saveExportedBackupRootKey(null) },
        onPermissionPermanentlyDenied = { viewModel.saveExportedBackupRootKey(null) },
    )

    LaunchedEffect(pendingExport?.fileName) {
        if (pendingExport != null) {
            createExportFileFlow.launch()
        }
    }

    AutomaticBackupsDebugContent(
        state = state,
        exportBackupRootKeyPasswordTextState = viewModel.exportBackupRootKeyPasswordState,
        onNavigationPressed = navigator::navigateBack,
        onFetchBackupRootKey = viewModel::fetchBackupRootKey,
        onGenerateNewKey = viewModel::generateNewBackupRootKey,
        onShowExportBackupRootKeyPasswordDialog = viewModel::showExportBackupRootKeyPasswordDialog,
        onDismissExportBackupRootKeyPasswordDialog = viewModel::dismissExportBackupRootKeyPasswordDialog,
        onExportBackupRootKey = viewModel::exportBackupRootKey,
        onCreateBackup = viewModel::createBackup,
        onRestoreBackup = viewModel::restoreLatestBackup,
        modifier = modifier,
    )
}

@Composable
internal fun AutomaticBackupsDebugContent(
    state: AutomaticBackupsDebugState,
    exportBackupRootKeyPasswordTextState: TextFieldState,
    onNavigationPressed: () -> Unit,
    onFetchBackupRootKey: () -> Unit,
    onGenerateNewKey: () -> Unit,
    onShowExportBackupRootKeyPasswordDialog: () -> Unit,
    onDismissExportBackupRootKeyPasswordDialog: () -> Unit,
    onExportBackupRootKey: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                titleContent = {
                    WireTopAppBarTitle(
                        title = stringResource(R.string.debug_settings_automatic_backups),
                        style = typography().title01,
                        maxLines = 2,
                    )
                },
                navigationIconType = NavigationIconType.Close(R.string.content_description_conversation_details_close_btn),
                onNavigationPressed = onNavigationPressed,
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
            ) {
                SectionHeader(stringResource(R.string.debug_settings_backup_root_key))
                when {
                    state.isLoading -> LoadingState()
                    state.backupRootKey == null -> EmptyKeyState()
                    else -> BackupRootKeyDetails(state.backupRootKey)
                }
                if (!state.isLoading && state.backupRootKey == null) {
                    Spacer(modifier = Modifier.height(dimensions().spacing8x))
                    WirePrimaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensions().spacing16x),
                        onClick = onFetchBackupRootKey,
                        loading = state.isFetchingBackupRootKey,
                        state = if (state.isFetchingBackupRootKey) WireButtonState.Disabled else WireButtonState.Default,
                        text = stringResource(R.string.debug_settings_fetch_backup_root_key),
                    )
                }
                Spacer(modifier = Modifier.height(dimensions().spacing16x))
                WirePrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions().spacing16x),
                    onClick = onShowExportBackupRootKeyPasswordDialog,
                    loading = state.isExportingBackupRootKey,
                    state = if (state.isLoading || state.backupRootKey == null || state.isExportingBackupRootKey) {
                        WireButtonState.Disabled
                    } else {
                        WireButtonState.Default
                    },
                    text = stringResource(R.string.debug_settings_export_backup_root_key),
                )
                Spacer(modifier = Modifier.height(dimensions().spacing8x))
                WirePrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions().spacing16x),
                    onClick = onGenerateNewKey,
                    loading = state.isGenerating,
                    text = stringResource(R.string.debug_settings_generate_backup_root_key),
                )
                Spacer(modifier = Modifier.height(dimensions().spacing8x))
                WirePrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions().spacing16x),
                    onClick = onCreateBackup,
                    loading = state.isCreatingBackup,
                    state = if (state.isCreatingBackup) WireButtonState.Disabled else WireButtonState.Default,
                    text = stringResource(R.string.debug_settings_create_backup),
                )
                if (state.isCreatingBackup) {
                    Text(
                        text = "Progress: ${(state.backupCreationProgress * 100).toInt()}%",
                        modifier = Modifier.padding(dimensions().spacing16x),
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
                Spacer(modifier = Modifier.height(dimensions().spacing8x))
                WirePrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions().spacing16x),
                    onClick = onRestoreBackup,
                    loading = state.isRestoringBackup,
                    state = if (state.isRestoringBackup) WireButtonState.Disabled else WireButtonState.Default,
                    text = stringResource(R.string.debug_settings_restore_backup),
                )
                if (state.isRestoringBackup) {
                    Text(
                        text = "Progress: ${(state.backupRestoreProgress * 100).toInt()}%",
                        modifier = Modifier.padding(dimensions().spacing16x),
                        style = MaterialTheme.wireTypography.body01,
                    )
                }
            }
        },
    )

    if (state.showExportBackupRootKeyPasswordDialog) {
        ExportBackupRootKeyPasswordDialog(
            passwordTextState = exportBackupRootKeyPasswordTextState,
            isExporting = state.isExportingBackupRootKey,
            onDismiss = onDismissExportBackupRootKeyPasswordDialog,
            onExport = onExportBackupRootKey,
        )
    }
}

@Composable
private fun ExportBackupRootKeyPasswordDialog(
    passwordTextState: TextFieldState,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
) {
    WireDialog(
        title = stringResource(R.string.debug_settings_export_backup_root_key_dialog_title),
        text = stringResource(R.string.debug_settings_export_backup_root_key_dialog_message),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            text = stringResource(R.string.label_cancel),
            onClick = onDismiss,
            type = WireDialogButtonType.Secondary,
            state = if (isExporting) WireButtonState.Disabled else WireButtonState.Default,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            text = stringResource(R.string.debug_settings_export_backup_root_key),
            onClick = onExport,
            type = WireDialogButtonType.Primary,
            loading = isExporting,
            state = if (isExporting) WireButtonState.Disabled else WireButtonState.Default,
        ),
    ) {
        WirePasswordTextField(
            textState = passwordTextState,
            labelText = stringResource(R.string.login_password_label),
            state = if (isExporting) WireTextFieldState.Disabled else WireTextFieldState.Default,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensions().spacing16x),
        )
    }
}

@Composable
private fun LoadingState() {
    Text(
        text = stringResource(R.string.location_loading_label),
        modifier = Modifier.padding(dimensions().spacing16x),
        style = MaterialTheme.wireTypography.body01,
    )
}

@Composable
private fun EmptyKeyState() {
    Text(
        text = stringResource(R.string.debug_settings_no_backup_root_key),
        modifier = Modifier.padding(dimensions().spacing16x),
        style = MaterialTheme.wireTypography.body01,
        color = MaterialTheme.wireColorScheme.secondaryText,
    )
}

@Composable
private fun BackupRootKeyDetails(backupRootKey: BackupRootKeyInfo) {
    SettingsItem(
        title = stringResource(R.string.debug_settings_backup_root_key_id),
        text = backupRootKey.id,
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_backup_root_key_fingerprint),
        text = backupRootKey.fingerprint,
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_backup_root_key_created_at),
        text = backupRootKey.createdAt.toString(),
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_backup_root_key_created_by_client),
        text = backupRootKey.createdByClientId.value,
    )
    SettingsItem(
        title = stringResource(R.string.debug_settings_backup_root_key_version),
        text = backupRootKey.version.toString(),
    )
}

private const val DEFAULT_BACKUP_ROOT_KEY_EXPORT_FILE_NAME = "wire-backup-root-key.wbrk"
private const val BACKUP_ROOT_KEY_EXPORT_MIME_TYPE = "application/json"
