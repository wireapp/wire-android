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
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.wire.android.model.ItemActionType
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4

@Composable
fun SearchAllPeopleScreen(
    searchQuery: String,
    noneSearchSucceed: Boolean,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
    contactsAddedToGroup: ImmutableSet<Contact>,
    isLoading: Boolean,
    isSearchActive: Boolean,
    actionType: ItemActionType,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    if (contactsSearchResult.isEmpty() && publicSearchResult.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        if (noneSearchSucceed) {
            SearchFailureBox(R.string.label_no_results_found)
        } else {
            Column {
                SearchResult(
                    searchQuery = searchQuery,
                    publicSearchResult = publicSearchResult,
                    contactsSearchResult = contactsSearchResult,
                    contactsAddedToGroup = contactsAddedToGroup,
                    onChecked = onChecked,
                    onOpenUserProfile = onOpenUserProfile,
                    lazyListState = lazyListState,
                    isSearchActive = isSearchActive,
                    isLoading = isLoading,
                    actionType = actionType,
                )
            }
        }
    }
}

@Composable
private fun SearchResult(
    searchQuery: String,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
    isLoading: Boolean,
    isSearchActive: Boolean,
    actionType: ItemActionType,
    contactsAddedToGroup: ImmutableSet<Contact>,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val searchPeopleScreenState = rememberSearchPeopleScreenState()
    val context = LocalContext.current

    Column {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
        ) {
            if (contactsSearchResult.isNotEmpty()) {
                internalSearchResults(
                    searchTitle = context.getString(R.string.label_contacts),
                    searchQuery = searchQuery,
                    contactsAddedToGroup = contactsAddedToGroup,
                    onChecked = onChecked,
                    isLoading = isLoading,
                    contactSearchResult = contactsSearchResult,
                    showAllItems = !isSearchActive || searchPeopleScreenState.contactsAllResultsCollapsed,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllContactsResult,
                    onOpenUserProfile = onOpenUserProfile,
                    actionType = actionType,
                )
            }

            if (publicSearchResult.isNotEmpty()) {
                externalSearchResults(
                    searchTitle = context.getString(R.string.label_public_wire),
                    searchQuery = searchQuery,
                    contactSearchResult = publicSearchResult,
                    isLoading = isLoading,
                    showAllItems = searchPeopleScreenState.publicResultsCollapsed,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllPublicResult,
                    onOpenUserProfile = onOpenUserProfile,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSearchResults(
    searchTitle: String,
    searchQuery: String,
    contactsAddedToGroup: ImmutableSet<Contact>,
    onChecked: (Boolean, Contact) -> Unit,
    actionType: ItemActionType,
    isLoading: Boolean,
    contactSearchResult: ImmutableList<Contact>,
    showAllItems: Boolean,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit
) {
    when {
        isLoading -> {
            inProgressItem()
        }

        else -> {
            internalSuccessItem(
                searchTitle = searchTitle,
                showAllItems = showAllItems,
                contactsAddedToGroup = contactsAddedToGroup,
                onChecked = onChecked,
                searchResult = contactSearchResult,
                searchQuery = searchQuery,
                onShowAllButtonClicked = onShowAllButtonClicked,
                onOpenUserProfile = onOpenUserProfile,
                actionType = actionType,
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.externalSearchResults(
    searchTitle: String,
    searchQuery: String,
    contactSearchResult: ImmutableList<Contact>,
    isLoading: Boolean,
    showAllItems: Boolean,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
) {
    when {
        isLoading -> {
            inProgressItem()
        }

        else -> {
            externalSuccessItem(
                searchTitle = searchTitle,
                showAllItems = showAllItems,
                searchResult = contactSearchResult,
                searchQuery = searchQuery,
                onShowAllButtonClicked = onShowAllButtonClicked,
                onOpenUserProfile = onOpenUserProfile,
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSuccessItem(
    searchTitle: String,
    showAllItems: Boolean,
    actionType: ItemActionType,
    contactsAddedToGroup: ImmutableSet<Contact>,
    onChecked: (Boolean, Contact) -> Unit,
    searchResult: ImmutableList<Contact>,
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
                val onClick = remember { { isChecked: Boolean -> onChecked(isChecked, this) } }
                InternalContactSearchResultItem(
                    avatarData = avatarData,
                    name = name,
                    label = label,
                    membership = membership,
                    searchQuery = searchQuery,
                    connectionState = connectionState,
                    isAddedToGroup = contactsAddedToGroup.contains(contact),
                    onCheckChange = onClick,
                    actionType = actionType,
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
                userId = UserId(id, domain),
                name = name,
                label = label,
                membership = membership,
                connectionState = connectionState,
                searchQuery = searchQuery,
                clickable = remember { Clickable(enabled = true) { onOpenUserProfile(contact) } }
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
