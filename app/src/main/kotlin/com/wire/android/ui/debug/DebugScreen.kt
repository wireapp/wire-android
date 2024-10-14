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

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.MigrationScreenDestination
import com.wire.android.util.AppNameUtil
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.getMimeType
import com.wire.android.util.getUrisOfFilesInDirectory
import com.wire.android.util.multipleFileSharingIntent
import com.wire.kalium.logic.data.user.UserId
import java.io.File

@RootNavGraph
@WireDestination
@Composable
fun DebugScreen(navigator: Navigator, userDebugViewModel: UserDebugViewModel = hiltViewModel()) {
    UserDebugContent(
        onNavigationPressed = navigator::navigateBack,
        onManualMigrationPressed = {
            navigator.navigate(
                NavigationCommand(
                    MigrationScreenDestination(it),
                    BackStackMode.CLEAR_WHOLE
                )
            )
        },
        state = userDebugViewModel.state,
        onLoggingEnabledChange = userDebugViewModel::setLoggingEnabledState,
        onDeleteLogs = userDebugViewModel::deleteLogs
    )
}

@Composable
internal fun UserDebugContent(
    state: UserDebugState,
    onNavigationPressed: () -> Unit,
    onManualMigrationPressed: (currentAccount: UserId) -> Unit,
    onLoggingEnabledChange: (Boolean) -> Unit,
    onDeleteLogs: () -> Unit,
) {
    val debugContentState: DebugContentState = rememberDebugContentState(state.logPath)

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.label_debug_title),
                elevation = dimensions().spacing0x,
                navigationIconType = NavigationIconType.Back,
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
                    onShareLogs = debugContentState::shareLogs,
                )
                DebugDataOptions(
                    appVersion = AppNameUtil.createAppName(),
                    buildVariant = "${BuildConfig.FLAVOR}${BuildConfig.BUILD_TYPE.replaceFirstChar { it.uppercase() }}",
                    onCopyText = debugContentState::copyToClipboard,
                    onManualMigrationPressed = onManualMigrationPressed
                )
            }
        }
    }
}

@Composable
fun rememberDebugContentState(logPath: String): DebugContentState {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    return remember {
        DebugContentState(
            context,
            clipboardManager,
            logPath,
            scrollState
        )
    }
}

data class DebugContentState(
    val context: Context,
    val clipboardManager: ClipboardManager,
    val logPath: String,
    val scrollState: ScrollState
) {
    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(
            context,
            context.getText(R.string.label_text_copied),
            Toast.LENGTH_SHORT
        ).show()
    }

    fun shareLogs() {
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

@Preview(heightDp = 1400)
@Composable
internal fun PreviewUserDebugContent() = WireTheme {
    UserDebugContent(
        state = UserDebugState(
            isLoggingEnabled = true,
            logPath = "/data/user/0/com.wire.android/files/logs"
        ),
        onNavigationPressed = {},
        onManualMigrationPressed = {},
        onLoggingEnabledChange = {},
        onDeleteLogs = {}
    )
}
