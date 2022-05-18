package com.wire.android.ui.home.newconversation.search

import androidx.annotation.StringRes
import com.wire.android.ui.home.newconversation.model.Contact

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
    val scrollPosition: Int = 0,
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
    data class Failure(@StringRes val failureString: Int) : SearchResultState()

    data class Success(val result: List<Contact>) : SearchResultState()
}
