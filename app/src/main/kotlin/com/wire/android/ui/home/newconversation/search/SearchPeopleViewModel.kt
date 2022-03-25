package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.newconversation.contacts.toContact
import com.wire.android.ui.home.newconversation.search.ContactSearchResult.PublicContact
import com.wire.android.util.flow.SearchQueryStateFlow
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUserDirectoryUseCase
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SearchPeopleViewModel @Inject constructor(
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchUserDirectoryUseCase
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

                launch { searchPublic(searchTerm) }
                launch { searchKnown(searchTerm) }
            }
        }
    }

    private suspend fun searchKnown(searchTerm: String) {
        //TODO: this is going to be refactored on the Kalium side so that we do not use Flow
        searchKnownUsers(searchTerm).onStart {
            state = state.copy(
                localContactSearchResult = ContactSearchResult.LocalContact(SearchResultState.InProgress)
            )
        }.catch {
            state = state.copy(
                localContactSearchResult = ContactSearchResult.LocalContact(SearchResultState.Failure())
            )
        }.flowOn(Dispatchers.IO).collect {
            state = state.copy(
                localContactSearchResult = ContactSearchResult.LocalContact(
                    SearchResultState.Success(it.result.map { publicUser -> publicUser.toContact() })
                )
            )
        }
    }

    private suspend fun searchPublic(searchTerm: String) {
        state = state.copy(
            publicContactsSearchResult = PublicContact(SearchResultState.InProgress)
        )

        val result = withContext(Dispatchers.IO) {
            searchPublicUsers(
                searchQuery = searchTerm,
                domain = HARDCODED_TEST_DOMAIN
            )
        }

        state = when (result) {
            is Either.Left -> {
                state.copy(
                    publicContactsSearchResult = PublicContact(SearchResultState.Failure())
                )
            }
            is Either.Right -> {
                state.copy(
                    publicContactsSearchResult = PublicContact(
                        SearchResultState.Success(result.value.result.map { it.toContact() })
                    )
                )
            }
        }
    }

    fun search(searchTerm: String) {
        searchQueryStateFlow.search(searchTerm)
    }

}
