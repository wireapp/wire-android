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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import com.wire.android.feature.cells.ui.create.FileTypeBottomSheetDialog
import com.wire.android.feature.cells.ui.create.file.CreateFileScreenNavArgs
import com.wire.android.feature.cells.ui.destinations.AddRemoveTagsScreenDestination
import com.wire.android.feature.cells.ui.destinations.ConversationFilesWithSlideInTransitionScreenDestination
import com.wire.android.feature.cells.ui.destinations.CreateFileScreenDestination
import com.wire.android.feature.cells.ui.destinations.CreateFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.MoveToFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
import com.wire.android.feature.cells.ui.destinations.RecycleBinScreenDestination
import com.wire.android.feature.cells.ui.destinations.RenameNodeScreenDestination
import com.wire.android.feature.cells.ui.destinations.SearchScreenDestination
import com.wire.android.feature.cells.ui.destinations.VersionHistoryScreenDestination
import com.wire.android.feature.cells.ui.dialog.CellsNewActionBottomSheet
import com.wire.android.feature.cells.ui.dialog.CellsOptionsBottomSheet
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.search.sharedElementSearchInputKey
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
import com.wire.android.ui.common.search.SearchBarState
import com.wire.android.ui.common.search.rememberSearchbarState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.theme.WireTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope> {
        error("SharedTransitionScope not provided. Wrap NavHost in SharedTransitionLayout.")
    }

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
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CellViewModel = hiltViewModel(),
) {
    val conversationSearchBarState = rememberSearchbarState(viewModel.isSearchByDefaultActive)

    LaunchedEffect(conversationSearchBarState.searchQueryTextState.text) {
        viewModel.onSearchQueryUpdated(conversationSearchBarState.searchQueryTextState.text.toString())
    }

    BackHandler(conversationSearchBarState.isSearchActive) {
        conversationSearchBarState.closeSearch()
    }

    ConversationFilesScreenContent(
        animatedVisibilityScope = animatedVisibilityScope,
        navigator = navigator,
        currentNodeUuid = viewModel.currentNodeUuid(),
        conversationSearchBarState = conversationSearchBarState,
        isRecycleBin = viewModel.isRecycleBin(),
        actions = viewModel.actions,
        pagingListItems = viewModel.nodesFlow.collectAsLazyPagingItems(),
        downloadFileSheet = viewModel.downloadFileSheet,
        menu = viewModel.menu,
        isSearchResult = viewModel.hasSearchQuery(),
        isRestoreInProgress = viewModel.isRestoreInProgress.collectAsState().value,
        isDeleteInProgress = viewModel.isDeleteInProgress.collectAsState().value,
        isRefreshing = viewModel.isPullToRefresh.collectAsState(),
        breadcrumbs = viewModel.breadcrumbs(),
        sendIntent = viewModel::sendIntent,
        onRefresh = viewModel::onPullToRefresh,
        retryEditNodeError = viewModel::editNode
    )

    LaunchedEffect(Unit) {
        viewModel.clearRemovedItems()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ConversationFilesScreenContent(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigator: WireNavigator,
    currentNodeUuid: String?,
    conversationSearchBarState: SearchBarState,
    isSearchResult: Boolean,
    actions: Flow<CellViewAction>,
    pagingListItems: LazyPagingItems<CellNodeUi>,
    downloadFileSheet: StateFlow<CellNodeUi.File?>,
    menu: SharedFlow<MenuOptions>,
    sendIntent: (CellViewIntent) -> Unit,
    isRefreshing: State<Boolean>,
    onRefresh: () -> Unit,
    retryEditNodeError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDeleteInProgress: Boolean = false,
    screenTitle: String? = null,
    isRecycleBin: Boolean = false,
    isRestoreInProgress: Boolean = false,
    breadcrumbs: Array<String>? = emptyArray(),
) {
    val newActionBottomSheetState = rememberWireModalSheetState<Unit>()
    val fileTypeBottomSheetState = rememberWireModalSheetState<Unit>()
    val optionsBottomSheetState = rememberWireModalSheetState<Unit>()

    val isFabVisible = when {
        pagingListItems.isLoading() -> false
        pagingListItems.isError() -> false
        isRecycleBin -> false
        else -> true
    }

    CellsNewActionBottomSheet(
        sheetState = newActionBottomSheetState,
        onDismiss = {
            newActionBottomSheetState.hide()
        },
        onCreateFolder = {
            newActionBottomSheetState.hide()
            navigator.navigate(NavigationCommand(CreateFolderScreenDestination(currentNodeUuid)))
        },
        onCreateFile = {
            newActionBottomSheetState.hide()
            fileTypeBottomSheetState.show()
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
                        isRecycleBin = true,
                        breadcrumbs = arrayOf(breadcrumbs?.first() ?: ""),
                    )
                )
            )
            optionsBottomSheetState.hide()
        }
    )

    FileTypeBottomSheetDialog(
        sheetState = fileTypeBottomSheetState,
        onDismiss = {
            fileTypeBottomSheetState.hide()
        },
        onItemSelected = {
            currentNodeUuid?.let { uuid ->
                navigator.navigate(NavigationCommand(CreateFileScreenDestination(CreateFileScreenNavArgs(uuid, it))))
            }
            fileTypeBottomSheetState.hide()
        },
    )

    WireScaffold(
        modifier = modifier,
        topBar = {
            Column {
                WireCenterAlignedTopAppBar(
                    onNavigationPressed = { navigator.navigateBack() },
                    title = screenTitle ?: stringResource(R.string.conversation_files_title),
                    navigationIconType = NavigationIconType.Back(),
                    elevation = dimensions().spacing0x,
                    actions = {
                        if (!isRecycleBin) {
                            MoreOptionIcon(
                                contentDescription = R.string.content_description_conversation_files_more_button,
                                onButtonClicked = { optionsBottomSheetState.show() }
                            )
                        }
                    }
                )

                val sharedScope = LocalSharedTransitionScope.current
                val focusRequester = remember { FocusRequester() }

                with(sharedScope) {
                    SearchTopBar(
                        modifier = Modifier
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = sharedElementSearchInputKey),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                        isSearchActive = conversationSearchBarState.isSearchActive,
                        searchBarHint = stringResource(R.string.search_shared_drive_text_input_hint),
                        searchQueryTextState = conversationSearchBarState.searchQueryTextState,
                        onActiveChanged = conversationSearchBarState::searchActiveChanged,
                        onTap = {
                            currentNodeUuid?.let {
                                navigator.navigate(
                                    NavigationCommand(
                                        SearchScreenDestination(conversationId = it)
                                    )
                                )
                            }
                        },
                        focusRequester = focusRequester,
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
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            CellScreenContent(
                actionsFlow = actions,
                pagingListItems = pagingListItems,
                sendIntent = sendIntent,
                downloadFileState = downloadFileSheet,
                menuState = menu,
                isSearchResult = isSearchResult,
                isRestoreInProgress = isRestoreInProgress,
                isDeleteInProgress = isDeleteInProgress,
                isRecycleBin = isRecycleBin,
                openFolder = { path, title, parentFolderUuid ->
                    navigator.navigate(
                        NavigationCommand(
                            ConversationFilesWithSlideInTransitionScreenDestination(
                                conversationId = path,
                                screenTitle = title,
                                isRecycleBin = isRecycleBin,
                                parentFolderUuid = parentFolderUuid,
                                breadcrumbs = (breadcrumbs ?: emptyArray()) + title
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
                },
                showVersionHistoryScreen = { uuid, fileName ->
                    navigator.navigate(NavigationCommand(VersionHistoryScreenDestination(uuid, fileName)))
                },
                retryEditNodeError = { retryEditNodeError(it) },
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@MultipleThemePreviews
fun PreviewConversationFilesScreen() {
    WireTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                ConversationFilesScreenContent(
                    animatedVisibilityScope = this,
                    navigator = PreviewNavigator,
                    currentNodeUuid = "conversationId",
                    conversationSearchBarState = rememberSearchbarState(),
                    isSearchResult = false,
                    actions = flowOf(),
                    pagingListItems = MutableStateFlow(
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
                                    userHandle = "userHandle",
                                    ownerUserId = "userA",
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
                                    userHandle = "userHandle",
                                    ownerUserId = "userB",
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
                    isRefreshing = remember { mutableStateOf(false) },
                    onRefresh = {},
                    retryEditNodeError = {},
                )

            }


        }

    }
}
