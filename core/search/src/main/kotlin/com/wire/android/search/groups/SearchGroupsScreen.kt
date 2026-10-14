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
package com.wire.android.search.groups

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.wire.android.search.R
import com.wire.android.search.users.SearchUiState
import com.wire.android.search.widget.SearchFailureBox
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversations.search.EmptySearchQueryScreen
import com.wire.android.ui.common.rowitem.CollapsingSectionHeader
import com.wire.android.ui.home.conversationslist.common.ConversationItemTemplate
import com.wire.android.ui.home.conversationslist.common.ConversationItemSelectionMode
import com.wire.android.ui.home.conversationslist.common.ConversationTitle
import com.wire.android.util.ui.FolderType
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.sectionWithElements
import com.wire.android.util.ui.keepOnTopWhenNotScrolled
import com.wire.android.ui.common.R as commonR

@Composable
fun SearchGroupsScreen(
    query: String,
    selectedGroups: Set<SearchGroup>,
    onGroupChecked: (Boolean, SearchGroup) -> Unit,
    viewModel: SearchGroupsToAddMembersViewModel,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    LaunchedEffect(query) { viewModel.search(query) }
    val groups = viewModel.groups.collectAsLazyPagingItems()
    SearchGroupsContent(query, groups, selectedGroups, onGroupChecked, lazyListState)
}

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun SearchGroupsContent(
    query: String,
    groups: LazyPagingItems<SearchGroup>,
    selectedGroups: Set<SearchGroup>,
    onGroupChecked: (Boolean, SearchGroup) -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    var selectedGroupsExpanded by remember { mutableStateOf(false) }
    var availableGroupsExpanded by remember { mutableStateOf(true) }
    val matchingSelectedGroups = remember(query, selectedGroups) {
        selectedGroups.filter { it.name.contains(query, ignoreCase = true) }
    }
    val selectedGroupIds = remember(selectedGroups) { selectedGroups.map { it.id }.toSet() }
    val hasAvailableGroups = groups.itemSnapshotList.items.any { it.id !in selectedGroupIds }
    val noSearchResults = groups.itemCount == 0 && matchingSelectedGroups.isEmpty()
    val uiState = when {
        groups.loadState.refresh is LoadState.Loading && noSearchResults -> SearchUiState.Loading
        query.isBlank() && noSearchResults -> SearchUiState.EmptyQuery
        query.isNotBlank() && noSearchResults -> SearchUiState.NoResults
        else -> SearchUiState.ShowResults
    }
    when (uiState) {
        SearchUiState.Loading -> CenteredCircularProgressBarIndicator()
        SearchUiState.EmptyQuery -> EmptySearchQueryScreen(text = stringResource(R.string.label_search_groups_instruction))
        SearchUiState.NoResults -> SearchFailureBox(R.string.label_no_results_found)
        SearchUiState.ShowResults -> {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxHeight()
            ) {
                sectionWithElements(
                    header = UIText.StringResource(commonR.string.label_selected, matchingSelectedGroups.size),
                    items = matchingSelectedGroups.associateBy { it.id },
                    folderType = FolderType.Collapsing(selectedGroupsExpanded) { selectedGroupsExpanded = it },
                ) { group ->
                    SelectableGroupItem(group, query, true, onGroupChecked)
                }
                if (hasAvailableGroups) {
                    item(key = "available_groups_header") {
                        CollapsingSectionHeader(
                            name = stringResource(R.string.label_add_member_groups),
                            expanded = availableGroupsExpanded,
                            onClicked = { availableGroupsExpanded = it },
                            modifier = Modifier.fillMaxWidth().animateItem()
                        )
                    }
                }
                if (availableGroupsExpanded) {
                    items(count = groups.itemCount, key = groups.itemKey { it.id.toString() }) { index ->
                        // Access every row so paging can load more even when the current page is fully selected.
                        groups[index]?.takeIf { it.id !in selectedGroupIds }?.let { group ->
                            SelectableGroupItem(
                                group = group,
                                query = query,
                                selected = false,
                                onGroupChecked = onGroupChecked,
                                modifier = Modifier.fillMaxWidth().animateItem()
                            )
                        }
                    }
                }
            }
            SideEffect {
                keepOnTopWhenNotScrolled(lazyListState)
            }
        }
    }
}

@Composable
private fun SelectableGroupItem(
    group: SearchGroup,
    query: String,
    selected: Boolean,
    onGroupChecked: (Boolean, SearchGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    ConversationItemTemplate(
        conversationId = group.id,
        isChannel = group.isChannel,
        isPrivateChannel = group.isPrivateChannel,
        selectionMode = ConversationItemSelectionMode.MULTIPLE,
        isChecked = selected,
        onSelectionChanged = { onGroupChecked(it, group) },
        title = { ConversationTitle(name = group.name, searchQuery = query) },
        modifier = modifier,
    )
}
