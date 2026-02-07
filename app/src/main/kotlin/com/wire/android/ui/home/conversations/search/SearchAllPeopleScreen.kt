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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.wire.android.util.ui.FolderType
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.keepOnTopWhenNotScrolled
import com.wire.android.util.ui.sectionWithElements
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList

private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4

/**
 * state of the search screen
 * Loading - loading state
 * EmptyQuery - empty search query, show all users
 * NoResults - no results found
 * ShowResults - search results are found
 */
private sealed interface SearchUiState {
    object Loading : SearchUiState
    object EmptyQuery : SearchUiState
    object NoResults : SearchUiState
    object ShowResults : SearchUiState
}

@Composable
fun SearchAllPeopleScreen(
    searchQuery: String,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
    contactsSelectedSearchResult: ImmutableList<Contact>,
    isLoading: Boolean,
    isSearchActive: Boolean,
    actionType: ItemActionType,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    selectedContactResultsExpanded: Boolean = false,
    onSelectedContactResultsExpansionChanged: (Boolean) -> Unit = {},
    contactResultsExpanded: Boolean = true,
    onContactResultsExpansionChanged: (Boolean) -> Unit = {},
    publicResultsExpanded: Boolean = true,
    onPublicResultsExpansionChanged: (Boolean) -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState()
) {

    val noSearchResults: Boolean by remember(publicSearchResult, contactsSearchResult, contactsSelectedSearchResult) {
        derivedStateOf {
            publicSearchResult.isEmpty() &&
                    contactsSearchResult.isEmpty() &&
                    contactsSelectedSearchResult.isEmpty()
        }
    }

    val uiState by remember(searchQuery, isLoading, noSearchResults) {
        derivedStateOf {
            when {
                isLoading -> SearchUiState.Loading
                searchQuery.isBlank() && noSearchResults -> SearchUiState.EmptyQuery
                searchQuery.isNotBlank() && noSearchResults -> SearchUiState.NoResults
                else -> SearchUiState.ShowResults
            }
        }
    }

    when (uiState) {
        SearchUiState.Loading -> CenteredCircularProgressBarIndicator()
        SearchUiState.EmptyQuery -> EmptySearchQueryScreen()
        SearchUiState.NoResults -> SearchFailureBox(R.string.label_no_results_found)
        SearchUiState.ShowResults -> SearchResult(
            searchQuery = searchQuery,
            publicSearchResult = publicSearchResult,
            contactsSearchResult = contactsSearchResult,
            contactsSelectedSearchResult = contactsSelectedSearchResult,
            onChecked = onChecked,
            onOpenUserProfile = onOpenUserProfile,
            lazyListState = lazyListState,
            isSearchActive = isSearchActive,
            actionType = actionType,
            selectedContactResultsExpanded = selectedContactResultsExpanded,
            onSelectedContactResultsExpansionChanged = onSelectedContactResultsExpansionChanged,
            contactResultsExpanded = contactResultsExpanded,
            onContactResultsExpansionChanged = onContactResultsExpansionChanged,
            publicResultsExpanded = publicResultsExpanded,
            onPublicResultsExpansionChanged = onPublicResultsExpansionChanged,
        )
    }
}

@Composable
private fun SearchResult(
    searchQuery: String,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
    contactsSelectedSearchResult: ImmutableList<Contact>,
    isSearchActive: Boolean,
    actionType: ItemActionType,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    selectedContactResultsExpanded: Boolean,
    onSelectedContactResultsExpansionChanged: (Boolean) -> Unit,
    contactResultsExpanded: Boolean,
    onContactResultsExpansionChanged: (Boolean) -> Unit,
    publicResultsExpanded: Boolean,
    onPublicResultsExpansionChanged: (Boolean) -> Unit,
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
            if (contactsSelectedSearchResult.isNotEmpty()) { // selected contacts section filtered by search query
                internalSearchResults(
                    searchTitle = context.getString(R.string.label_selected) + " (${contactsSelectedSearchResult.size})",
                    searchQuery = searchQuery,
                    onChecked = onChecked,
                    searchResult = contactsSelectedSearchResult.map { it to true }.toImmutableList(),
                    allItemsVisible = true, // for selected contacts we always show all items
                    showMoreOrLessButtonVisible = false,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllContactsResult,
                    onOpenUserProfile = onOpenUserProfile,
                    actionType = actionType,
                    expanded = selectedContactResultsExpanded,
                    onExpansionChanged = onSelectedContactResultsExpansionChanged,
                )
            }

            if (contactsSearchResult.isNotEmpty()) { // non-selected contacts section filtered by search query
                internalSearchResults(
                    searchTitle = UIText.StringResource(R.string.label_contacts),
                    searchQuery = searchQuery,
                    onChecked = onChecked,
                    searchResult = contactsSearchResult.map { it to false }.toImmutableList(),
                    allItemsVisible = !isSearchActive || searchPeopleScreenState.contactsAllResultsCollapsed,
                    showMoreOrLessButtonVisible = isSearchActive,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllContactsResult,
                    onOpenUserProfile = onOpenUserProfile,
                    actionType = actionType,
                    expanded = contactResultsExpanded,
                    onExpansionChanged = onContactResultsExpansionChanged,
                )
            }

            if (publicSearchResult.isNotEmpty()) { // public results section filtered by search query
                externalSearchResults(
                    searchTitle = UIText.StringResource(R.string.label_public_wire),
                    searchQuery = searchQuery,
                    searchResult = publicSearchResult,
                    allItemsVisible = searchPeopleScreenState.publicResultsCollapsed,
                    showMoreOrLessButtonVisible = isSearchActive,
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllPublicResult,
                    onOpenUserProfile = onOpenUserProfile,
                    expanded = publicResultsExpanded,
                    onExpansionChanged = onPublicResultsExpansionChanged,
                )
            }
        }

        SideEffect {
            keepOnTopWhenNotScrolled(lazyListState)
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSearchResults(
    searchTitle: UIText,
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    actionType: ItemActionType,
    onChecked: (Boolean, Contact) -> Unit,
    searchResult: ImmutableList<Pair<Contact, Boolean>>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onExpansionChanged: (Boolean) -> Unit,
    expanded: Boolean,
) {
    if (searchResult.isNotEmpty()) {
        sectionWithElements(
            header = searchTitle,
            items = (if (allItemsVisible) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE))
                .associateBy { (contact, _) ->
                    contact.id
                },
            folderType = FolderType.Collapsing(expanded = expanded, onChanged = onExpansionChanged),
        ) { (contact, isSelected) ->
            with(contact) {
                val onCheckDescription = stringResource(
                    id = if (isSelected) R.string.content_description_unselect_label else R.string.content_description_select_label
                )
                val clickDescription = stringResource(id = R.string.content_description_open_user_profile_label)
                val onCheckClickable = remember(isSelected) {
                    Clickable(onClickDescription = onCheckDescription) { onChecked(!isSelected, this) }
                }
                InternalContactSearchResultItem(
                    avatarData = avatarData,
                    name = name,
                    label = label,
                    membership = membership,
                    searchQuery = searchQuery,
                    connectionState = connectionState,
                    isSelected = isSelected,
                    onCheckClickable = onCheckClickable,
                    actionType = actionType,
                    clickable = remember { Clickable(onClickDescription = clickDescription) { onOpenUserProfile(contact) } }
                )
            }
        }
    }

    if (searchResult.size > DEFAULT_SEARCH_RESULT_ITEM_SIZE && showMoreOrLessButtonVisible && expanded) {
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
    searchTitle: UIText,
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    searchResult: List<Contact>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    expanded: Boolean,
    onExpansionChanged: (Boolean) -> Unit,
) {
    val itemsList =
        if (allItemsVisible) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)

    sectionWithElements(
        header = searchTitle,
        items = itemsList.associateBy { it.id },
        folderType = FolderType.Collapsing(expanded = expanded, onChanged = onExpansionChanged),
    ) { contact ->
        with(contact) {
            val clickDescription = stringResource(id = R.string.content_description_open_user_profile_label)
            ExternalContactSearchResultItem(
                avatarData = avatarData,
                userId = UserId(id, domain),
                name = name,
                label = label,
                membership = membership,
                connectionState = connectionState,
                searchQuery = searchQuery,
                clickable = remember { Clickable(onClickDescription = clickDescription) { onOpenUserProfile(contact) } }
            )
        }
    }
    if (searchResult.size > DEFAULT_SEARCH_RESULT_ITEM_SIZE && showMoreOrLessButtonVisible && expanded) {
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
        searchQuery = "Search query",
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = true,
        isSearchActive = true,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_InitialResults() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true)
    SearchAllPeopleScreen(
        searchQuery = "",
        contactsSearchResult = contacts,
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_EmptyInitialResults() = WireTheme {
    SearchAllPeopleScreen(
        searchQuery = "",
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = false,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_SearchResults_TypeClick() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true)
    val public = previewContactsList(count = 10, startIndex = 10, isContact = false)
    SearchAllPeopleScreen(
        searchQuery = "Con",
        contactsSearchResult = contacts,
        publicSearchResult = public,
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_SearchResults_TypeCheck() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true)
    val public = previewContactsList(count = 10, startIndex = 10, isContact = false)
    val selectedContacts = previewContactsList(count = 5, startIndex = 20, isContact = true)
    SearchAllPeopleScreen(
        searchQuery = "Con",
        contactsSearchResult = contacts,
        publicSearchResult = public,
        contactsSelectedSearchResult = selectedContacts,
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
        selectedContactResultsExpanded = true,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchAllPeopleScreen_EmptySearchResults() = WireTheme {
    SearchAllPeopleScreen(
        searchQuery = "Con",
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = {},
        selectedContactResultsExpanded = true,
    )
}

private fun previewContact(index: Int, isContact: Boolean) = Contact(
    id = index.toString(),
    domain = "wire.com",
    name = "Contact nr $index",
    handle = "contact_$index",
    connectionState = if (isContact) ConnectionState.ACCEPTED else ConnectionState.NOT_CONNECTED,
    membership = Membership.Standard,
)

private fun previewContactsList(count: Int, startIndex: Int = 0, isContact: Boolean): ImmutableList<Contact> = buildList {
    repeat(count) { index -> add(previewContact(startIndex + index, isContact)) }
}.toPersistentList()
