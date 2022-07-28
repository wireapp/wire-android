package com.wire.android.ui.home.conversations.search

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.wire.android.R
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.AppTopBarWithSearchBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.newconversation.common.SearchListScreen
import com.wire.android.ui.home.newconversation.model.Contact

@Composable
fun SearchPeopleRouter(
    searchBarTitle: String,
    onPeoplePicked: () -> Unit,
    searchPeopleViewModel: SearchPeopleViewModel,
) {
    val searchBarState = rememberSearchbarState()
    Navigator(
        screen = SearchListScreen.KnownContacts(
            scrollPositionProvider = { searchBarState.scrollPositionProvider = it },
            searchPeopleViewModel = searchPeopleViewModel,
            onNewGroupClicked = onPeoplePicked
        )
    ) { navigator ->
        val searchQuery = searchPeopleViewModel.state.searchQuery
        AppTopBarWithSearchBar(
            searchBarState = searchBarState,
            searchBarHint = stringResource(R.string.label_search_people),
            searchQuery = searchQuery,
            onSearchQueryChanged = { searchTerm ->
                // when the searchTerm changes, we want to propagate it
                // to the ViewModel, only when the searchQuery inside the ViewModel
                // is different than searchTerm coming from the TextInputField
                if (searchTerm != searchQuery) {
                    searchPeopleViewModel.search(searchTerm)
                }
            },
            onSearchClicked = {
                navigator.push(
                    SearchListScreen.SearchPeople(
                        scrollPositionProvider = { searchBarState.scrollPositionProvider = it },
                        searchPeopleViewModel = searchPeopleViewModel,
                        onNewGroupClicked = onPeoplePicked
                    )
                )
            },
            onCloseSearchClicked = {
                searchBarState.closeSearch()
                navigator.pop()
            },
            appTopBar = {
                WireCenterAlignedTopAppBar(
                    elevation = 0.dp,
                    title = searchBarTitle,
                    navigationIconType = NavigationIconType.Close,
                    onNavigationPressed = searchPeopleViewModel::close
                )
            },
            content = { CurrentScreen() }
        )
        BackHandler(searchBarState.isSearchActive) {
            searchBarState.closeSearch()
            navigator.pop()
        }
    }
}
