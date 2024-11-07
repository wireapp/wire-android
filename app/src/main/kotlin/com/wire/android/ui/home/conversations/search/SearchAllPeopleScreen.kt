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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
<<<<<<< HEAD
import androidx.compose.ui.unit.dp
=======
import androidx.hilt.navigation.compose.hiltViewModel
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.model.ItemActionType
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
<<<<<<< HEAD
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.home.conversationslist.model.Membership
=======
import com.wire.android.ui.common.progress.CenteredCircularProgressBarIndicator
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.SendConnectionRequestViewModel
import com.wire.android.ui.home.newconversation.SendConnectionRequestViewModelImpl
import com.wire.android.ui.home.newconversation.SendConnectionRequestViewModelPreview
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.extension.FolderType
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.PreviewMultipleThemes
<<<<<<< HEAD
import com.wire.android.util.ui.keepOnTopWhenNotScrolled
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
=======
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))

private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4

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
<<<<<<< HEAD
    if (contactsSearchResult.isEmpty() && publicSearchResult.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        SearchResult(
            searchQuery = searchQuery,
            publicSearchResult = publicSearchResult,
            contactsSearchResult = contactsSearchResult,
            contactsSelectedSearchResult = contactsSelectedSearchResult,
=======
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
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
            onChecked = onChecked,
            onOpenUserProfile = onOpenUserProfile,
            lazyListState = lazyListState,
            isSearchActive = isSearchActive,
<<<<<<< HEAD
            isLoading = isLoading,
            actionType = actionType,
            selectedContactResultsExpanded = selectedContactResultsExpanded,
            onSelectedContactResultsExpansionChanged = onSelectedContactResultsExpansionChanged,
            contactResultsExpanded = contactResultsExpanded,
            onContactResultsExpansionChanged = onContactResultsExpansionChanged,
            publicResultsExpanded = publicResultsExpanded,
            onPublicResultsExpansionChanged = onPublicResultsExpansionChanged,
=======
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
        )
    }
}

@Composable
private fun SearchResult(
    searchQuery: String,
    contactsSearchResult: ImmutableList<Contact>,
    publicSearchResult: ImmutableList<Contact>,
<<<<<<< HEAD
    contactsSelectedSearchResult: ImmutableList<Contact>,
    isLoading: Boolean,
=======
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
    isSearchActive: Boolean,
    actionType: ItemActionType,
    onChecked: (Boolean, Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
<<<<<<< HEAD
    selectedContactResultsExpanded: Boolean,
    onSelectedContactResultsExpansionChanged: (Boolean) -> Unit,
    contactResultsExpanded: Boolean,
    onContactResultsExpansionChanged: (Boolean) -> Unit,
    publicResultsExpanded: Boolean,
    onPublicResultsExpansionChanged: (Boolean) -> Unit,
=======
    sendConnectionRequestViewModel: SendConnectionRequestViewModel =
        if (LocalInspectionMode.current) SendConnectionRequestViewModelPreview
        else hiltViewModel<SendConnectionRequestViewModelImpl>(),
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
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
<<<<<<< HEAD
                    isLoading = isLoading,
                    contactSearchResult = contactsSelectedSearchResult.map { it to true }.toImmutableList(),
                    allItemsVisible = true, // for selected contacts we always show all items
                    showMoreOrLessButtonVisible = false,
=======
                    searchResult = contactsSearchResult,
                    contactsAddedToGroup = contactsAddedToGroup,
                    showAllItems = !isSearchActive || searchPeopleScreenState.contactsAllResultsCollapsed,
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
                    onShowAllButtonClicked = searchPeopleScreenState::toggleShowAllContactsResult,
                    onOpenUserProfile = onOpenUserProfile,
                    actionType = actionType,
                    expanded = selectedContactResultsExpanded,
                    onExpansionChanged = onSelectedContactResultsExpansionChanged,
                )
            }

            if (contactsSearchResult.isNotEmpty()) { // non-selected contacts section filtered by search query
                internalSearchResults(
                    searchTitle = context.getString(R.string.label_contacts),
                    searchQuery = searchQuery,
                    onChecked = onChecked,
                    isLoading = isLoading,
                    contactSearchResult = contactsSearchResult.map { it to false }.toImmutableList(),
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
                    searchTitle = context.getString(R.string.label_public_wire),
                    searchQuery = searchQuery,
<<<<<<< HEAD
                    contactSearchResult = publicSearchResult,
                    isLoading = isLoading,
                    allItemsVisible = searchPeopleScreenState.publicResultsCollapsed,
                    showMoreOrLessButtonVisible = isSearchActive,
=======
                    searchResult = publicSearchResult,
                    showAllItems = searchPeopleScreenState.publicResultsCollapsed,
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
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
    searchTitle: String,
<<<<<<< HEAD
    searchQuery: String,
    onChecked: (Boolean, Contact) -> Unit,
    actionType: ItemActionType,
    isLoading: Boolean,
    contactSearchResult: ImmutableList<Pair<Contact, Boolean>>,
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    expanded: Boolean,
    onExpansionChanged: (Boolean) -> Unit,
) {
    when {
        isLoading -> {
            inProgressItem()
        }

        else -> {
            internalSuccessItem(
                searchTitle = searchTitle,
                allItemsVisible = allItemsVisible,
                showMoreOrLessButtonVisible = showMoreOrLessButtonVisible,
                onChecked = onChecked,
                searchResult = contactSearchResult,
                searchQuery = searchQuery,
                onShowAllButtonClicked = onShowAllButtonClicked,
                onOpenUserProfile = onOpenUserProfile,
                actionType = actionType,
                expanded = expanded,
                onExpansionChanged = onExpansionChanged,
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
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    expanded: Boolean,
    onExpansionChanged: (Boolean) -> Unit,
) {
    when {
        isLoading -> {
            inProgressItem()
        }

        else -> {
            externalSuccessItem(
                searchTitle = searchTitle,
                allItemsVisible = allItemsVisible,
                showMoreOrLessButtonVisible = showMoreOrLessButtonVisible,
                searchResult = contactSearchResult,
                searchQuery = searchQuery,
                onShowAllButtonClicked = onShowAllButtonClicked,
                onOpenUserProfile = onOpenUserProfile,
                expanded = expanded,
                onExpansionChanged = onExpansionChanged,
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSuccessItem(
    searchTitle: String,
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    actionType: ItemActionType,
=======
    showAllItems: Boolean,
    contactsAddedToGroup: ImmutableSet<Contact>,
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
    onChecked: (Boolean, Contact) -> Unit,
    searchResult: ImmutableList<Pair<Contact, Boolean>>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    expanded: Boolean,
    onExpansionChanged: (Boolean) -> Unit,
) {
    if (searchResult.isNotEmpty()) {
        folderWithElements(
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
    searchTitle: String,
    allItemsVisible: Boolean,
    showMoreOrLessButtonVisible: Boolean,
    searchResult: List<Contact>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onExpansionChanged: (Boolean) -> Unit,
    expanded: Boolean,
) {
    val itemsList =
        if (allItemsVisible) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)

    folderWithElements(
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

<<<<<<< HEAD
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

=======
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
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
<<<<<<< HEAD
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
=======
    SearchAllPeopleScreen("Search query", persistentListOf(), persistentListOf(), persistentSetOf(), true, false, { _, _ -> }, {})
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
}

@PreviewMultipleThemes
@Composable
<<<<<<< HEAD
fun PreviewSearchAllPeopleScreen_EmptyList() = WireTheme {
    SearchAllPeopleScreen(
        searchQuery = "Search query",
        contactsSearchResult = persistentListOf(),
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = true,
        actionType = ItemActionType.CHECK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
=======
fun PreviewSearchAllPeopleScreen_InitialResults() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true).toPersistentList()
    SearchAllPeopleScreen("", contacts, persistentListOf(), persistentSetOf(), false, false, { _, _ -> }, {})
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
}

@PreviewMultipleThemes
@Composable
<<<<<<< HEAD
fun PreviewSearchAllPeopleScreen_NoSearch() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true)
    SearchAllPeopleScreen(
        searchQuery = "",
        contactsSearchResult = contacts,
        publicSearchResult = persistentListOf(),
        contactsSelectedSearchResult = persistentListOf(),
        isLoading = false,
        isSearchActive = false,
        actionType = ItemActionType.CLICK,
        onChecked = { _, _ -> },
        onOpenUserProfile = { },
    )
=======
fun PreviewSearchAllPeopleScreen_EmptyInitialResults() = WireTheme {
    SearchAllPeopleScreen("", persistentListOf(), persistentListOf(), persistentSetOf(), false, false, { _, _ -> }, {})
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
}

@PreviewMultipleThemes
@Composable
<<<<<<< HEAD
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
=======
fun PreviewSearchAllPeopleScreen_SearchResults() = WireTheme {
    val contacts = previewContactsList(count = 10, startIndex = 0, isContact = true).toPersistentList()
    val public = previewContactsList(count = 10, startIndex = 10, isContact = false).toPersistentList()
    SearchAllPeopleScreen("Con", contacts, public, persistentSetOf(), false, true, { _, _ -> }, {})
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
}

@PreviewMultipleThemes
@Composable
<<<<<<< HEAD
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
=======
fun PreviewSearchAllPeopleScreen_EmptySearchResults() = WireTheme {
    SearchAllPeopleScreen("Con", persistentListOf(), persistentListOf(), persistentSetOf(), false, true, { _, _ -> }, {})
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
}

private fun previewContact(index: Int, isContact: Boolean) = Contact(
    id = index.toString(),
    domain = "wire.com",
    name = "Contact nr $index",
<<<<<<< HEAD
    handle = "contact_$index",
=======
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
    connectionState = if (isContact) ConnectionState.ACCEPTED else ConnectionState.NOT_CONNECTED,
    membership = Membership.Standard,
)

<<<<<<< HEAD
private fun previewContactsList(count: Int, startIndex: Int = 0, isContact: Boolean): ImmutableList<Contact> = buildList {
    repeat(count) { index -> add(previewContact(startIndex + index, isContact)) }
}.toPersistentList()
=======
private fun previewContactsList(count: Int, startIndex: Int = 0, isContact: Boolean): List<Contact> =
    buildList { repeat(count) { index -> add(previewContact(startIndex + index, isContact)) } }
>>>>>>> 0b3dc07e6 (fix: show proper empty user search screens [WPB-6257] (#3589))
