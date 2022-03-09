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

    fun search(searchQuery: String) {
        state = state.copy(
            searchQuery = searchQuery,
            contactSearchResult = listOf(
                Contact(
                    id = 1,
                    "This is first contact",
                    label = "test23"
                ), Contact(
                    id =2,
                    "test2",
                    label = "test23"
                ), Contact(
                    id = 3,
                    "test3",
                    label = "test23"
                ),
                Contact(
                    id = 4,
                    "test1",
                    label = "test23"
                ), Contact(
                    id = 5,
                    "test2",
                    label = "test23"
                ), Contact(
                    id = 6,
                    "test3",
                    label = "test23"
                ),
                Contact(
                    id = 7,
                    "test1",
                    label = "test23"
                ), Contact(
                    id = 8,
                    "test2",
                    label = "test23"
                ), Contact(
                    id = 9,
                    "test3",
                    label = "test23"
                ),
                Contact(
                    id = 10,
                    "test1"
                ), Contact(
                    id = 11,
                    "test2",
                    label = "test235"
                ), Contact(
                    id = 12,
                    "test3",
                    label = "test234"
                ),
                Contact(
                    id = 13,
                    "test1"
                ), Contact(
                    id = 14,
                    "test2",
                    label = "test23"
                ), Contact(
                    id = 15,
                    "This is last contact",
                    label = "test2"
                )
            )
        )
    }

}

