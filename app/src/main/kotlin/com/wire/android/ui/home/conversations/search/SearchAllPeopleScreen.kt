/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.home.conversations.search

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.PreviewMultipleThemes

private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4

@Composable
fun SearchAllPeopleScreen(
    searchQuery: String,
    noneSearchSucceed: Boolean,
    searchResult: Map<SearchResultTitle, ContactSearchResult>,
    contactsAddedToGroup: List<Contact>,
    onAddToGroup: (Contact) -> Unit,
    onRemoveFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onAddContactClicked: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    if (searchQuery.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        if (noneSearchSucceed) {
            SearchFailureBox(R.string.label_no_results_found)
        } else {
            Column {
                SearchResult(
                    searchQuery = searchQuery,
                    searchResult = searchResult,
                    contactsAddedToGroup = contactsAddedToGroup,
                    onAddToGroup = onAddToGroup,
                    onRemoveContactFromGroup = onRemoveFromGroup,
                    onOpenUserProfile = onOpenUserProfile,
                    onAddContactClicked = onAddContactClicked,
                    lazyListState = lazyListState
                )
            }
        }
    }
}

@Composable
private fun SearchResult(
    searchQuery: String,
    searchResult: Map<SearchResultTitle, ContactSearchResult>,
    contactsAddedToGroup: List<Contact>,
    onAddToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onAddContactClicked: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val searchPeopleScreenState = rememberSearchPeopleScreenState()
    val context = LocalContext.current

    Column {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f),
        ) {
            searchResult.forEach { (searchTitle, result) ->
                when (result) {
                    is ContactSearchResult.ExternalContact -> {
                        externalSearchResults(
                            searchTitle = context.getString(searchTitle.stringRes),
                            searchQuery = searchQuery,
                            contactSearchResult = result,
                            showAllItems = searchPeopleScreenState.publicResultsCollapsed,
                            onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllPublicResult,
                            onOpenUserProfile = onOpenUserProfile,
                            onAddContactClicked = onAddContactClicked
                        )
                    }

                    is ContactSearchResult.InternalContact -> {
                        internalSearchResults(
                            searchTitle = context.getString(searchTitle.stringRes),
                            searchQuery = searchQuery,
                            contactsAddedToGroup = contactsAddedToGroup,
                            onAddToGroup = onAddToGroup,
                            removeFromGroup = onRemoveContactFromGroup,
                            contactSearchResult = result,
                            showAllItems = searchPeopleScreenState.contactsAllResultsCollapsed,
                            onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllContactsResult,
                            onOpenUserProfile = onOpenUserProfile,
                        )
                    }
                }
            }
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSearchResults(
    searchTitle: String,
    searchQuery: String,
    contactsAddedToGroup: List<Contact>,
    onAddToGroup: (Contact) -> Unit,
    removeFromGroup: (Contact) -> Unit,
    contactSearchResult: ContactSearchResult,
    showAllItems: Boolean,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit
) {
    when (val searchResult = contactSearchResult.searchResultState) {
        SearchResultState.InProgress -> {
            inProgressItem()
        }

        is SearchResultState.Success -> {
            internalSuccessItem(
                searchTitle = searchTitle,
                showAllItems = showAllItems,
                contactsAddedToGroup = contactsAddedToGroup,
                onAddToGroup = onAddToGroup,
                removeFromGroup = removeFromGroup,
                searchResult = searchResult.result,
                searchQuery = searchQuery,
                onShowAllButtonClicked = onShowAllButtonClicked,
                onOpenUserProfile = onOpenUserProfile
            )
        }

        is SearchResultState.Failure -> {
            failureItem(
                failureMessage = searchResult.failureString
            )
        }
        // We do not display anything on Initial or Empty state
        SearchResultState.Initial, SearchResultState.EmptyResult -> { }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.externalSearchResults(
    searchTitle: String,
    searchQuery: String,
    contactSearchResult: ContactSearchResult,
    showAllItems: Boolean,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onAddContactClicked: (Contact) -> Unit
) {
    when (val searchResult = contactSearchResult.searchResultState) {
        SearchResultState.InProgress -> {
            inProgressItem()
        }

        is SearchResultState.Success -> {
            externalSuccessItem(
                searchTitle = searchTitle,
                showAllItems = showAllItems,
                searchResult = searchResult.result,
                searchQuery = searchQuery,
                onShowAllButtonClicked = onShowAllButtonClicked,
                onOpenUserProfile = onOpenUserProfile,
                onAddContactClicked = onAddContactClicked
            )
        }

        is SearchResultState.Failure -> {
            failureItem(
                failureMessage = searchResult.failureString
            )
        }
        // We do not display anything on Initial or Empty state
        SearchResultState.Initial, SearchResultState.EmptyResult -> {
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSuccessItem(
    searchTitle: String,
    showAllItems: Boolean,
    contactsAddedToGroup: List<Contact>,
    onAddToGroup: (Contact) -> Unit,
    removeFromGroup: (Contact) -> Unit,
    searchResult: List<Contact>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit
) {
    if (searchResult.isNotEmpty()) {
        folderWithElements(header = searchTitle,
            items = (if (showAllItems) searchResult else searchResult.take(
                DEFAULT_SEARCH_RESULT_ITEM_SIZE
            ))
                .associateBy { it.id }) { contact ->
            with(contact) {
                InternalContactSearchResultItem(
                    avatarData = avatarData,
                    name = name,
                    label = label,
                    membership = membership,
                    searchQuery = searchQuery,
                    connectionState = connectionState,
                    isAddedToGroup = contactsAddedToGroup.contains(contact),
                    addToGroup = { onAddToGroup(contact) },
                    removeFromGroup = { removeFromGroup(contact) },
                    clickable = remember { Clickable(enabled = true) { onOpenUserProfile(contact) } }
                )
            }
        }
    }

    if (searchResult.size > DEFAULT_SEARCH_RESULT_ITEM_SIZE) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                ShowButton(
                    isShownAll = showAllItems,
                    onShowButtonClicked = onShowAllButtonClicked,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = dimensions().spacing8x)
                )
            }
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.externalSuccessItem(
    searchTitle: String,
    showAllItems: Boolean,
    searchResult: List<Contact>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onAddContactClicked: (Contact) -> Unit,
) {
    val itemsList =
        if (showAllItems) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)

    folderWithElements(
        header = searchTitle,
        items = itemsList.associateBy { it.id }
    ) { contact ->
        with(contact) {
            ExternalContactSearchResultItem(
                avatarData = avatarData,
                name = name,
                label = label,
                membership = membership,
                connectionState = connectionState,
                searchQuery = searchQuery,
                clickable = remember { Clickable(enabled = true) { onOpenUserProfile(contact) } },
                onAddContactClicked = { onAddContactClicked(contact) }
            )
        }
    }

    if (searchResult.size > DEFAULT_SEARCH_RESULT_ITEM_SIZE) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                ShowButton(
                    isShownAll = showAllItems,
                    onShowButtonClicked = onShowAllButtonClicked,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = dimensions().spacing8x)
                )
            }
        }
    }
}

fun LazyListScope.inProgressItem() {
    item {
        Box(
            Modifier
                .fillMaxWidth()
                .height(224.dp)
        ) {
            WireCircularProgressIndicator(
                progressColor = Color.Black,
                modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }
    }
}

fun LazyListScope.failureItem(@StringRes failureMessage: Int) {
    item {
        SearchFailureBox(failureMessage)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ShowButton(
    isShownAll: Boolean,
    onShowButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        AnimatedContent(isShownAll) { showAll ->
            WireSecondaryButton(
                text = if (!showAll) stringResource(R.string.label_show_more) else stringResource(R.string.label_show_less),
                onClick = onShowButtonClicked,
                minSize = dimensions().buttonSmallMinSize,
                minClickableSize = dimensions().buttonMinClickableSize,
                fillMaxWidth = false,
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewShowButton() {
    WireTheme {
        ShowButton(isShownAll = false, onShowButtonClicked = {})
    }
}
