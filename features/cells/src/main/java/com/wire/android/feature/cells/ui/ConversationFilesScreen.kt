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
package com.wire.android.feature.cells.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
import com.wire.android.navigation.LocalNavigator
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

/**
 * Show files in one conversation.
 * Conversation id is passed to view model via navigation parameters [CellFilesNavArgs].
 */
@Destination(
    style = PopUpNavigationAnimation::class,
    navArgsDelegate = CellFilesNavArgs::class,
)
@Composable
fun ConversationFilesScreen(
    modifier: Modifier = Modifier,
    viewModel: CellViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.current

    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = { navigator.navigateBack() },
                title = stringResource(R.string.conversation_files_title),
                navigationIconType = NavigationIconType.Close(),
                elevation = dimensions().spacing0x
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            CellScreenContent(
                actionsFlow = viewModel.actions,
                viewState = state,
                sendIntent = { viewModel.sendIntent(it) },
                downloadFileState = viewModel.downloadFile,
                fileMenuState = viewModel.menu,
                isAllFiles = false,
                showPublicLinkScreen = { assetId, fileName, linkId ->
                    navigator.navigate(
                        NavigationCommand(
                            PublicLinkScreenDestination(
                                assetId = assetId,
                                fileName = fileName,
                                publicLinkId = linkId
                            )
                        )
                    )
                },
            )
        }
    }
}
