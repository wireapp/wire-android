package com.wire.android.ui.home.newconversation.search

import com.wire.android.ui.home.newconversation.contacts.Contact

data class SearchPeopleState(
    val searchQuery: String = "",
    val contactContactSearchResult: ContactSearchResult = ContactSearchResult(searchSource = SearchSource.Internal),
    val publicContactContactSearchResult: ContactSearchResult = ContactSearchResult(searchSource = SearchSource.External),
    val federatedContactContactSearchResult: ContactSearchResult = ContactSearchResult(searchSource = SearchSource.External)
)

data class ContactSearchResult(val searchSource: SearchSource, val result: List<Contact> = emptyList())

sealed class SearchSource {
    object External : SearchSource()
    object Internal : SearchSource()
}


