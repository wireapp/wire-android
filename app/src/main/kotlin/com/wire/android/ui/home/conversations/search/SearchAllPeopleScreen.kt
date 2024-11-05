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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.SendConnectionRequestViewModel
import com.wire.android.ui.home.newconversation.SendConnectionRequestViewModelImpl
import com.wire.android.ui.home.newconversation.SendConnectionRequestViewModelPreview
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
import kotlinx.coroutines.launch

private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4

@Composable
fun SearchAllPeopleScreen(
    searchQuery: String,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
    contactsAddedToGroup: ImmutableSet<Contact>,
    isLoading: Boolean,
    isSearchActive: Boolean,
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
        )
    }
}

@Composable
private fun SearchResult(
    searchQuery: String,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
    isSearchActive: Boolean,
    contactsAddedToGroup: ImmutableSet<Contact>,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    sendConnectionRequestViewModel: SendConnectionRequestViewModel =
        if (LocalInspectionMode.current) SendConnectionRequestViewModelPreview
        else hiltViewModel<SendConnectionRequestViewModelImpl>(),
    lazyListState: LazyListState = rememberLazyListState()
) {
    val searchPeopleScreenState = rememberSearchPeopleScreenState()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val sendConnectionRequest: (UserId) -> Unit = remember {
        { userId: UserId ->
            val deferredResult = sendConnectionRequestViewModel.addContact(userId)
            deferredResult.invokeOnCompletion { throwable ->
                if (throwable != null) {
                    return@invokeOnCompletion
                }
                if (deferredResult.getCompleted()) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.connection_request_sent))
                    }
                }
            }
        }
    }
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
                    onChecked = onChecked,
                    searchResult = contactsSearchResult,
                    contactsAddedToGroup = contactsAddedToGroup,
                    showAllItems = !isSearchActive || searchPeopleScreenState.contactsAllResultsCollapsed,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllContactsResult,
                    onOpenUserProfile = onOpenUserProfile,
                )
            }

            if (publicSearchResult.isNotEmpty()) {
                externalSearchResults(
                    searchTitle = context.getString(R.string.label_public_wire),
                    searchQuery = searchQuery,
                    searchResult = publicSearchResult,
                    showAllItems = searchPeopleScreenState.publicResultsCollapsed,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllPublicResult,
                    onOpenUserProfile = onOpenUserProfile,
                    onAddContactClicked = sendConnectionRequest
                )
            }
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSearchResults(
    searchTitle: String,
    showAllItems: Boolean,
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
private fun LazyListScope.externalSearchResults(
    searchTitle: String,
    showAllItems: Boolean,
    searchResult: List<Contact>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onAddContactClicked: (UserId) -> Unit,
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
                onAddContactClicked = { onAddContactClicked(UserId(contact.id, contact.domain)) }
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
    SearchAllPeopleScreen("Search query", persistentListOf(), persistentListOf(), persistentSetOf(), true, false, { _, _ -> }, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_InitialResults() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true).toPersistentList()
    SearchAllPeopleScreen("", contacts, persistentListOf(), persistentSetOf(), false, false, { _, _ -> }, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_EmptyInitialResults() = WireTheme {
    SearchAllPeopleScreen("", persistentListOf(), persistentListOf(), persistentSetOf(),false, false, { _, _ -> }, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_SearchResults() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true).toPersistentList()
    val public = previewContactsList(count = 10, startIndex = 10, isContact = false).toPersistentList()
    SearchAllPeopleScreen("Con", contacts, public, persistentSetOf(), false, true, { _, _ -> }, {})
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_EmptySearchResults() = WireTheme {
    SearchAllPeopleScreen("Con", persistentListOf(), persistentListOf(), persistentSetOf(), false, true, { _, _ -> }, {})
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
