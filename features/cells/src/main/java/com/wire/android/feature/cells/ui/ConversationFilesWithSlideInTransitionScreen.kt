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

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.topappbar.NavigationIconType

@Destination(
    style = SlideNavigationAnimation::class,
    navArgsDelegate = CellFilesNavArgs::class,
)
@Composable
fun ConversationFilesWithSlideInTransitionScreen(
    navigator: WireNavigator,
    cellFilesNavArgs: CellFilesNavArgs,
    viewModel: CellViewModel = hiltViewModel(),
) {

    ConversationFilesScreenContent(
        navigator = navigator,
        currentNodeUuid = viewModel.currentNodeUuid(),
        screenTitle = cellFilesNavArgs.screenTitle,
        actions = viewModel.actions,
        pagingListItems = viewModel.nodesFlow.collectAsLazyPagingItems(),
        downloadFileSheet = viewModel.downloadFileSheet,
        menu = viewModel.menu,
        sendIntent = { viewModel.sendIntent(it) },
        navigationIconType = NavigationIconType.Back()
    )
}
