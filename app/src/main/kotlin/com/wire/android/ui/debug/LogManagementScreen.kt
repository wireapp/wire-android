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

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@WireRootDestination
@Composable
fun LogManagementScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: LogManagementViewModel = hiltViewModel()
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
                onNavigationPressed = navigator::navigateBack
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
                onShareLogs = { contentState.shareLogs(viewModel::flushLogs) },
                isDBLoggerEnabled = false,
                onDBLoggerEnabledChange = {},
                isPrivateBuild = false
            )
        }
    }
}
