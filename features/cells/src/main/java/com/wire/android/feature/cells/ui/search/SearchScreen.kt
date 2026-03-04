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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.ramcosta.composedestinations.generated.cells.destinations.AddRemoveTagsScreenDestination
import com.ramcosta.composedestinations.generated.cells.destinations.MoveToFolderScreenDestination
import com.ramcosta.composedestinations.generated.cells.destinations.PublicLinkScreenDestination
import com.ramcosta.composedestinations.generated.cells.destinations.RenameNodeScreenDestination
import com.ramcosta.composedestinations.generated.cells.destinations.VersionHistoryScreenDestination
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.CellScreenContent
import com.wire.android.feature.cells.ui.CellViewModel
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.search.filter.FilterChipsRow
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.FilterByTypeBottomSheet
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.owner.FilterByOwnerBottomSheet
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.tags.FilterByTagsBottomSheet
import com.wire.android.feature.cells.ui.search.sort.SortRowWithMenu
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireCellsDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.navigation.transition.LocalSharedTransitionScope
import com.wire.android.navigation.transition.SHARED_ELEMENT_SEARCH_INPUT_KEY
import com.wire.android.ui.common.bottomsheet.WireSheetValue
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@WireCellsDestination(
    style = PopUpNavigationAnimation::class,
    navArgs = SearchNavArgs::class,
)
@Composable
fun SearchScreen(
    navigator: WireNavigator,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    searchScreenViewModel: SearchScreenViewModel = hiltViewModel(),
    cellViewModel: CellViewModel = hiltViewModel()
) {

    val uiState by searchScreenViewModel.uiState.collectAsStateWithLifecycle()

    val filterTypeSheetState = rememberWireModalSheetState<Unit>(WireSheetValue.Hidden)
    val filterTagsSheetState = rememberWireModalSheetState<Unit>(WireSheetValue.Hidden)
    val filterOwnerSheetState = rememberWireModalSheetState<Unit>(WireSheetValue.Hidden)

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
                Column {
                    SearchTopBar(
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = SHARED_ELEMENT_SEARCH_INPUT_KEY),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                        isSearchActive = uiState.isSearchActive,
                        searchBarHint = stringResource(R.string.search_shared_drive_text_input_hint),
                        searchQueryTextState = searchState,
                        onCloseSearchClicked = { navigator.navigateBack() },
                        onActiveChanged = { },
                    )
                    FilterChipsRow(
                        state = uiState.chipsState,
                        onFilterByTagsClicked = {
                            filterTagsSheetState.show(Unit, isImeVisible)
                        },
                        onFilterByTypeClicked = {
                            filterTypeSheetState.show(Unit, isImeVisible)
                        },
                        onFilterByOwnerClicked = {
                            filterOwnerSheetState.show(Unit, isImeVisible)
                        },
                        onFilterBySharedByLinkClicked = {
                            searchScreenViewModel.onSharedByMeClicked()
                        },
                        onRemoveAllFiltersClicked = {
                            searchScreenViewModel.onRemoveAllFilters()
                        }
                    )

                    with(searchScreenViewModel.uiState.collectAsState().value) {
                        SortRowWithMenu(
                            sortingCriteria = sortingCriteria,
                            isSearchResult = searchState.text.isNotEmpty() || uiState.hasAnyFilter,
                            onSortByClicked = {
                                searchScreenViewModel.setSortBy(it)
                            },
                            onOrderClicked = {
                                searchScreenViewModel.setSorting(it)
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            val lazyListState = rememberLazyListState()
            val lazyItems = searchScreenViewModel.cellNodesFlow.collectAsLazyPagingItems()

            LaunchedEffect(uiState.sortingCriteria) {
                lazyItems.refresh()
                // wait for refresh to complete
                snapshotFlow { lazyItems.loadState.refresh }
                    .first { it is androidx.paging.LoadState.NotLoading }
                lazyListState.animateScrollToItem(0)
            }

            CellScreenContent(
                lazyListState = lazyListState,
                isPullToRefreshEnabled = false,
                modifier = Modifier.padding(innerPadding),
                actionsFlow = cellViewModel.actions,
                pagingListItems = lazyItems,
                sendIntent = { cellViewModel.sendIntent(it) },
                downloadFileState = cellViewModel.downloadFileSheet,
                menuState = cellViewModel.menu,
                isSearchResult = true,
                isRestoreInProgress = cellViewModel.isRestoreInProgress.collectAsState().value,
                isDeleteInProgress = cellViewModel.isDeleteInProgress.collectAsState().value,
                openFolder = { _, _, _ -> },
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
                retryEditNodeError = { cellViewModel.editNode(it) },
                isRefreshing = remember { mutableStateOf(false) },
                onRefresh = { }
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
        }
    }
}
