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
import androidx.compose.foundation.lazy.items
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
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.search.widget.SearchFailureBox
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsRow
import com.wire.android.ui.home.newconversation.model.Contact

private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4

data class SearchOpenUserProfile(val contact: Contact)

@Composable
fun SearchPeopleScreen(
    searchQuery: String,
    noneSearchSucceed: Boolean,
    knownContactSearchResult: ContactSearchResult,
    publicContactSearchResult: ContactSearchResult,
    contactsAddedToGroup: List<Contact>,
    onAddToGroup: (Contact) -> Unit,
    onRemoveFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (SearchOpenUserProfile) -> Unit,
    onAddContactClicked: (Contact) -> Unit,
) {
    if (searchQuery.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        if (noneSearchSucceed) {
            // TODO : all failed we want to display a general error
        } else {
            Column {
                SearchResult(
                    searchQuery = searchQuery,
                    knownContactSearchResult = knownContactSearchResult,
                    publicContactSearchResult = publicContactSearchResult,
                    contactsAddedToGroup = contactsAddedToGroup,
                    onAddToGroup = onAddToGroup,
                    onRemoveContactFromGroup = onRemoveFromGroup,
                    onOpenUserProfile = onOpenUserProfile,
                    onAddContactClicked = onAddContactClicked
                )
            }
        }
    }
}

@Composable
private fun SearchResult(
    searchQuery: String,
    knownContactSearchResult: ContactSearchResult,
    publicContactSearchResult: ContactSearchResult,
    contactsAddedToGroup: List<Contact>,
    onAddToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (SearchOpenUserProfile) -> Unit,
    onAddContactClicked: (Contact) -> Unit,
) {
    val searchPeopleScreenState = rememberSearchPeopleScreenState()
    val lazyListState = rememberLazyListState()

    val context = LocalContext.current

    Column {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f),
        ) {
            internalSearchResults(
                searchTitle = { stringResource(R.string.label_contacts) },
                searchQuery = searchQuery,
                contactsAddedToGroup = contactsAddedToGroup,
                onAddToGroup = onAddToGroup,
                removeFromGroup = onRemoveContactFromGroup,
                contactSearchResult = knownContactSearchResult,
                showAllItems = searchPeopleScreenState.contactsAllResultsCollapsed,
                onShowAllButtonClicked = { searchPeopleScreenState.toggleShowAllContactsResult() },
                onOpenUserProfile = { contact -> onOpenUserProfile(SearchOpenUserProfile(contact)) },
            )
            externalSearchResults(
                searchTitle = context.getString(R.string.label_public_wire),
                searchQuery = searchQuery,
                contactSearchResult = publicContactSearchResult,
                showAllItems = searchPeopleScreenState.publicResultsCollapsed,
                onShowAllButtonClicked = { searchPeopleScreenState.toggleShowAllPublicResult() },
                onOpenUserProfile = { externalUser -> onOpenUserProfile(SearchOpenUserProfile(externalUser)) },
                onAddContactClicked = onAddContactClicked
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSearchResults(
    searchTitle: @Composable () -> String,
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
        // We do not display anything on Initial state
        SearchResultState.Initial -> {
        }
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
        // We do not display anything on Initial state
        SearchResultState.Initial -> {
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.internalSuccessItem(
    searchTitle: @Composable () -> String,
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
        item { FolderHeader(searchTitle()) }

        items(if (showAllItems) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)) { contact ->
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
    folderWithElements(header = searchTitle,
        items = (if (showAllItems) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)).associateBy { it.id }) { contact ->
        with(contact) {
            ExternalContactSearchResultItem(
                avatarData = avatarData,
                name = name,
                label = label,
                membership = membership,
                connectionState = contact.connectionState,
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
                minHeight = dimensions().showAllCollapseButtonMinHeight,
                fillMaxWidth = false,
            )
        }
    }
}
