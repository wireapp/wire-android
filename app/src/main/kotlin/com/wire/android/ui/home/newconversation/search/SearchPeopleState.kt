package com.wire.android.ui.home.newconversation.search

import com.wire.android.ui.home.newconversation.contacts.Contact

data class SearchPeopleState(
    val searchQuery: String = "",
    val localContactSearchResult: ContactSearchResult = ContactSearchResult.LocalContact(searchResultState = SearchResultState.Initial),
    val publicContactsSearchResult: ContactSearchResult = ContactSearchResult.PublicContact(searchResultState = SearchResultState.Initial),
    val federatedContactSearchResult: ContactSearchResult = ContactSearchResult.FederatedContact(searchResultState = SearchResultState.Initial)
)

enum class SearchSource {
    Internal, External
}

sealed class ContactSearchResult(val searchResultState: SearchResultState, val searchSource: SearchSource) {
    class LocalContact(searchResultState: SearchResultState) :
        ContactSearchResult(
            searchResultState = searchResultState,
            searchSource = SearchSource.Internal
        )

    class PublicContact(searchResultState: SearchResultState) :
        ContactSearchResult(
            searchResultState = searchResultState,
            searchSource = SearchSource.External
        )

    class FederatedContact(searchResultState: SearchResultState) :
        ContactSearchResult(
            searchResultState = searchResultState,
            searchSource = SearchSource.External
        )
}

sealed class SearchResultState {
    object Initial : SearchResultState()
    object InProgress : SearchResultState()
    data class Failure(val failureMessage: String? = null) : SearchResultState()
    data class Success(val result: List<Contact>) : SearchResultState()
}
