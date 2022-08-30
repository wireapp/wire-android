package com.wire.android.ui.home.conversations.search

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.newconversation.model.Contact

data class SearchPeopleState(
    val initialContacts: SearchResultState = SearchResultState.Initial,
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val searchResult: Map<String, ContactSearchResult> = emptyMap(),
    val noneSearchSucceed: Boolean = false,
    val contactsAddedToGroup: List<Contact> = emptyList()
)

sealed class ContactSearchResult(val searchResultState: SearchResultState) {
    class InternalContact(searchResultState: SearchResultState) :
        ContactSearchResult(
            searchResultState = searchResultState
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
