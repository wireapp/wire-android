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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.conversations.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.ItemActionType
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4

@Composable
fun SearchAllPeopleScreen(
    searchQuery: String,
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
    val emptyResults = contactsSearchResult.isEmpty() && publicSearchResult.isEmpty()
    when {
        isLoading -> CenteredCircularProgressBarIndicator()

        searchQuery.isBlank() && emptyResults -> EmptySearchQueryScreen()

        searchQuery.isNotBlank() && emptyResults -> SearchFailureBox(R.string.label_no_results_found)

        else -> SearchResult(
            searchQuery = searchQuery,
            publicSearchResult = publicSearchResult,
            contactsSearchResult = contactsSearchResult,
            contactsAddedToGroup = contactsAddedToGroup,
            onChecked = onChecked,
            onOpenUserProfile = onOpenUserProfile,
            lazyListState = lazyListState,
            isSearchActive = isSearchActive,
            actionType = actionType,
        )
    }
}

@Composable
private fun SearchResult(
    searchQuery: String,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
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
                    searchResult = contactsSearchResult,
                    allItemsVisible = !isSearchActive || searchPeopleScreenState.contactsAllResultsCollapsed,
                    showMoreOrLessButtonVisible = isSearchActive,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllContactsResult,
                    onOpenUserProfile = onOpenUserProfile,
                    actionType = actionType,
                )
            }

            if (publicSearchResult.isNotEmpty()) {
                externalSearchResults(
                    searchTitle = context.getString(R.string.label_public_wire),
                    searchQuery = searchQuery,
                    searchResult = publicSearchResult,
                    allItemsVisible = searchPeopleScreenState.publicResultsCollapsed,
                    showMoreOrLessButtonVisible = isSearchActive,
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
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    actionType: ItemActionType,
    contactsAddedToGroup: ImmutableSet<Contact>,
    onChecked: (Boolean, Contact) -> Unit,
    searchResult: ImmutableList<Contact>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit
) {
    if (searchResult.isNotEmpty()) {
        folderWithElements(
            header = searchTitle,
            items = (if (allItemsVisible) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)).associateBy {
                it.id
            }
        ) { contact ->
            with(contact) {
                val onClick = remember { { isChecked: Boolean -> onChecked(isChecked, this) } }
                InternalContactSearchResultItem(
                    avatarData = avatarData,
                    name = name,
                    label = label,
                    membership = membership,
                    searchQuery = searchQuery,
                    connectionState = connectionState,
                    isAddedToGroup = contactsAddedToGroup.any { it.id == contact.id },
                    onCheckChange = onClick,
                    actionType = actionType,
                    clickable = remember { Clickable(enabled = true) { onOpenUserProfile(contact) } }
                )
            }
        }
    }

    if (searchResult.size > DEFAULT_SEARCH_RESULT_ITEM_SIZE && showMoreOrLessButtonVisible) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                ShowButton(
                    isShownAll = allItemsVisible,
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
private fun LazyListScope.externalSearchResults(
    searchTitle: String,
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    searchResult: List<Contact>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
) {
    val itemsList =
        if (allItemsVisible) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)

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

    if (searchResult.size > DEFAULT_SEARCH_RESULT_ITEM_SIZE && showMoreOrLessButtonVisible) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                ShowButton(
                    isShownAll = allItemsVisible,
                    onShowButtonClicked = onShowAllButtonClicked,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = dimensions().spacing8x)
                )
            }
        }
    }
}

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

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_Loading() = WireTheme {
    SearchAllPeopleScreen(
        searchQuery = "",
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsAddedToGroup = persistentSetOf(),
        isLoading = true,
        isSearchActive = false,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_InitialResults() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true).toPersistentList()
    SearchAllPeopleScreen(
        searchQuery = "",
        contactsSearchResult = contacts,
        publicSearchResult = persistentListOf(),
        contactsAddedToGroup = persistentSetOf(),
        isLoading = false,
        isSearchActive = false,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_EmptyInitialResults() = WireTheme {
    SearchAllPeopleScreen(
        searchQuery = "",
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsAddedToGroup = persistentSetOf(),
        isLoading = false,
        isSearchActive = false,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_SearchResults_TypeClick() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true).toPersistentList()
    val public = previewContactsList(count = 10, startIndex = 10, isContact = false).toPersistentList()
    SearchAllPeopleScreen(
        searchQuery = "Con",
        contactsSearchResult = contacts,
        publicSearchResult = public,
        contactsAddedToGroup = persistentSetOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_SearchResults_TypeCheck() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true).toPersistentList()
    val public = previewContactsList(count = 10, startIndex = 10, isContact = false).toPersistentList()
    val selectedContacts = contacts.filterIndexed { index, _ -> index % 3 == 0 }.toPersistentSet()
    SearchAllPeopleScreen(
        searchQuery = "Con",
        contactsSearchResult = contacts,
        publicSearchResult = public,
        contactsAddedToGroup = selectedContacts,
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_EmptySearchResults() = WireTheme {
    SearchAllPeopleScreen(
        searchQuery = "Con",
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsAddedToGroup = persistentSetOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = {}
    )
}

private fun previewContact(index: Int, isContact: Boolean) = Contact(
    id = index.toString(),
    domain = "wire.com",
    name = "Contact nr $index",
    connectionState = if (isContact) ConnectionState.ACCEPTED else ConnectionState.NOT_CONNECTED,
    membership = Membership.Standard,
)

private fun previewContactsList(count: Int, startIndex: Int = 0, isContact: Boolean): List<Contact> =
    buildList { repeat(count) { index -> add(previewContact(startIndex + index, isContact)) } }
