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
import com.wire.android.ui.common.scaffold.WireScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.MigrationScreenDestination
import com.wire.android.util.getMimeType
import com.wire.android.util.getUrisOfFilesInDirectory
import com.wire.android.util.multipleFileSharingIntent
import com.wire.kalium.logic.data.user.UserId
import java.io.File

@RootNavGraph
@Destination
@Composable
fun DebugScreen(navigator: Navigator) {
    UserDebugContent(
        onNavigationPressed = navigator::navigateBack,
        onManualMigrationPressed = {
            navigator.navigate(NavigationCommand(MigrationScreenDestination(it), BackStackMode.CLEAR_WHOLE))
        }
    )
}

@Composable
private fun UserDebugContent(
    onNavigationPressed: () -> Unit,
    onManualMigrationPressed: (currentAccount: UserId) -> Unit,
) {

    val userDebugViewModel: UserDebugViewModel = hiltViewModel()
    val debugContentState: DebugContentState = rememberDebugContentState(userDebugViewModel.logPath)

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.label_debug_title),
                elevation = 0.dp,
                navigationIconType = NavigationIconType.Back,
                onNavigationPressed = onNavigationPressed
            )
        }
    ) { internalPadding ->
        with(userDebugViewModel.state) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(debugContentState.scrollState)
                    .padding(internalPadding)
            ) {
                LogOptions(
                    isLoggingEnabled = isLoggingEnabled,
                    onLoggingEnabledChange = userDebugViewModel::setLoggingEnabledState,
                    onDeleteLogs = userDebugViewModel::deleteLogs,
                    onShareLogs = debugContentState::shareLogs,
                )
                DebugDataOptions(
                    appVersion = BuildConfig.VERSION_NAME,
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
        val fileUris = context.getUrisOfFilesInDirectory(dir)
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
