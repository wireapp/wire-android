package com.wire.android.ui.home.newconversation.search

import com.wire.android.ui.home.newconversation.contacts.Contact

data class SearchPeopleState(
    val searchQuery: String = "",
    val contactSearchResult: List<Contact> = emptyList()
)

