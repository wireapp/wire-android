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
                    "This is first contact"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                ),
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                ),
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                ),
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "test3"
                ),
                Contact(
                    "test1"
                ), Contact(
                    "test2"
                ), Contact(
                    "This is last contact"
                )
            )
        )
    }

}



