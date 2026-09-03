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
package com.wire.android.ui.debug

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@Composable
internal fun LogManagementRouteScreen(
    onBack: () -> Unit,
    onShareLogsViaWire: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogManagementViewModel = logManagementViewModel(),
) {
    val state = viewModel.state
    val contentState = rememberDebugContentState(state.logPath)

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.label_logs_option_title),
                elevation = dimensions().spacing0x,
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onBack
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(contentState.scrollState)
                .padding(internalPadding)
        ) {
            LogOptions(
                isLoggingEnabled = state.isLoggingEnabled,
                onLoggingEnabledChange = viewModel::setLoggingEnabledState,
                onDeleteLogs = viewModel::deleteLogs,
                onShareLogsExternally = { contentState.shareLogsExternally(viewModel::flushLogs) },
                onShareLogsViaWire = { contentState.shareLogsViaWire(viewModel::flushLogs, onShareLogsViaWire) },
                isDBLoggerEnabled = false,
                onDBLoggerEnabledChange = {},
                isPrivateBuild = false
            )
        }
    }
}
