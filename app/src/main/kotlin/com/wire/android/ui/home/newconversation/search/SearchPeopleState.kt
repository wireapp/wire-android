package com.wire.android.ui.home.newconversation.search

import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.android.ui.home.newconversation.contacts.ExternalContact

data class SearchPeopleState(
    val searchQuery: String = "",
    val contactSearchResult: List<Contact> = emptyList(),
    val publicContactSearchResult: List<ExternalContact> = emptyList(),
    val federatedContactSearchResult: List<ExternalContact> = emptyList()
)

