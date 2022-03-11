package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.android.ui.home.newconversation.contacts.ExternalContact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchPeopleViewModel @Inject constructor() : ViewModel() {

    var state: SearchPeopleState by mutableStateOf(
        SearchPeopleState()
    )

    fun search(searchQuery: String) {
        state = state.copy(
            searchQuery = searchQuery,
            contactSearchResult = buildList {
                for (i in 1..5) {
                    add(
                        Contact(
                            id = i,
                            "test$i",
                            label = "test$i",
                            eventType = EventType.UnreadMention
                        )
                    )
                }
                for (i in 1..5) {
                    add(
                        Contact(
                            id = i,
                            "test$i",
                            label = "test$i",
                            eventType = EventType.MissedCall
                        )
                    )
                }
                for (i in 1..5) {
                    add(
                        Contact(
                            id = i,
                            "test$i",
                            label = "test$i",
                            eventType = EventType.UnreadMessage(i)
                        )
                    )
                }
            },
            publicContactSearchResult = buildList {
                for (i in 1..15) {
                    add(
                        ExternalContact(
                            id = i,
                            "test$i",
                            label = "test$i",
                        )
                    )
                }
            },
            federatedContactSearchResult = buildList {
                for (i in 1..15) {
                    add(
                        ExternalContact(
                            id = i,
                            "test$i",
                            label = "test$i",
                        )
                    )
                }
            }
        )
    }

}

