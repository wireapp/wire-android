package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.android.util.flow.SearchQueryStateFlow
import com.wire.kalium.logic.feature.user.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.user.SearchPublicUserUseCase
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchPeopleViewModel @Inject constructor(
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchPublicUserUseCase
) : ViewModel() {
    private companion object {
        const val HARDCODED_TEST_DOMAIN = "staging.zinfra.io"
    }

    var state: SearchPeopleState by mutableStateOf(
        SearchPeopleState()
    )

    private val searchQueryStateFlow = SearchQueryStateFlow()

    init {
        viewModelScope.launch {
            searchQueryStateFlow.onSearchAction { searchTerm ->
                state = state.copy(searchQuery = searchTerm)

                launch {
                    state = state.copy(
                        publicContactsSearchResult = ContactSearchResult.PublicContact(
                            searchResultState = SearchResultState.InProgress
                        )
                    )

                    when (val result = searchPublicUsers(
                        searchQuery = searchTerm,
                        domain = HARDCODED_TEST_DOMAIN
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
            }
        }
    }

    fun search(searchTerm: String) {
        searchQueryStateFlow.search(searchTerm)
    }

}
