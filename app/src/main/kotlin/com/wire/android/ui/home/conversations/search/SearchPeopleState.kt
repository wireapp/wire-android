package com.wire.android.ui.home.conversations.search

import androidx.annotation.StringRes
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.home.newconversation.model.Contact
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class SearchPeopleState(
    val initialContacts: SearchResultState = SearchResultState.Initial,
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val searchResult: ImmutableMap<SearchResultTitle, ContactSearchResult> = persistentMapOf(),
    val noneSearchSucceed: Boolean = false,
    val contactsAddedToGroup: ImmutableList<Contact> = persistentListOf(),
    val isGroupCreationContext: Boolean = true
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

    data class Success(val result: ImmutableList<Contact>) : SearchResultState()
}

data class SearchResultTitle(@StringRes val stringRes: Int)
