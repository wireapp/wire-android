package com.wire.android.ui.home.newconversation.search

import com.wire.android.ui.home.newconversation.contacts.Contact

data class SearchPeopleState(
    val searchQuery: String = "",
    val noneSearchSucceed: Boolean = false,
    val localContactSearchResult: ContactSearchResult =
        ContactSearchResult.InternalContact(
            searchResultState = SearchResultState.Initial
        ),
    val publicContactsSearchResult: ContactSearchResult =
        ContactSearchResult.ExternalContact(
            searchResultState = SearchResultState.Initial
        ),
    val federatedContactSearchResult: ContactSearchResult =
        ContactSearchResult.ExternalContact(
            searchResultState = SearchResultState.Initial
        ),
    val contactsAddedToGroup: List<Contact> = emptyList(),
    val allKnownContacts: List<Contact> = emptyList(),
    val scrollPosition : Int,
)

sealed class ContactSearchResult(val searchResultState: SearchResultState) {
    class InternalContact(searchResultState: SearchResultState) :
        ContactSearchResult(
            searchResultState = searchResultState,
        )

    class ExternalContact(searchResultState: SearchResultState) :
        ContactSearchResult(
            searchResultState = searchResultState
        )
}

sealed class SearchResultState {
    object Initial : SearchResultState()
    object InProgress : SearchResultState()
    data class Failure(val failureMessage: String? = null) : SearchResultState()
    data class Success(val result: List<Contact>) : SearchResultState()
}
