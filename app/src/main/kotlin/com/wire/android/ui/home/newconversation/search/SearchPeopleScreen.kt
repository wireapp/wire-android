package com.wire.android.ui.home.newconversation.search


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.WireCircularProgressIndicator
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.newconversation.common.GroupButton
import com.wire.android.ui.home.newconversation.contacts.Contact


private const val DEFAULT_SEARCH_RESULT_ITEM_SIZE = 4


data class SearchOpenUserProfile(val contact: Contact, val internal: Boolean)

@Composable
fun SearchPeopleScreen(
    searchQuery: String,
    noneSearchSucceed: Boolean,
    knownContactSearchResult: ContactSearchResult,
    publicContactSearchResult: ContactSearchResult,
    federatedBackendResultContact: ContactSearchResult,
    contactsAddedToGroup: List<Contact>,
    onAddToGroup: (Contact) -> Unit,
    onRemoveFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (SearchOpenUserProfile) -> Unit
) {
    if (searchQuery.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        if (noneSearchSucceed) {
            //TODO : all failed we want to display a general error
        } else {
            Column {
                SearchResult(
                    searchQuery = searchQuery,
                    knownContactSearchResult = knownContactSearchResult,
                    publicContactSearchResult = publicContactSearchResult,
                    federatedBackendResultContact = federatedBackendResultContact,
                    contactsAddedToGroup = contactsAddedToGroup,
                    onAddToGroup = onAddToGroup,
                    onRemoveContactFromGroup = onRemoveFromGroup,
                    onOpenUserProfile = onOpenUserProfile
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResult(
    searchQuery: String,
    knownContactSearchResult: ContactSearchResult,
    publicContactSearchResult: ContactSearchResult,
    federatedBackendResultContact: ContactSearchResult,
    contactsAddedToGroup: List<Contact>,
    onAddToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (SearchOpenUserProfile) -> Unit,
) {
    val searchPeopleScreenState = rememberSearchPeopleScreenState()

    Column {
        LazyColumn(
            state = searchPeopleScreenState.lazyListState,
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
                onOpenUserProfile = { onOpenUserProfile(SearchOpenUserProfile(it, true)) },
            )
            externalSearchResults(
                searchTitle = { stringResource(R.string.label_public_wire) },
                searchQuery = searchQuery,
                contactSearchResult = publicContactSearchResult,
                showAllItems = searchPeopleScreenState.publicResultsCollapsed,
                onShowAllButtonClicked = { searchPeopleScreenState.toggleShowAllPublicResult() },
                onOpenUserProfile = { onOpenUserProfile(SearchOpenUserProfile(it, false)) }
            )
            externalSearchResults(
                searchTitle = { stringResource(R.string.label_federated_backends) },
                searchQuery = searchQuery,
                contactSearchResult = federatedBackendResultContact,
                showAllItems = searchPeopleScreenState.federatedBackendResultsCollapsed,
                onShowAllButtonClicked = { searchPeopleScreenState.toggleShowFederatedBackendResult() },
                onOpenUserProfile = { onOpenUserProfile(SearchOpenUserProfile(it, false)) }
            )
        }
        Divider()
        GroupButton(groupSize = contactsAddedToGroup.size)
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
    onOpenUserProfile: (Contact) -> Unit,
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
                searchTitle = searchTitle,
                failureMessage = searchResult.failureMessage
            )
        }
        // We do not display anything on Initial state
        SearchResultState.Initial -> {
        }
    }
}

private fun LazyListScope.externalSearchResults(
    searchTitle: @Composable () -> String,
    searchQuery: String,
    contactSearchResult: ContactSearchResult,
    showAllItems: Boolean,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
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
                onOpenUserProfile = onOpenUserProfile
            )
        }
        is SearchResultState.Failure -> {
            failureItem(
                searchTitle = searchTitle,
                failureMessage = searchResult.failureMessage
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
                    avatarUrl = avatarUrl,
                    userStatus = userStatus,
                    name = name,
                    label = label,
                    searchQuery = searchQuery,
                    isAddedToGroup = contactsAddedToGroup.contains(contact),
                    addToGroup = { onAddToGroup(contact) },
                    removeFromGroup = { removeFromGroup(contact) },
                    onRowItemClicked = { onOpenUserProfile(contact) },
                    onRowItemLongClicked = { }
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
                        totalSearchResultCount = searchResult.size,
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

private fun LazyListScope.externalSuccessItem(
    searchTitle: @Composable () -> String,
    showAllItems: Boolean,
    searchResult: List<Contact>,
    searchQuery: String,
    onShowAllButtonClicked: () -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
) {
    if (searchResult.isNotEmpty()) {
        item { FolderHeader(searchTitle()) }

        items(if (showAllItems) searchResult else searchResult.take(DEFAULT_SEARCH_RESULT_ITEM_SIZE)) { contact ->
            with(contact) {
                ExternalContactSearchResultItem(
                    avatarUrl = avatarUrl,
                    userStatus = userStatus,
                    name = name,
                    label = label,
                    searchQuery = searchQuery,
                    onRowItemClicked = { onOpenUserProfile(contact) },
                    onRowItemLongClicked = { }
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
                        totalSearchResultCount = searchResult.size,
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

fun LazyListScope.inProgressItem() {
    item {
        Box(
            Modifier
                .fillMaxWidth()
                .height(224.dp)
        ) {
            WireCircularProgressIndicator(
                progressColor = Color.Black, modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }
    }
}

fun LazyListScope.failureItem(searchTitle: @Composable () -> String, failureMessage: String?) {
    item { FolderHeader(searchTitle()) }

    item {
        Box(
            Modifier
                .fillMaxWidth()
                .height(224.dp)
        ) {
            Text(
                failureMessage ?: "We are sorry, something went wrong", modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ShowButton(
    totalSearchResultCount: Int,
    isShownAll: Boolean,
    onShowButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        AnimatedContent(isShownAll) { showAll ->
            WireSecondaryButton(
                text = if (!showAll) "Show All ($totalSearchResultCount)" else "Show Less",
                onClick = onShowButtonClicked,
                minHeight = dimensions().showAllCollapseButtonMinHeight,
                fillMaxWidth = false,
            )
        }
    }
}
