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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.CellFilesNavArgs
import com.wire.android.feature.cells.ui.CellScreenContent
import com.wire.android.feature.cells.ui.CellViewModel
import com.wire.android.feature.cells.ui.common.FullScreenLoading
import com.wire.android.feature.cells.ui.destinations.ConversationFilesWithSlideInTransitionScreenDestination
import com.wire.android.feature.cells.ui.destinations.MoveToFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.theme.wireTypography

@WireDestination(
    navArgsDelegate = CellFilesNavArgs::class,
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
                WireCenterAlignedTopAppBar(
                    elevation = dimensions().spacing0x,
                    titleContent = {
                        WireTopAppBarTitle(
                            title = stringResource(R.string.recycle_bin),
                            style = MaterialTheme.wireTypography.title01,
                            maxLines = 2
                        )
                    },
                    navigationIconType = NavigationIconType.Close(com.wire.android.ui.common.R.string.content_description_close),
                    onNavigationPressed = {
                        navigator.navigateBack()
                    }
                )
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
                    onFolderClick = {
                        val folderPath = "${cellViewModel.currentNodeUuid()}/recycle_bin/${it.name}"

                        navigator.navigate(
                            NavigationCommand(
                                ConversationFilesWithSlideInTransitionScreenDestination(
                                    conversationId = folderPath,
                                    screenTitle = it.name,
                                    isRecycleBin = true
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
                    showAddRemoveTagsScreen = {}
                )
            }
        }

        if (cellViewModel.isLoading.collectAsState().value) {
            FullScreenLoading()
        }
    }
}
