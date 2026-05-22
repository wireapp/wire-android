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
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.ramcosta.composedestinations.generated.app.destinations.ConversationCryptoStatsScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.DebugFeatureFlagsScreenDestination
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.home.settings.backup.BackupAndRestoreDialog
import com.wire.android.ui.home.settings.backup.rememberBackUpAndRestoreStateHolder
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.AppNameUtil
import com.wire.android.util.logging.LogShareLauncher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.io.File

@WireRootDestination
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
        onShowFeatureFlags = {
            navigator.navigate(NavigationCommand(DebugFeatureFlagsScreenDestination))
        },
        onShowCryptoStats = {
            navigator.navigate(NavigationCommand(ConversationCryptoStatsScreenDestination))
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
    onShowFeatureFlags: () -> Unit,
    onShowCryptoStats: () -> Unit,
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
                    onShowCryptoStats = onShowCryptoStats,
                )
                if (BuildConfig.PRIVATE_BUILD) {
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
        hiltViewModelScoped<ExportObfuscatedCopyViewModelImpl, ExportObfuscatedCopyViewModel>()
) {

    Column(modifier = modifier) {
        SectionHeader("Danger Zone DO NOT TOUCH")
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
    val shareLogsFailureMessage = stringResource(R.string.label_share_logs_failed)
    val logShareLauncher = remember(context, coroutineScope, shareLogsFailureMessage) {
        LogShareLauncher(
            context = context,
            coroutineScope = coroutineScope,
            onFailure = {
                Toast.makeText(context, shareLogsFailureMessage, Toast.LENGTH_SHORT).show()
            }
        )
    }

    return remember(context, clipboardManager, logPath, scrollState, logShareLauncher) {
        DebugContentState(
            context,
            clipboardManager,
            logPath,
            scrollState,
            logShareLauncher
        )
    }
}

data class DebugContentState(
    val context: Context,
    val clipboardManager: ClipboardManager,
    val logPath: String,
    val scrollState: ScrollState,
    val logShareLauncher: LogShareLauncher
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
        val dir = File(logPath).parentFile
        if (dir != null && dir.exists()) {
            logShareLauncher.shareLogs(dir) {
                // Flush any buffered logs before sharing to ensure completeness.
                onFlushLogs().await()
            }
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
        onShowFeatureFlags = {},
        onShowCryptoStats = {},
    )
}
