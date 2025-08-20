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

package com.wire.android.ui.debug

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.model.Clickable
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.DebugFeatureFlagsScreenDestination
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.home.settings.backup.BackupAndRestoreDialog
import com.wire.android.ui.home.settings.backup.rememberBackUpAndRestoreStateHolder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.AppNameUtil
import com.wire.android.util.getMimeType
import com.wire.android.util.getUrisOfFilesInDirectory
import com.wire.android.util.multipleFileSharingIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import java.io.File

@WireDestination
@Composable
fun DebugScreen(
    navigator: Navigator,
    userDebugViewModel: UserDebugViewModel = hiltViewModel(),
) {
    UserDebugContent(
        onNavigationPressed = navigator::navigateBack,
        state = userDebugViewModel.state,
        onLoggingEnabledChange = userDebugViewModel::setLoggingEnabledState,
        onDeleteLogs = userDebugViewModel::deleteLogs,
        onFlushLogs = userDebugViewModel::flushLogs,
        onDatabaseLoggerEnabledChanged = userDebugViewModel::setDatabaseLoggerEnabledState,
        onEnableWireCellsFeature = userDebugViewModel::enableWireCellsFeature,
        onShowFeatureFlags = {
            navigator.navigate(NavigationCommand(DebugFeatureFlagsScreenDestination))
        }
    )
}

@Composable
internal fun UserDebugContent(
    state: UserDebugState,
    onNavigationPressed: () -> Unit,
    onLoggingEnabledChange: (Boolean) -> Unit,
    onDatabaseLoggerEnabledChanged: (Boolean) -> Unit,
    onDeleteLogs: () -> Unit,
    onFlushLogs: () -> Deferred<Unit>,
    onEnableWireCellsFeature: (Boolean) -> Unit,
    onShowFeatureFlags: () -> Unit,
) {
    val debugContentState: DebugContentState = rememberDebugContentState(state.logPath)

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.label_debug_title),
                elevation = dimensions().spacing0x,
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = onNavigationPressed
            )
        }
    ) { internalPadding ->
        with(state) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(debugContentState.scrollState)
                    .padding(internalPadding)
            ) {
                LogOptions(
                    isLoggingEnabled = isLoggingEnabled,
                    onLoggingEnabledChange = onLoggingEnabledChange,
                    onDeleteLogs = onDeleteLogs,
                    onShareLogs = { debugContentState.shareLogs(onFlushLogs) },
                    isDBLoggerEnabled = state.isDBLoggingEnabled,
                    onDBLoggerEnabledChange = onDatabaseLoggerEnabledChanged,
                    isPrivateBuild = BuildConfig.PRIVATE_BUILD,
                )
                DebugDataOptions(
                    appVersion = AppNameUtil.createAppName(),
                    buildVariant = "${BuildConfig.FLAVOR}${BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercase() }}",
                    onCopyText = debugContentState::copyToClipboard,
                    onShowFeatureFlags = onShowFeatureFlags,
                )
                if (BuildConfig.PRIVATE_BUILD) {
                    DebugWireCellOptions(
                        isCellFeatureEnabled = isWireCellFeatureEnabled,
                        onCheckedChange = onEnableWireCellsFeature,
                    )
                    DangerOptions()
                }
            }
        }
    }
}

@Composable
fun DangerOptions(
    modifier: Modifier = Modifier,
    exportObfuscatedCopyViewModel: ExportObfuscatedCopyViewModel =
        hiltViewModelScoped<ExportObfuscatedCopyViewModelImpl, ExportObfuscatedCopyViewModel, ExportObfuscatedCopyArgs>(
            ExportObfuscatedCopyArgs
        ),
) {

    Column(modifier = modifier) {
        FolderHeader("Danger Zone DO NOT TOUCH")
        @SuppressLint("ComposeViewModelInjection")
        val backupAndRestoreStateHolder = rememberBackUpAndRestoreStateHolder()

        SettingsItem(
            text = "Create Obfuscated Database Copy",
            onRowPressed = Clickable(enabled = true, onClick = backupAndRestoreStateHolder::showBackupDialog),
            modifier = Modifier.background(Color.Red)
        )
        when (backupAndRestoreStateHolder.dialogState) {
            BackupAndRestoreDialog.CreateBackup -> {
                CreateObfuscatedCopyFlow(
                    backUpAndRestoreState = exportObfuscatedCopyViewModel.state,
                    backupPasswordTextState = exportObfuscatedCopyViewModel.createBackupPasswordState,
                    onCreateBackup = exportObfuscatedCopyViewModel::createObfuscatedCopy,
                    onSaveBackup = exportObfuscatedCopyViewModel::saveCopy,
                    onShareBackup = exportObfuscatedCopyViewModel::shareCopy,
                    onCancelCreateBackup = {
                        backupAndRestoreStateHolder.dismissDialog()
                        exportObfuscatedCopyViewModel.cancelBackupCreation()
                    },
                    onPermissionPermanentlyDenied = {}
                )
            }

            BackupAndRestoreDialog.None -> {
                /*no-op*/
            }

            BackupAndRestoreDialog.RestoreBackup -> TODO("Restore backup not implemented")
        }
    }
}

@Composable
fun rememberDebugContentState(logPath: String): DebugContentState {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    return remember {
        DebugContentState(
            context,
            clipboardManager,
            logPath,
            scrollState,
            coroutineScope
        )
    }
}

data class DebugContentState(
    val context: Context,
    val clipboardManager: ClipboardManager,
    val logPath: String,
    val scrollState: ScrollState,
    val coroutineScope: CoroutineScope
) {
    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(
            context,
            context.getText(R.string.label_text_copied),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun shareLogs(onFlushLogs: () -> Deferred<Unit>) {
        coroutineScope.launch {
            // Flush any buffered logs before sharing to ensure completeness
            onFlushLogs().await()
            val dir = File(logPath).parentFile
            val fileUris =
                if (dir != null && dir.exists()) context.getUrisOfFilesInDirectory(dir) else arrayListOf()
            val intent = context.multipleFileSharingIntent(fileUris)
            // The first log file is simply text, not compressed. Get its mime type separately
            // and set it as the mime type for the intent.
            intent.type = fileUris.firstOrNull()?.getMimeType(context) ?: "text/plain"
            // Get all other mime types and add them
            val mimeTypes = fileUris.drop(1).mapNotNull { it.getMimeType(context) }
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toSet().toTypedArray())
            context.startActivity(intent)
        }
    }
}

@Preview(heightDp = 1400)
@Composable
internal fun PreviewUserDebugContent() = WireTheme {
    UserDebugContent(
        state = UserDebugState(
            isLoggingEnabled = true,
            logPath = "/data/user/0/com.wire.android/files/logs"
        ),
        onNavigationPressed = {},
        onLoggingEnabledChange = {},
        onDeleteLogs = {},
        onFlushLogs = { CompletableDeferred(Unit) },
        onDatabaseLoggerEnabledChanged = {},
        onEnableWireCellsFeature = {},
        onShowFeatureFlags = {},
    )
}
