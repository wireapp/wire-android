/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.feature.cells.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.CellScreenContent
import com.wire.android.feature.cells.ui.CellFilesNavArgs
import com.wire.android.feature.cells.ui.CellViewModel
import com.wire.android.feature.cells.ui.common.OfflineBanner
import com.wire.android.feature.cells.ui.search.filter.FilterChipsRow
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.FilterByTypeBottomSheet
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.conversation.FilterByConversationBottomSheet
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.owner.FilterByOwnerBottomSheet
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.tags.FilterByTagsBottomSheet
import com.wire.android.feature.cells.ui.search.sort.SortRowWithMenu
import com.wire.android.feature.cells.ui.searchScreenViewModel
import com.wire.android.navigation.transition.LocalSharedTransitionScope
import com.wire.android.navigation.transition.SHARED_ELEMENT_SEARCH_INPUT_KEY
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SearchRouteScreen(
    navigation: com.wire.android.feature.cells.ui.CellsFilesNavigation,
    animatedVisibilityScope: AnimatedVisibilityScope,
    cellViewModel: CellViewModel,
    searchScreenViewModel: SearchScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by searchScreenViewModel.uiState.collectAsStateWithLifecycle()
    val isOnlineState by cellViewModel.isOnline.collectAsState()
    // When offline files are disabled, never enter offline mode so all offline UI stays hidden.
    val isOnline = isOnlineState || !cellViewModel.offlineFilesEnabled

    val filterTypeSheetState = rememberWireModalSheetState<Unit>(WireSheetValue.Hidden)
    val filterTagsSheetState = rememberWireModalSheetState<Unit>(WireSheetValue.Hidden)
    val filterOwnerSheetState = rememberWireModalSheetState<Unit>(WireSheetValue.Hidden)
    val filterConversationSheetState = rememberWireModalSheetState<Unit>(WireSheetValue.Hidden)

    val isImeVisible = WindowInsets.isImeVisible

    val sharedScope = LocalSharedTransitionScope.current

    val searchState = remember { TextFieldState() }

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect { searchScreenViewModel.onSearchQueryChanged(it) }
    }

    with(sharedScope) {

        WireScaffold(
            modifier = modifier,
            topBar = {
                AnimatedContent(isOnline) { online ->
                    if (online) {
                        Column {
                            SearchTopBar(
                                modifier = Modifier.sharedElement(
                                    sharedContentState = rememberSharedContentState(key = SHARED_ELEMENT_SEARCH_INPUT_KEY),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                                isSearchActive = uiState.isSearchActive,
                                shouldClearTextOnClearFocus = false,
                                keepBackButtonVisible = true,
                                searchBarHint = when (searchScreenViewModel.screenType) {
                                    DriveSearchScreenType.SHARED_DRIVE -> stringResource(R.string.search_shared_drive_text_input_hint)
                                    DriveSearchScreenType.DRIVE -> stringResource(R.string.search_drive_text_input_hint)
                                },
                                searchQueryTextState = searchState,
                                onCloseSearchClicked = navigation::back,
                                onActiveChanged = {
                                    searchScreenViewModel.onSetSearchActive(it)
                                },
                            )
                            FilterChipsRow(
                                state = uiState.chipsState,
                                screenType = searchScreenViewModel.screenType,
                                onFilterByTagsClicked = {
                                    searchScreenViewModel.onSetSearchActive(false)
                                    filterTagsSheetState.show(Unit, isImeVisible)
                                },
                                onFilterByTypeClicked = {
                                    searchScreenViewModel.onSetSearchActive(false)
                                    filterTypeSheetState.show(Unit, isImeVisible)
                                },
                                onFilterByOwnerClicked = {
                                    searchScreenViewModel.onSetSearchActive(false)
                                    filterOwnerSheetState.show(Unit, isImeVisible)
                                },
                                onFilterBySharedByLinkClicked = {
                                    searchScreenViewModel.onSharedByMeClicked()
                                },
                                onFilterByConversationClicked = {
                                    searchScreenViewModel.onSetSearchActive(false)
                                    filterConversationSheetState.show(Unit, isImeVisible)
                                },
                                onRemoveAllFiltersClicked = {
                                    searchScreenViewModel.onRemoveAllFilters()
                                }
                            )

                            with(uiState) {
                                SortRowWithMenu(
                                    screenType = searchScreenViewModel.screenType,
                                    sortingCriteria = sortingCriteria,
                                    isSearchResult = searchState.text.isNotEmpty() || hasAnyFilter,
                                    onSortByClicked = {
                                        searchScreenViewModel.setSortBy(it)
                                    },
                                    onOrderClicked = {
                                        searchScreenViewModel.setSorting(it)
                                    }
                                )
                            }
                        }
                    } else {
                        Column {
                            WireCenterAlignedTopAppBar(
                                title = "",
                                navigationIconType = NavigationIconType.Close(),
                                onNavigationPressed = navigation::back,
                            )
                            OfflineBanner()
                        }
                    }
                }
            },
        ) { innerPadding ->
            val lazyListState = rememberLazyListState()

            val isShowingFilteredResults = uiState.hasAnyFilter ||
                    searchState.text.isNotEmpty() ||
                    uiState.sortingCriteria != searchScreenViewModel.inheritedSortingCriteria
            val initialItems = cellViewModel.nodesFlow.collectAsLazyPagingItems()
            val filteredItems = searchScreenViewModel.cellNodesFlow.collectAsLazyPagingItems()
            val lazyItems = if (isShowingFilteredResults) filteredItems else initialItems

            LaunchedEffect(uiState.sortingCriteria) {
                lazyListState.animateScrollToItem(0)
            }

            CellScreenContent(
                lazyListState = lazyListState,
                isPullToRefreshEnabled = false,
                modifier = Modifier.padding(innerPadding),
                actionsFlow = cellViewModel.actions,
                pagingListItems = lazyItems,
                sendIntent = { cellViewModel.sendIntent(it) },
                menuState = cellViewModel.menu,
                isSearchResult = true,
                isRestoreInProgress = cellViewModel.isRestoreInProgress.collectAsState().value,
                isDeleteInProgress = cellViewModel.isDeleteInProgress.collectAsState().value,
                openFolder = { path, title, parentFolderUuid ->
                    navigation.folder(
                        CellFilesNavArgs(
                            conversationId = path,
                            screenTitle = title,
                            parentFolderUuid = parentFolderUuid,
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
                showPdfViewer = navigation::pdf,
                retryEditNodeError = { cellViewModel.editNode(it) },
                isRefreshing = remember { mutableStateOf(false) },
                onRefresh = { },
                fileReadyFlow = cellViewModel.fileReadyFlow,
            )
        }
    }

    FilterByTagsBottomSheet(
        items = uiState.availableTags,
        sheetState = filterTagsSheetState,
        onDismiss = {
            filterTagsSheetState.hide()
        },
        onSave = { selectedItems ->
            searchScreenViewModel.onSaveTags(selectedItems)
            filterTagsSheetState.hide()
        },
        onRemoveAll = {
            searchScreenViewModel.onRemoveAllTags()
        }
    )

    FilterByTypeBottomSheet(
        items = uiState.availableTypes,
        sheetState = filterTypeSheetState,
        onDismiss = {
            filterTypeSheetState.hide()
        },
        onSave = { selectedItems ->
            searchScreenViewModel.onSaveTypes(selectedItems)
            filterTypeSheetState.hide()
        },
        onRemoveFilter = {
            searchScreenViewModel.onRemoveTypeFilter()
        }
    )

    FilterByOwnerBottomSheet(
        items = uiState.availableOwners,
        sheetState = filterOwnerSheetState,
        onDismiss = {
            filterOwnerSheetState.hide()
        },
        onSave = { selectedItems ->
            searchScreenViewModel.onSaveOwners(selectedItems)
            filterOwnerSheetState.hide()
        },
        onRemoveAll = { searchScreenViewModel.onRemoveOwners() }
    )

    val lazyConversations = searchScreenViewModel.conversationsFlow.collectAsLazyPagingItems()

    FilterByConversationBottomSheet(
        sheetState = filterConversationSheetState,
        conversations = lazyConversations,
        selectedConversation = uiState.selectedConversation,
        onSearchQueryChanged = { searchScreenViewModel.onConversationSearchQueryChanged(it) },
        onDismiss = {
            filterConversationSheetState.hide()
        },
        onRemoveAll = {
            searchScreenViewModel.onRemoveConversations()
        },
        onSave = { selectedConversation ->
            searchScreenViewModel.onSaveConversation(selectedConversation)
            filterConversationSheetState.hide()
        }
    )
}
