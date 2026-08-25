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
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.common.OfflineBanner
import com.wire.android.feature.cells.ui.create.FileTypeBottomSheetDialog
import com.wire.android.feature.cells.ui.dialog.CellsNewActionBottomSheet
import com.wire.android.feature.cells.ui.dialog.CellsOptionsBottomSheet
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortRowWithMenu
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.feature.cells.ui.search.sort.toNavArg
import com.wire.android.navigation.transition.LocalSharedTransitionScope
import com.wire.android.navigation.transition.SHARED_ELEMENT_SEARCH_INPUT_KEY
import com.wire.android.navigation.transition.SHARED_ELEMENT_TOP_APP_BAR_KEY
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.button.FloatingActionButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.theme.WireTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

@Composable
internal fun ConversationFilesRouteScreen(
    navigation: CellsFilesNavigation,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CellViewModel,
) {
    val isOnlineState by viewModel.isOnline.collectAsState()
    // When offline files are disabled, never enter offline mode so all offline UI stays hidden.
    val isOnline = isOnlineState || !viewModel.offlineFilesEnabled

    ConversationFilesScreenContent(
        animatedVisibilityScope = animatedVisibilityScope,
        navigation = navigation,
        currentNodeUuid = viewModel.currentNodeUuid(),
        isRecycleBin = viewModel.isRecycleBin(),
        actions = viewModel.actions,
        pagingListItems = viewModel.nodesFlow.collectAsLazyPagingItems(),
        menu = viewModel.menu,
        isSearchResult = false,
        isOnline = isOnline,
        isRestoreInProgress = viewModel.isRestoreInProgress.collectAsState().value,
        isDeleteInProgress = viewModel.isDeleteInProgress.collectAsState().value,
        isRefreshing = viewModel.isPullToRefresh.collectAsState(),
        breadcrumbs = viewModel.breadcrumbs(),
        sendIntent = viewModel::sendIntent,
        onRefresh = viewModel::onPullToRefresh,
        retryEditNodeError = viewModel::editNode,
        fileReadyFlow = viewModel.fileReadyFlow,
        sortingCriteria = viewModel.sortingCriteria.collectAsState().value,
        onSortByClicked = viewModel::setSortBy,
        onSortOrderClicked = viewModel::setSorting,
    )

    LaunchedEffect(Unit) {
        viewModel.clearRemovedItems()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Suppress("CyclomaticComplexMethod")
@Composable
internal fun ConversationFilesScreenContent(
    animatedVisibilityScope: AnimatedVisibilityScope,
    navigation: CellsFilesNavigation,
    currentNodeUuid: String?,
    isSearchResult: Boolean,
    actions: Flow<CellViewAction>,
    pagingListItems: LazyPagingItems<CellNodeUi>,
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
    isOnline: Boolean = true,
    breadcrumbs: Array<String>? = emptyArray(),
    fileReadyFlow: Flow<CellNodeUi.File> = emptyFlow(),
    sortingCriteria: SortingCriteria = SortingCriteria.FoldersFirst,
    onSortByClicked: (SortBy) -> Unit = {},
    onSortOrderClicked: (SortingCriteria) -> Unit = {},
) {
    val sharedScope = LocalSharedTransitionScope.current

    val newActionBottomSheetState = rememberWireModalSheetState<Unit>()
    val fileTypeBottomSheetState = rememberWireModalSheetState<Unit>()
    val optionsBottomSheetState = rememberWireModalSheetState<Unit>()

    val isFabVisible = when {
        pagingListItems.isLoading() -> false
        pagingListItems.isError() -> false
        isRecycleBin -> false
        else -> true
    }

    val lazyListState = rememberLazyListState()
    LaunchedEffect(sortingCriteria) {
        lazyListState.animateScrollToItem(0)
    }

    CellsNewActionBottomSheet(
        sheetState = newActionBottomSheetState,
        onDismiss = {
            newActionBottomSheetState.hide()
        },
        onCreateFolder = {
            newActionBottomSheetState.hide()
            navigation.createFolder(currentNodeUuid)
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
            navigation.recycleBin(
                CellFilesNavArgs(
                    conversationId = currentNodeUuid?.substringBefore("/"),
                    isRecycleBin = true,
                    breadcrumbs = arrayOf(breadcrumbs?.first() ?: ""),
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
                navigation.createFile(uuid, it)
            }
            fileTypeBottomSheetState.hide()
        },
    )
    with(sharedScope) {
        WireScaffold(
            modifier = modifier,
            topBar = {
                Column {
                    WireCenterAlignedTopAppBar(
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = SHARED_ELEMENT_TOP_APP_BAR_KEY),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                        onNavigationPressed = navigation::back,
                        title = screenTitle ?: stringResource(R.string.conversation_files_title),
                        navigationIconType = NavigationIconType.Back(),
                        elevation = dimensions().spacing0x,
                        actions = {
                            if (!isRecycleBin && isOnline) {
                                MoreOptionIcon(
                                    contentDescription = R.string.content_description_conversation_files_more_button,
                                    onButtonClicked = { optionsBottomSheetState.show() }
                                )
                            }
                        }
                    )

                    if (isOnline) {
                        SearchTopBar(
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = SHARED_ELEMENT_SEARCH_INPUT_KEY),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            isSearchActive = false,
                            searchBarHint = stringResource(R.string.search_label),
                            searchQueryTextState = TextFieldState(),
                            onTap = {
                                currentNodeUuid?.let {
                                    navigation.search(it, sortingCriteria.toNavArg())
                                }
                            },
                        )
                        if (!isRecycleBin) {
                            SortRowWithMenu(
                                sortingCriteria = sortingCriteria,
                                screenType = DriveSearchScreenType.SHARED_DRIVE,
                                onSortByClicked = onSortByClicked,
                                onOrderClicked = onSortOrderClicked,
                            )
                        }
                    } else {
                        OfflineBanner()
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
            CellScreenContent(
                modifier = Modifier.padding(innerPadding),
                lazyListState = lazyListState,
                actionsFlow = actions,
                pagingListItems = pagingListItems,
                sendIntent = sendIntent,
                menuState = menu,
                isSearchResult = isSearchResult,
                isRestoreInProgress = isRestoreInProgress,
                isDeleteInProgress = isDeleteInProgress,
                isRecycleBin = isRecycleBin,
                isOffline = !isOnline,
                openFolder = { path, title, parentFolderUuid ->
                    navigation.folder(
                        CellFilesNavArgs(
                            conversationId = path,
                            screenTitle = title,
                            isRecycleBin = isRecycleBin,
                            parentFolderUuid = parentFolderUuid,
                            breadcrumbs = (breadcrumbs ?: emptyArray()) + title,
                        )
                    )
                },
                showPublicLinkScreen = navigation::publicLink,
                showMoveToFolderScreen = navigation::move,
                showRenameScreen = navigation::rename,
                showAddRemoveTagsScreen = navigation::tags,
                showVersionHistoryScreen = navigation::versionHistory,
                showImageViewer = navigation::image,
                showVideoViewer = navigation::video,
                showAudioPlayer = navigation::audio,
                retryEditNodeError = { retryEditNodeError(it) },
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                fileReadyFlow = fileReadyFlow,
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
                    navigation = NoOpCellsFilesNavigation,
                    currentNodeUuid = "conversationId",
                    isSearchResult = false,
                    actions = flowOf(),
                    pagingListItems = MutableStateFlow(
                        PagingData.from(
                            listOf(
                                CellNodeUi.File(
                                    uuid = "file1",
                                    conversationId = "conversationId",
                                    name = "File 1",
                                    assetType = AttachmentFileType.IMAGE,
                                    size = 123456,
                                    localPath = null,
                                    mimeType = "image/png",
                                    publicLinkId = "link1",
                                    userName = "User A",
                                    userHandle = "userHandle",
                                    ownerUserId = "userA",
                                    conversationName = "Conversation A",
                                    modifiedTime = 1696154400000L,
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
                                    modifiedTime = 1696154400000L,
                                    size = 123456,
                                )
                            )
                        )
                    ).collectAsLazyPagingItems(),
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
