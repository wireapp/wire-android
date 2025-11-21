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
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.ui.cells.destinations.ConversationFilesWithSlideInTransitionScreenDestination
import com.wire.android.ui.cells.destinations.MoveToFolderScreenDestination
import com.wire.android.ui.cells.destinations.PublicLinkScreenDestination
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.CellFilesNavArgs
import com.wire.android.feature.cells.ui.CellScreenContent
import com.wire.android.feature.cells.ui.CellViewModel
import com.wire.android.feature.cells.ui.common.Breadcrumbs
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.theme.wireTypography

@Destination<ExternalModuleGraph>(
    navArgs = CellFilesNavArgs::class,
)
@Composable
fun RecycleBinScreen(
    navigator: WireNavigator,
    modifier: Modifier = Modifier,
    cellViewModel: CellViewModel = hiltViewModel()
) {

    Box(modifier = modifier) {
        WireScaffold(
            topBar = {
                Column {
                    WireCenterAlignedTopAppBar(
                        elevation = dimensions().spacing0x,
                        titleContent = {
                            WireTopAppBarTitle(
                                title = stringResource(R.string.recycle_bin),
                                style = MaterialTheme.wireTypography.title01,
                                maxLines = 2
                            )
                        },
                        navigationIconType = NavigationIconType.Back(com.wire.android.ui.common.R.string.content_description_back_button),
                        onNavigationPressed = {
                            navigator.navigateBack()
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
                    downloadFileState = cellViewModel.downloadFileSheet,
                    menuState = cellViewModel.menu,
                    isAllFiles = false,
                    isRecycleBin = true,
                    isRestoreInProgress = cellViewModel.isRestoreInProgress.collectAsState().value,
                    isDeleteInProgress = cellViewModel.isDeleteInProgress.collectAsState().value,
                    openFolder = { path, title, parentFolderUuid ->
                        navigator.navigate(
                            NavigationCommand(
                                ConversationFilesWithSlideInTransitionScreenDestination(
                                    conversationId = path,
                                    screenTitle = title,
                                    isRecycleBin = true,
                                    breadcrumbs = (cellViewModel.breadcrumbs() ?: emptyArray()) + title,
                                    parentFolderUuid = parentFolderUuid,
                                ),
                                BackStackMode.NONE,
                                launchSingleTop = false
                            )
                        )
                    },
                    showPublicLinkScreen = { publicLinkScreenData ->
                        navigator.navigate(
                            NavigationCommand(
                                PublicLinkScreenDestination(
                                    assetId = publicLinkScreenData.assetId,
                                    fileName = publicLinkScreenData.fileName,
                                    publicLinkId = publicLinkScreenData.linkId,
                                    isFolder = publicLinkScreenData.isFolder
                                )
                            )
                        )
                    },
                    showMoveToFolderScreen = { currentPath, nodePath, uuid ->
                        navigator.navigate(
                            NavigationCommand(
                                MoveToFolderScreenDestination(
                                    currentPath = currentPath,
                                    nodeToMovePath = nodePath,
                                    uuid = uuid,
                                )
                            )
                        )
                    },
                    showRenameScreen = { },
                    showAddRemoveTagsScreen = {},
                    isRefreshing = cellViewModel.isPullToRefresh.collectAsState(),
                    onRefresh = { cellViewModel.onPullToRefresh() }
                )
            }
        }
    }
}
