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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.common.Breadcrumbs
import com.wire.android.feature.cells.ui.destinations.AddRemoveTagsScreenDestination
import com.wire.android.feature.cells.ui.destinations.ConversationFilesWithSlideInTransitionScreenDestination
import com.wire.android.feature.cells.ui.destinations.CreateFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.MoveToFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
import com.wire.android.feature.cells.ui.destinations.RecycleBinScreenDestination
import com.wire.android.feature.cells.ui.destinations.RenameNodeScreenDestination
import com.wire.android.feature.cells.ui.dialog.CellsNewActionBottomSheet
import com.wire.android.feature.cells.ui.dialog.CellsOptionsBottomSheet
import com.wire.android.feature.cells.ui.model.BottomSheetActionsContext
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.PreviewNavigator
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.button.FloatingActionButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.WireTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Show files in one conversation.
 * Conversation id is passed to view model via navigation parameters [CellFilesNavArgs].
 */
@WireDestination(
    style = PopUpNavigationAnimation::class,
    navArgsDelegate = CellFilesNavArgs::class,
)
@Composable
fun ConversationFilesScreen(
    navigator: WireNavigator,
    viewModel: CellViewModel = hiltViewModel(),
) {

    LaunchedEffect(viewModel.isRecycleBin()) {
        if (viewModel.isRecycleBin()) {
            viewModel.setBottomSheetActionsContext(BottomSheetActionsContext.RecycleBin)
        } else {
            viewModel.setBottomSheetActionsContext(BottomSheetActionsContext.Conversation)
        }
    }

    ConversationFilesScreenContent(
        navigator = navigator,
        currentNodeUuid = viewModel.currentNodeUuid(),
        isRecycleBin = viewModel.isRecycleBin(),
        actions = viewModel.actions,
        pagingListItems = viewModel.nodesFlow.collectAsLazyPagingItems(),
        downloadFileSheet = viewModel.downloadFileSheet,
        menu = viewModel.menu,
        breadcrumbs = viewModel.breadcrumbs(),
        sendIntent = { viewModel.sendIntent(it) },
    )
}

@Composable
fun ConversationFilesScreenContent(
    navigator: WireNavigator,
    currentNodeUuid: String?,
    actions: Flow<CellViewAction>,
    pagingListItems: LazyPagingItems<CellNodeUi>,
    downloadFileSheet: StateFlow<CellNodeUi.File?>,
    menu: SharedFlow<MenuOptions>,
    sendIntent: (CellViewIntent) -> Unit,
    modifier: Modifier = Modifier,
    screenTitle: String? = null,
    isRecycleBin: Boolean = false,
    breadcrumbs: Array<String>? = emptyArray(),
    navigationIconType: NavigationIconType = NavigationIconType.Close()
) {
    val newActionBottomSheetState = rememberWireModalSheetState<Unit>()
    val optionsBottomSheetState = rememberWireModalSheetState<Unit>()

    val isFabVisible = when {
        pagingListItems.isLoading() -> false
        pagingListItems.isError() -> false
        isRecycleBin == true -> false
        else -> true
    }

    CellsNewActionBottomSheet(
        sheetState = newActionBottomSheetState,
        onDismiss = {
            newActionBottomSheetState.hide()
        },
        onCreateFolder = {
            navigator.navigate(NavigationCommand(CreateFolderScreenDestination(currentNodeUuid)))
        }
    )

    CellsOptionsBottomSheet(
        sheetState = optionsBottomSheetState,
        onDismiss = {
            optionsBottomSheetState.hide()
        },
        showRecycleBin = {
            navigator.navigate(
                NavigationCommand(
                    RecycleBinScreenDestination(
                        conversationId = currentNodeUuid?.substringBefore("/"),
                        isRecycleBin = true
                    )
                )
            )
            optionsBottomSheetState.hide()
        }
    )

    WireScaffold(
        modifier = modifier,
        snackbarHost = {},
        topBar = {
            Column {
                WireCenterAlignedTopAppBar(
                    onNavigationPressed = { navigator.navigateBack() },
                    title = screenTitle ?: stringResource(R.string.conversation_files_title),
                    navigationIconType = navigationIconType,
                    elevation = dimensions().spacing0x,
                    actions = {
                        if (isRecycleBin == false) {
                            MoreOptionIcon(
                                contentDescription = R.string.content_description_conversation_files_more_button,
                                onButtonClicked = { optionsBottomSheetState.show() }
                            )
                        }
                    }
                )
                breadcrumbs?.let {
                    Breadcrumbs(
                        modifier = Modifier
                            .height(dimensions().spacing40x)
                            .fillMaxWidth(),
                        pathSegments = it
                    )
                }
            }
        },
        floatingActionButton = {
            if (isFabVisible) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    FloatingActionButton(
                        text = stringResource(R.string.cells_new_label),
                        icon = {
                            Image(
                                painter = painterResource(id = com.wire.android.ui.common.R.drawable.ic_plus),
                                contentDescription = stringResource(R.string.cells_new_label_content_description),
                                contentScale = ContentScale.FillBounds,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                modifier = Modifier
                                    .padding(
                                        start = dimensions().spacing4x,
                                        top = dimensions().spacing2x
                                    )
                                    .size(dimensions().fabIconSize)
                            )
                        },
                        onClick = { newActionBottomSheetState.show() }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            CellScreenContent(
                actionsFlow = actions,
                pagingListItems = pagingListItems,
                sendIntent = sendIntent,
                downloadFileState = downloadFileSheet,
                menuState = menu,
                isAllFiles = false,
                isRecycleBin = isRecycleBin,
                onFolderClick = {
                    val folderPath = "$currentNodeUuid/${it.name}"

                    navigator.navigate(
                        NavigationCommand(
                            ConversationFilesWithSlideInTransitionScreenDestination(
                                conversationId = folderPath,
                                screenTitle = it.name,
                                isRecycleBin = isRecycleBin,
                                breadcrumbs = it.name?.let { name -> (breadcrumbs ?: emptyArray()) + name }
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
                                uuid = uuid
                            )
                        )
                    )
                },
                showRenameScreen = { cellNodeUi ->
                    navigator.navigate(
                        NavigationCommand(
                            RenameNodeScreenDestination(
                                uuid = cellNodeUi.uuid,
                                currentPath = cellNodeUi.remotePath,
                                isFolder = cellNodeUi is CellNodeUi.Folder,
                                nodeName = cellNodeUi.name,
                            )
                        )
                    )
                },
                showAddRemoveTagsScreen = { node ->
                    navigator.navigate(
                        NavigationCommand(
                            AddRemoveTagsScreenDestination(node.uuid, node.tags.toCollection(ArrayList()))
                        )
                    )
                }
            )
        }
    }
}

@Composable
@MultipleThemePreviews
fun PreviewConversationFilesScreen() {
    WireTheme {
        ConversationFilesScreenContent(
            navigator = PreviewNavigator,
            currentNodeUuid = "conversationId",
            actions = flowOf(),
            pagingListItems = flowOf(
                PagingData.from(
                    listOf(
                        CellNodeUi.File(
                            uuid = "file1",
                            name = "File 1",
                            downloadProgress = 0.5f,
                            assetType = AttachmentFileType.IMAGE,
                            size = 123456,
                            localPath = null,
                            mimeType = "image/png",
                            publicLinkId = "link1",
                            userName = "User A",
                            conversationName = "Conversation A",
                            modifiedTime = "2023-10-01T12:00:00Z",
                            remotePath = "/path/to/file1.png",
                            contentHash = null,
                            contentUrl = null,
                            previewUrl = null
                        ),
                        CellNodeUi.Folder(
                            uuid = "folder1",
                            name = "Folder 1",
                            remotePath = "/path/to/folder1",
                            userName = "User B",
                            conversationName = "Conversation B",
                            modifiedTime = "2023-10-01T12:00:00Z",
                            size = 123456,
                        )
                    )
                )
            ).collectAsLazyPagingItems(),
            downloadFileSheet = MutableStateFlow(null),
            menu = MutableSharedFlow(replay = 0),
            sendIntent = {},
            screenTitle = "Android",
            isRecycleBin = false,
            breadcrumbs = arrayOf("Engineering", "Android"),
            navigationIconType = NavigationIconType.Close()
        )
    }
}
