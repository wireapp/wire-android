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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.feature.cells.R
import com.wire.android.feature.cells.ui.CellScreenContent
import com.wire.android.feature.cells.ui.CellViewModel
import com.wire.android.feature.cells.ui.LocalSharedTransitionScope
import com.wire.android.feature.cells.ui.destinations.AddRemoveTagsScreenDestination
import com.wire.android.feature.cells.ui.destinations.MoveToFolderScreenDestination
import com.wire.android.feature.cells.ui.destinations.PublicLinkScreenDestination
import com.wire.android.feature.cells.ui.destinations.RenameNodeScreenDestination
import com.wire.android.feature.cells.ui.destinations.VersionHistoryScreenDestination
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.search.filter.FilterChipsRow
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.owner.FilterByOwnerBottomSheet
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.tags.FilterByTagsBottomSheet
import com.wire.android.feature.cells.ui.search.filter.bottomsheet.FilterByTypeBottomSheet
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.WireNavigator
import com.wire.android.navigation.annotation.features.cells.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val sharedElementSearchInputKey = "search_bar"

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@WireDestination(
    style = PopUpNavigationAnimation::class,
    navArgsDelegate = SearchNavArgs::class,
)
@Composable
fun SearchScreen(
    navigator: WireNavigator,
    animatedVisibilityScope: AnimatedVisibilityScope,
    searchScreenViewModel: SearchScreenViewModel = hiltViewModel(),
    cellViewModel: CellViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    cellViewModel.isSearchByDefaultActive

    val uiState by searchScreenViewModel.uiState.collectAsStateWithLifecycle()

    val filterTypeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterTagsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterOwnerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun closeSheet(sheetState: SheetState, onCloseFlag: () -> Unit) {
        scope.launch {
            sheetState.hide()
            onCloseFlag()
            searchScreenViewModel.onSetSearchActive(true)
        }
    }

    fun openSheet(onOpenFlag: () -> Unit = { }) {
        scope.launch {
            focusManager.clearFocus()
            onOpenFlag()
            searchScreenViewModel.onSetSearchActive(false)
        }
    }

    val sharedScope = LocalSharedTransitionScope.current
    var playScrollHint by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1500)
        playScrollHint = true
        delay(2000)
        playScrollHint = false
    }

    val searchState = remember { TextFieldState() }

    LaunchedEffect(searchState) {
        snapshotFlow { searchState.text.toString() }
            .collect { searchScreenViewModel.onSearchQueryChanged(it) }
    }


    with(sharedScope) {

        WireScaffold(
            topBar = {
                Column {
                    SearchTopBar(
                        modifier = Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = sharedElementSearchInputKey),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                        isSearchActive = uiState.isSearchActive,
                        searchBarHint = stringResource(R.string.search_shared_drive_text_input_hint),
                        searchQueryTextState = searchState,
                        onCloseSearchClicked = { navigator.navigateBack() },
                        onActiveChanged = { },
                        focusRequester = focusRequester,
                        focusManager = focusManager
                    )
                    FilterChipsRow(
                        shouldPlayHint = playScrollHint,
                        isSharedByLinkSelected = uiState.isSharedByMe,
                        tagsCount = uiState.tagsCount,
                        typeCount = uiState.typeCount,
                        ownerCount = uiState.ownerCount,
                        hasAnyFilter = uiState.hasAnyFilter,
                        onFilterByTagsClicked = {
                            openSheet { searchScreenViewModel.onFilterByTagsClicked() }
                        },
                        onFilterByTypeClicked = {
                            openSheet { searchScreenViewModel.onFilterByTypeClicked() }
                        },
                        onFilterByOwnerClicked = {
                            openSheet { searchScreenViewModel.onFilterByOwnerClicked() }
                        },
                        onFilterBySharedByLinkClicked = {
                            searchScreenViewModel.onSharedByMeClicked()
                        },
                        onRemoveAllFiltersClicked = {
                            searchScreenViewModel.onRemoveAllFilters()
                        }
                    )
                }
            }
        ) { innerPadding ->
            with(searchScreenViewModel.cellNodesFlow.collectAsLazyPagingItems()) {
                CellScreenContent(
                    modifier = Modifier.padding(innerPadding),
                    actionsFlow = cellViewModel.actions,
                    pagingListItems = this,
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

            if (uiState.showFilterByTags) {
                FilterByTagsBottomSheet(
                    items = searchScreenViewModel.uiState.collectAsState().value.availableTags,
                    sheetState = filterTagsSheetState,
                    onDismiss = {
                        closeSheet(
                            sheetState = filterTagsSheetState,
                            onCloseFlag = { searchScreenViewModel.onCloseTagsSheet() }
                        )
                    },
                    onSave = { selectedItems ->
                        searchScreenViewModel.onSaveTags(selectedItems)
                        closeSheet(
                            sheetState = filterTagsSheetState,
                            onCloseFlag = { searchScreenViewModel.onCloseTagsSheet() }
                        )
                    },
                    onRemoveAll = {
                        searchScreenViewModel.onRemoveAllTags()
                    }
                )
            }

            if (uiState.showFilterByType) {
                FilterByTypeBottomSheet(
                    items = searchScreenViewModel.uiState.collectAsState().value.availableTypes,
                    sheetState = filterTypeSheetState,
                    onDismiss = {
                        closeSheet(
                            sheetState = filterTypeSheetState,
                            onCloseFlag = { searchScreenViewModel.onCloseTypeSheet() }
                        )
                    },
                    onSave = { selectedItems ->

                        searchScreenViewModel.onSaveTypes(selectedItems)
                        closeSheet(
                            sheetState = filterTypeSheetState,
                            onCloseFlag = { searchScreenViewModel.onCloseTypeSheet() }
                        )
                    },
                    onRemoveFilter = {
                        searchScreenViewModel.onRemoveTypeFilter()
                    }
                )
            }

            if (uiState.showFilterByOwner) {
                FilterByOwnerBottomSheet(
                    items = searchScreenViewModel.uiState.collectAsState().value.availableOwners,
                    sheetState = filterOwnerSheetState,
                    onDismiss = {
                        closeSheet(
                            sheetState = filterOwnerSheetState,
                            onCloseFlag = { searchScreenViewModel.onCloseOwnerSheet() }
                        )
                    },
                    onSave = { selectedItems ->

                        searchScreenViewModel.onSaveOwners(selectedItems)

                        closeSheet(
                            sheetState = filterOwnerSheetState,
                            onCloseFlag = { searchScreenViewModel.onCloseOwnerSheet() }
                        )
                    },
                    onRemoveAll = { searchScreenViewModel.onRemoveOwners() }
                )
            }
        }
    }
}
