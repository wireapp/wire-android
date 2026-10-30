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
package com.wire.android.feature.cells.ui.recyclebin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.CellBulkAction
import com.wire.android.feature.cells.ui.CellBulkActionsBar
import com.wire.android.feature.cells.ui.CellFilesNavArgs
import com.wire.android.feature.cells.ui.CellScreenContent
import com.wire.android.feature.cells.ui.CellViewIntent
import com.wire.android.feature.cells.ui.CellViewModel
import com.wire.android.feature.cells.ui.CellsFilesNavigation
import com.wire.android.feature.cells.ui.SelectionAwareTitle
import com.wire.android.feature.cells.ui.common.Breadcrumbs
import com.wire.android.feature.cells.ui.dialog.BulkDeleteDialogs
import com.wire.android.feature.cells.ui.dialog.BulkRestoreDialogs
import com.wire.android.feature.cells.ui.toIntent
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun RecycleBinRouteScreen(
    navigation: CellsFilesNavigation,
    cellViewModel: CellViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedNodes by cellViewModel.selectedNodes.collectAsState()
    val isSelectionMode = selectedNodes.isNotEmpty()
    val bulkDeleteDialog by cellViewModel.bulkDeleteDialog.collectAsState()
    val bulkRestoreDialog by cellViewModel.bulkRestoreDialog.collectAsState()

    BackHandler(enabled = isSelectionMode) {
        cellViewModel.sendIntent(CellViewIntent.OnClearSelection)
    }

    Box(modifier = modifier) {
        WireScaffold(
            topBar = {
                Column {
                    WireCenterAlignedTopAppBar(
                        elevation = dimensions().spacing0x,
                        titleContent = {
                            SelectionAwareTitle(
                                title = stringResource(R.string.recycle_bin),
                                selectedCount = selectedNodes.size,
                            )
                        },
                        navigationIconType = if (isSelectionMode) {
                            NavigationIconType.Close(R.string.cells_bulk_selection_cancel_content_description)
                        } else {
                            NavigationIconType.Back(com.wire.android.ui.common.R.string.content_description_back_button)
                        },
                        animateNavigationIcon = true,
                        onNavigationPressed = {
                            if (isSelectionMode) {
                                cellViewModel.sendIntent(CellViewIntent.OnClearSelection)
                            } else {
                                navigation.back()
                            }
                        }
                    )

                    cellViewModel.breadcrumbs()?.let {
                        Breadcrumbs(
                            modifier = Modifier
                                .height(dimensions().spacing40x)
                                .fillMaxWidth(),
                            pathSegments = it,
                            isRecycleBin = cellViewModel.isRecycleBin(),
                            onBreadcrumbsFolderClick = {}
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                CellScreenContent(
                    actionsFlow = cellViewModel.actions,
                    pagingListItems = cellViewModel.nodesFlow.collectAsLazyPagingItems(),
                    sendIntent = { cellViewModel.sendIntent(it) },
                    menuState = cellViewModel.menu,
                    isRecycleBin = true,
                    isRestoreInProgress = cellViewModel.isRestoreInProgress.collectAsState().value,
                    isDeleteInProgress = cellViewModel.isDeleteInProgress.collectAsState().value,
                    openFolder = { path, title, parentFolderUuid ->
                        navigation.folder(
                            CellFilesNavArgs(
                                conversationId = path,
                                screenTitle = title,
                                isRecycleBin = true,
                                breadcrumbs = (cellViewModel.breadcrumbs() ?: emptyArray()) + title,
                                parentFolderUuid = parentFolderUuid,
                            )
                        )
                    },
                    showPublicLinkScreen = navigation::publicLink,
                    showMoveToFolderScreen = navigation::move,
                    showBulkMoveToFolderScreen = navigation::moveNodes,
                    showRenameScreen = { },
                    showAddRemoveTagsScreen = {},
                    showImageViewer = navigation::image,
                    isRefreshing = cellViewModel.isPullToRefresh.collectAsState(),
                    onRefresh = { cellViewModel.onPullToRefresh() },
                    selectedNodeIds = selectedNodes.keys,
                    isSelectionEnabled = cellViewModel.isBulkSelectionEnabled.collectAsState().value,
                )
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = dimensions().spacing16x),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            CellBulkActionsBar(
                actions = persistentListOf(CellBulkAction.Restore, CellBulkAction.DeletePermanently),
                onActionClick = { action -> cellViewModel.sendIntent(action.toIntent()) },
            )
        }
    }

    bulkDeleteDialog?.let { dialog ->
        BulkDeleteDialogs(
            dialog = dialog,
            onConfirm = { cellViewModel.sendIntent(CellViewIntent.OnBulkDeleteConfirmed) },
            onDismiss = { cellViewModel.sendIntent(CellViewIntent.OnBulkDeleteDialogDismissed) },
        )
    }

    bulkRestoreDialog?.let { dialog ->
        BulkRestoreDialogs(
            dialog = dialog,
            onConfirm = { cellViewModel.sendIntent(CellViewIntent.OnBulkRestoreConfirmed) },
            onDismiss = { cellViewModel.sendIntent(CellViewIntent.OnBulkRestoreDialogDismissed) },
        )
    }
}
