package com.wire.android.ui.home.conversations.search

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.user.SelfUser

data class SearchPeopleState(
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val noneSearchSucceed: Boolean = false,
    val localContactSearchResult: ContactSearchResult =
        ContactSearchResult.InternalContact(
            searchResultState = SearchResultState.Initial
        ),
    val publicContactsSearchResult: ContactSearchResult =
        ContactSearchResult.ExternalContact(
            searchResultState = SearchResultState.Initial
        ),
    val contactsAddedToGroup: List<Contact> = emptyList(),
    val allKnownContacts: SearchResultState = SearchResultState.Initial,
    val self: SelfUser? = null
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
