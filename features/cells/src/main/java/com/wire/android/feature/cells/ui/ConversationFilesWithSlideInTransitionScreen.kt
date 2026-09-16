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

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R

@Composable
internal fun ConversationFilesSlideRouteScreen(
    navigation: CellsFilesNavigation,
    cellFilesNavArgs: CellFilesNavArgs,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CellViewModel,
) {
    LaunchedEffect(viewModel.navigateToRecycleBinRoot.collectAsState().value) {
        if (viewModel.navigateToRecycleBinRoot.value) {
            navigation.recycleBin(
                CellFilesNavArgs(
                    conversationId = viewModel.currentNodeUuid()?.substringBefore("/"),
                    isRecycleBin = true,
                ),
                popConsecutive = true,
            )
        }
    }

    ConversationFilesScreenContent(
        animatedVisibilityScope = animatedVisibilityScope,
        navigation = navigation,
        currentNodeUuid = viewModel.currentNodeUuid(),
        isSearchResult = false,
        screenTitle = stringResource(R.string.conversation_files_title),
        isRecycleBin = viewModel.isRecycleBin(),
        actions = viewModel.actions,
        pagingListItems = viewModel.nodesFlow.collectAsLazyPagingItems(),
        menu = viewModel.menu,
        isRestoreInProgress = viewModel.isRestoreInProgress.collectAsState().value,
        isDeleteInProgress = viewModel.isDeleteInProgress.collectAsState().value,
        isRefreshing = viewModel.isPullToRefresh.collectAsState(),
        breadcrumbs = cellFilesNavArgs.breadcrumbs,
        sendIntent = viewModel::sendIntent,
        onRefresh = viewModel::onPullToRefresh,
        retryEditNodeError = viewModel::editNode,
        fileReadyFlow = viewModel.fileReadyFlow,
        sortingCriteria = viewModel.sortingCriteria.collectAsState().value,
        onSortByClicked = viewModel::setSortBy,
        onSortOrderClicked = viewModel::setSorting,
        showViewerAccessBanner = viewModel.showViewerAccessBanner.collectAsState().value,
        drivePermissionsEnabled = viewModel.drivePermissionsEnabled,
    )
}
