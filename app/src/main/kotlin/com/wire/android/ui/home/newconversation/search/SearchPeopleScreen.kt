package com.wire.android.ui.home.newconversation.search


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import com.wire.android.ui.home.newconversation.contacts.Contact

@Composable
fun SearchPeopleScreen(
    searchPeopleState: SearchPeopleState,
) {
    SearchPeopleScreenContent(
        searchQuery = searchPeopleState.searchQuery,
        contactContactSearchResult = searchPeopleState.localContactSearchResult,
        publicContactSearchResult = searchPeopleState.publicContactsSearchResult,
        federatedBackendResultContact = searchPeopleState.federatedContactSearchResult
    )
}

@Composable
private fun SearchPeopleScreenContent(
    searchQuery: String,
    contactContactSearchResult: ContactSearchResult,
    publicContactSearchResult: ContactSearchResult,
    federatedBackendResultContact: ContactSearchResult,
) {
    if (searchQuery.isEmpty()) {
        EmptySearchQueryScreen()
    } else {
        SearchResult(
            searchQuery = searchQuery,
            contactContactSearchResult = contactContactSearchResult,
            publicContactSearchResult = publicContactSearchResult,
            federatedBackendResultContact = federatedBackendResultContact
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResult(
    searchQuery: String,
    contactContactSearchResult: ContactSearchResult,
    publicContactSearchResult: ContactSearchResult,
    federatedBackendResultContact: ContactSearchResult,
) {
    val searchPeopleScreenState = rememberSearchPeopleScreenState()

    LazyColumn(
        state = searchPeopleScreenState.lazyListState,
        modifier = Modifier
            .fillMaxSize()
    ) {
        searchResults(
            searchTitle = { stringResource(R.string.label_contacts) },
            searchQuery = searchQuery,
            contactSearchResult = contactContactSearchResult,
            showAllItems = searchPeopleScreenState.contactsAllResultsCollapsed,
            onShowAllButtonClicked = { searchPeopleScreenState.toggleShowAllContactsResult() }
        )
        searchResults(
            searchTitle = { stringResource(R.string.label_public_wire) },
            searchQuery = searchQuery,
            contactSearchResult = publicContactSearchResult,
            showAllItems = searchPeopleScreenState.publicResultsCollapsed,
            onShowAllButtonClicked = { searchPeopleScreenState.toggleShowAllPublicResult() }
        )
        searchResults(
            searchTitle = { stringResource(R.string.label_federated_backends) },
            searchQuery = searchQuery,
            contactSearchResult = federatedBackendResultContact,
            showAllItems = searchPeopleScreenState.federatedBackendResultsCollapsed,
            onShowAllButtonClicked = { searchPeopleScreenState.toggleShowFederatedBackendResult() }
        )
    }
}

private fun LazyListScope.searchResults(
    searchTitle: @Composable () -> String,
    searchQuery: String,
    contactSearchResult: ContactSearchResult,
    showAllItems: Boolean,
    onShowAllButtonClicked: () -> Unit,
) {
    item { FolderHeader(searchTitle()) }

    when (val searchResult = contactSearchResult.searchResultState) {
        SearchResultState.InProgress -> {
            inProgressItem()
        }
        is SearchResultState.Success -> {
            successItem(
                showAllItems = showAllItems,
                searchResult = searchResult.result,
                searchQuery = searchQuery,
                searchSource = contactSearchResult.searchSource,
                onShowAllButtonClicked = onShowAllButtonClicked
            )
        }
        is SearchResultState.Failure -> {
            failureItem(searchResult.failureMessage)
        }
    }
}

private fun LazyListScope.successItem(
    showAllItems: Boolean,
    searchResult: List<Contact>,
    searchQuery: String,
    searchSource: SearchSource,
    onShowAllButtonClicked: () -> Unit
) {
    items(if (showAllItems) searchResult else searchResult.take(4)) { contact ->
        with(contact) {
            ContactSearchResultItem(
                avatarUrl = avatarUrl,
                userStatus = userStatus,
                name = name,
                label = label,
                searchQuery = searchQuery,
                searchSource = searchSource,
                onRowItemClicked = { },
                onRowItemLongClicked = { }
            )
        }
    }
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

fun LazyListScope.failureItem(failureMessage: String?) {
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
