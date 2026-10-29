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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.domain.model.AttachmentFileType
import com.wire.android.feature.cells.ui.common.BulkSelectionHint
import com.wire.android.feature.cells.ui.common.OfflineBanner
import com.wire.android.feature.cells.ui.create.FileTypeBottomSheetDialog
import com.wire.android.feature.cells.ui.dialog.BulkDeleteDialog
import com.wire.android.feature.cells.ui.dialog.BulkDeleteDialogs
import com.wire.android.feature.cells.ui.dialog.CellsNewActionBottomSheet
import com.wire.android.feature.cells.ui.dialog.CellsOptionsBottomSheet
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.search.DriveSearchScreenType
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortRowWithMenu
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.feature.cells.ui.search.sort.toNavArg
import com.wire.android.feature.cells.ui.upload.UploadConfirmationDialog
import com.wire.android.feature.cells.ui.upload.UploadStatusBottomSheetContent
import com.wire.android.feature.cells.ui.upload.UploadStatusIndicator
import com.wire.android.navigation.transition.LocalSharedTransitionScope
import com.wire.android.navigation.transition.SHARED_ELEMENT_SEARCH_INPUT_KEY
import com.wire.android.navigation.transition.SHARED_ELEMENT_TOP_APP_BAR_KEY
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.banner.ViewerAccessBanner
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.button.FloatingActionButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.permission.rememberChooseMultipleFilesFlow
import com.wire.kalium.cells.domain.CellUploadItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
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
    val uploadStatusViewModel = uploadStatusViewModel(viewModel.currentNodeUuid()?.substringBefore("/"))
    val uploads by uploadStatusViewModel.uploads.collectAsState()
    val uploadConfirmation by viewModel.uploadConfirmation.collectAsState()

    // Sub folders of the recycle bin are opened with this screen too: after restoring their parent
    // folder, go back to the recycle bin root.
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
        uploads = uploads,
        onCancelUpload = uploadStatusViewModel::cancel,
        onCancelAllUploads = uploadStatusViewModel::cancelAll,
        onRetryUpload = uploadStatusViewModel::retry,
        onRetryAllFailedUploads = uploadStatusViewModel::retryAllFailed,
        onDismissUpload = uploadStatusViewModel::dismiss,
        onDismissAllUploads = uploadStatusViewModel::dismissAll,
        uploadConfirmation = uploadConfirmation,
        onConfirmUpload = viewModel::confirmUpload,
        onCancelUploadConfirmation = viewModel::cancelUploadConfirmation,
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
        screenTitle = viewModel.screenTitle(),
        breadcrumbs = viewModel.breadcrumbs(),
        sendIntent = viewModel::sendIntent,
        onRefresh = viewModel::onPullToRefresh,
        retryEditNodeError = viewModel::editNode,
        fileReadyFlow = viewModel.fileReadyFlow,
        sortingCriteria = viewModel.sortingCriteria.collectAsState().value,
        onSortByClicked = viewModel::setSortBy,
        onSortOrderClicked = viewModel::setSorting,
        showViewerAccessBanner = viewModel.showViewerAccessBanner.collectAsState().value,
        isSelfUserViewerOnly = viewModel.isSelfUserViewerOnly.collectAsState().value,
        driveDirectUploadEnabled = viewModel.driveDirectUploadEnabled,
        onViewerAccessBannerCloseClick = viewModel::onViewerAccessBannerDismissed,
        showBulkSelectionHint = viewModel.showBulkSelectionHint.collectAsState().value,
        onBulkSelectionHintDismiss = viewModel::onBulkSelectionHintDismissed,
        selectedNodes = viewModel.selectedNodes.collectAsState().value,
        isBulkSelectionEnabled = viewModel.isBulkSelectionEnabled.collectAsState().value,
        bulkDeleteDialog = viewModel.bulkDeleteDialog.collectAsState().value,
        offlineFilesEnabled = viewModel.offlineFilesEnabled,
    )

    LaunchedEffect(Unit) {
        viewModel.clearRemovedItems()
    }
}

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
    showViewerAccessBanner: Boolean = false,
    isSelfUserViewerOnly: Boolean = false,
    driveDirectUploadEnabled: Boolean = false,
    onViewerAccessBannerCloseClick: () -> Unit = {},
    showBulkSelectionHint: Boolean = false,
    onBulkSelectionHintDismiss: () -> Unit = {},
    uploads: List<CellUploadItem> = emptyList(),
    onCancelUpload: (String) -> Unit = {},
    onCancelAllUploads: () -> Unit = {},
    onRetryUpload: (String) -> Unit = {},
    onRetryAllFailedUploads: () -> Unit = {},
    onDismissUpload: (String) -> Unit = {},
    onDismissAllUploads: () -> Unit = {},
    uploadConfirmation: UploadConfirmation? = null,
    onConfirmUpload: () -> Unit = {},
    onCancelUploadConfirmation: () -> Unit = {},
    selectedNodes: Map<String, CellNodeUi> = emptyMap(),
    isBulkSelectionEnabled: Boolean = false,
    bulkDeleteDialog: BulkDeleteDialog? = null,
    offlineFilesEnabled: Boolean = false,
) {
    val sharedScope = LocalSharedTransitionScope.current
    val isSelectionMode = selectedNodes.isNotEmpty()
    val fabToBulkActionsKey = "$FAB_TO_BULK_ACTIONS_KEY-$currentNodeUuid"

    BackHandler(enabled = isSelectionMode) {
        sendIntent(CellViewIntent.OnClearSelection)
    }

    val newActionBottomSheetState = rememberWireModalSheetState<Unit>()
    val uploadStatusSheetState = rememberWireModalSheetState<Unit>()
    val fileTypeBottomSheetState = rememberWireModalSheetState<Unit>()
    val optionsBottomSheetState = rememberWireModalSheetState<Unit>()

    LaunchedEffect(uploads) {
        if (uploads.isEmpty() && uploadStatusSheetState.isVisible) {
            uploadStatusSheetState.hide()
        }
    }

    val uploadFilesFlow = rememberChooseMultipleFilesFlow(
        onFileBrowserItemPicked = { uris ->
            sendIntent(CellViewIntent.OnFilesPickedForUpload(uris))
        },
        onPermissionDenied = { /* Nothing to do */ },
        onPermissionPermanentlyDenied = { /* Nothing to do */ },
    )

    val isFabVisible = when {
        isSelectionMode -> false
        isSelfUserViewerOnly -> false
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
        },
        onUploadFiles = {
            newActionBottomSheetState.hide()
            uploadFilesFlow.launch()
        },
        driveDirectUploadEnabled = driveDirectUploadEnabled,
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
                    breadcrumbs = arrayOf(breadcrumbs?.firstOrNull() ?: ""),
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

    WireModalSheetLayout(sheetState = uploadStatusSheetState) {
        UploadStatusBottomSheetContent(
            uploads = uploads,
            onCollapse = uploadStatusSheetState::hide,
            onCancel = onCancelUpload,
            onCancelAll = onCancelAllUploads,
            onRetry = onRetryUpload,
            onRetryAllFailed = onRetryAllFailedUploads,
            onDismiss = onDismissUpload,
        )
    }

    uploadConfirmation?.let { confirmation ->
        val destinationName = breadcrumbs?.lastOrNull() ?: screenTitle ?: stringResource(R.string.conversation_files_title)
        UploadConfirmationDialog(
            fileNames = confirmation.fileNames,
            destinationName = destinationName,
            onConfirm = onConfirmUpload,
            onDismiss = onCancelUploadConfirmation,
        )
    }

    bulkDeleteDialog?.let { dialog ->
        BulkDeleteDialogs(
            dialog = dialog,
            onConfirm = { sendIntent(CellViewIntent.OnBulkDeleteConfirmed) },
            onDismiss = { sendIntent(CellViewIntent.OnBulkDeleteDialogDismissed) },
        )
    }

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
                        onNavigationPressed = if (isSelectionMode) {
                            { sendIntent(CellViewIntent.OnClearSelection) }
                        } else {
                            navigation::back
                        },
                        titleContent = {
                            SelectionAwareTitle(
                                title = screenTitle ?: stringResource(R.string.conversation_files_title),
                                selectedCount = selectedNodes.size,
                            )
                        },
                        navigationIconType = if (isSelectionMode) {
                            NavigationIconType.Close(R.string.cells_bulk_selection_cancel_content_description)
                        } else {
                            NavigationIconType.Back()
                        },
                        elevation = dimensions().spacing0x,
                        animateNavigationIcon = true,
                        actions = {
                            val shouldShowActionIcon =
                                !isRecycleBin &&
                                        isOnline &&
                                        !isSelfUserViewerOnly &&
                                        !isSelectionMode
                            if (shouldShowActionIcon) {
                                MoreOptionIcon(
                                    contentDescription = R.string.content_description_conversation_files_more_button,
                                    onButtonClicked = { optionsBottomSheetState.show() }
                                )
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = isOnline && !isSelectionMode,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
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
                    }

                    if (isOnline) {
                        if (showViewerAccessBanner) {
                            ViewerAccessBanner(onCloseClick = onViewerAccessBannerCloseClick)
                        }

                        // Hidden while selecting; with fewer than two items there is nothing to select several of.
                        // Not in the recycle bin's folders: the tip isn't shown in the recycle bin either.
                        AnimatedVisibility(
                            visible = showBulkSelectionHint && !isRecycleBin && !isSelectionMode && pagingListItems.itemCount >= 2,
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                        ) {
                            BulkSelectionHint(onDismiss = onBulkSelectionHintDismiss)
                        }

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
                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    val fabCornerRadius by transition.animateDp(label = "FabCornerRadius") { state ->
                        if (state == EnterExitState.Visible) fabCornerRadius() else pillCornerRadius()
                    }
                    FloatingActionButton(
                        modifier = Modifier
                            .padding(
                                bottom = if (uploads.isNotEmpty()) dimensions().spacing72x else dimensions().spacing0x
                            )
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = fabToBulkActionsKey),
                                animatedVisibilityScope = this@AnimatedVisibility,
                                resizeMode = RemeasureToBounds,
                            ),
                        text = stringResource(R.string.cells_new_label),
                        shape = RoundedCornerShape(fabCornerRadius),
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
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                CellScreenContent(
                    modifier = Modifier.fillMaxSize(),
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
                    showBulkMoveToFolderScreen = navigation::moveNodes,
                    showRenameScreen = navigation::rename,
                    showAddRemoveTagsScreen = navigation::tags,
                    showVersionHistoryScreen = navigation::versionHistory,
                    showUploadStatusBottomSheet = { uploadStatusSheetState.show() },
                    showImageViewer = navigation::image,
                    showVideoViewer = navigation::video,
                    showAudioPlayer = navigation::audio,
                    showPdfViewer = navigation::pdf,
                    retryEditNodeError = { retryEditNodeError(it) },
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    fileReadyFlow = fileReadyFlow,
                    sortBy = sortingCriteria.by,
                    selectedNodeIds = selectedNodes.keys,
                    isSelectionEnabled = isBulkSelectionEnabled,
                )
                UploadStatusIndicator(
                    uploads = uploads,
                    onDismissAll = onDismissAllUploads,
                    onClick = { uploadStatusSheetState.show() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
                // Drawn over the list rather than as the scaffold's bottom bar, so showing or hiding it
                // doesn't resize the content and relayout the whole list on every animation frame.
                // Shares its bounds with the FAB, so the FAB morphs into the bar when selection starts and back when it ends.
                AnimatedVisibility(
                    visible = isSelectionMode,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = if (uploads.isNotEmpty()) dimensions().spacing72x else dimensions().spacing16x
                        ),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    // Mirrors the FAB: starts from the FAB corners and rounds up to a pill as it takes the FAB's place.
                    val barCornerRadius by transition.animateDp(label = "BulkActionsBarCornerRadius") { state ->
                        if (state == EnterExitState.Visible) pillCornerRadius() else fabCornerRadius()
                    }
                    CellBulkActionsBar(
                        actions = remember(offlineFilesEnabled, isRecycleBin) { bulkActions(offlineFilesEnabled, isRecycleBin) },
                        onActionClick = { action -> sendIntent(action.toIntent()) },
                        shape = RoundedCornerShape(barCornerRadius),
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = fabToBulkActionsKey),
                            animatedVisibilityScope = this@AnimatedVisibility,
                            resizeMode = RemeasureToBounds,
                        ),
                    )
                }
            }
        }
    }
}

private const val FAB_TO_BULK_ACTIONS_KEY = "cells_fab_to_bulk_actions"

// Extended FAB default corners (Material3 ShapeDefaults.Large).
@Composable
private fun fabCornerRadius(): Dp = dimensions().corner16x

// Larger than half of the bar height, so RoundedCornerShape clamps it to a full pill.
@Composable
private fun pillCornerRadius(): Dp = dimensions().spacing32x

// This screen also shows the folders inside the recycle bin, where only the recycle bin actions apply.
private fun bulkActions(offlineFilesEnabled: Boolean, isRecycleBin: Boolean): ImmutableList<CellBulkAction> =
    if (isRecycleBin) {
        persistentListOf(CellBulkAction.Restore, CellBulkAction.DeletePermanently)
    } else {
        listOfNotNull(
            CellBulkAction.Move,
            CellBulkAction.Delete,
            CellBulkAction.MakeAvailableOffline.takeIf { offlineFilesEnabled },
        ).toPersistentList()
    }

/**
 * Rolls the title up when the selection count grows and down when it shrinks, so entering selection mode
 * (0 -> 1), picking more items and leaving it (1 -> 0) all share one motion.
 */
@Composable
internal fun SelectionAwareTitle(title: String, selectedCount: Int) {
    AnimatedContent(
        targetState = selectedCount,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInVertically { height -> height * direction } + fadeIn())
                .togetherWith(slideOutVertically { height -> -height * direction } + fadeOut())
        },
        label = "SelectionAwareTitle",
    ) { count ->
        WireTopAppBarTitle(
            title = if (count > 0) pluralStringResource(R.plurals.cells_bulk_selection_count, count, count) else title,
            style = MaterialTheme.wireTypography.title01,
        )
    }
}

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
