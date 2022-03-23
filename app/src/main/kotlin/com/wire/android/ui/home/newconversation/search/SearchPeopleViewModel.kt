package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wire.android.ui.home.newconversation.contacts.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchPeopleViewModel @Inject constructor() : ViewModel() {

    var state: SearchPeopleState by mutableStateOf(
        SearchPeopleState()
    )

    @Suppress("MagicNumber")
    fun search(searchQuery: String) {
        state = state.copy(
            searchQuery = searchQuery,
            contactContactSearchResult = ContactSearchResult(searchSource = SearchSource.Internal, result = buildList {
                for (i in 1..5) {
                    add(
                        Contact(
                            id = i,
                            "test$i",
                            label = "test$i",
                        )
                    )
                }
                for (i in 1..5) {
                    add(
                        Contact(
                            id = i,
                            "test$i",
                            label = "test$i",
                        )
                    )
                }
                for (i in 1..5) {
                    add(
                        Contact(
                            id = i,
                            "test$i",
                            label = "test$i",
                        )
                    )
                }
            }),
            publicContactContactSearchResult = ContactSearchResult(searchSource = SearchSource.External, result = buildList {
                for (i in 1..15) {
                    add(
                        Contact(
                            id = i,
                            "test$i",
                            label = "test$i",
                        )
                    )
                }
            }),
            federatedContactContactSearchResult = ContactSearchResult(searchSource = SearchSource.External, result = buildList {
                for (i in 1..15) {
                    add(
                        Contact(
                            id = i,
                            "test$i",
                            label = "test$i",
                        )
                    )
                }
            }
            )
        )
    }
}
