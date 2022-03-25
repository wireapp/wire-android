package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.kalium.logic.feature.user.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.user.SearchPublicUserUseCase
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchPeopleViewModel @Inject constructor(
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchPublicUserUseCase
) : ViewModel() {

    var state: SearchPeopleState by mutableStateOf(
        SearchPeopleState()
    )

    private val searchQuery = MutableStateFlow("")

    private var outGoingSearchJob: Job? = null

    init {
        viewModelScope.launch {
            searchQuery.debounce(500).collect { searchQuery ->
                state = state.copy(searchQuery = searchQuery)

                if (outGoingSearchJob != null) {
                    outGoingSearchJob?.let { job ->
                        if (job.isActive) {
                            job.cancel()
                        }
                    }
                }

                outGoingSearchJob = launch {
                    state = state.copy(
                        publicContactsSearchResult = ContactSearchResult.PublicContact(
                            searchResultState = SearchResultState.InProgress
                        )
                    )
                    launch {
                        when (val result = searchPublicUsers(
                            searchQuery = searchQuery,
                            domain = "staging.zinfra.io"
                        )) {
                            is Either.Left -> {
                                state = state.copy(
                                    publicContactsSearchResult = ContactSearchResult.PublicContact(
                                        searchResultState = SearchResultState.Failure()
                                    )
                                )
                            }
                            is Either.Right -> {
                                state = state.copy(
                                    publicContactsSearchResult = ContactSearchResult.PublicContact(
                                        searchResultState = SearchResultState.Success(result.value.publicUsers.map { publicUser ->
                                            Contact(
                                                id = 1,
                                                name = publicUser.name,
                                                label = publicUser.handle ?: "",
                                            )
                                        }
                                        )
                                    )
                                )
                            }
                        }
                    }

                    launch {
//                        try {
//                            searchKnownUsers(searchTerm).map {
//                                it.map {
//                                        Contact(
//                                            id = 1,
//                                            name = name ?: "",
//                                        )
//                                }
//                            }.collect {
//                                state =
//                                    state.copy(
//                                        localContactSearchResult = ContactSearchResult(
//                                            result = it,
//                                            searchSource = SearchSource.Local
//                                        )
//                                    )
//                            }
//                        } catch (exception: Exception) {
//                            Log.d("TEST", "error ${exception.stackTrace}")
//                        }
//                    }
                    }
                }
            }
        }
    }

    fun search(searchTerm: String) {
        searchQuery.value = searchTerm
    }

}


class SearchQueryStateFlow() {
    private val searchQuery = MutableStateFlow("")

    init {

    }

    private var outGoingSearchJob: Job? = null

    fun search(searchTerm: String) {
        searchQuery.value = searchTerm
    }
}
