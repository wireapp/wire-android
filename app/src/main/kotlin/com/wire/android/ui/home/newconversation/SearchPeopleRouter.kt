package com.wire.android.ui.home.newconversation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.AppTopBarWithSearchBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.newconversation.common.SearchListScreens
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.home.newconversation.search.SearchOpenUserProfile
import com.wire.android.ui.home.newconversation.search.SearchPeopleScreen
import com.wire.android.ui.home.newconversation.search.SearchPeopleState

@Composable
fun SearchPeopleRouter(
    searchPeopleState: SearchPeopleState,
    openNewGroup: () -> Unit,
    onSearchContact: (String) -> Unit,
    onClose: () -> Unit,
    onAddContactToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (SearchOpenUserProfile) -> Unit,
    onAddContact: (Contact) -> Unit
) {
    val searchNavController = rememberNavController()
    val searchBarState = rememberSearchbarState()


    with(searchPeopleState) {
        AppTopBarWithSearchBar(
            searchBarState = searchBarState,
//            scrollPositionProvider = searchBarState.scrollPositionProvider,
            searchBarHint = stringResource(R.string.label_search_people),
            searchQuery = searchQuery,
            onSearchQueryChanged = { searchTerm ->
                // when the searchTerm changes, we want to propagate it
                // to the ViewModel, only when the searchQuery inside the ViewModel
                // is different than searchTerm coming from the TextInputField
                if (searchTerm != searchQuery) {
                    onSearchContact(searchTerm)
                }
            },
            onSearchClicked = {
                searchNavController.navigate(SearchListScreens.SearchPeopleScreen.route)
            },
            onCloseSearchClicked = {
                searchBarState.closeSearch()
                searchNavController.popBackStack()
            },
            appTopBar = {
                WireCenterAlignedTopAppBar(
                    elevation = 0.dp,
                    title = stringResource(R.string.label_new_conversation),
                    navigationIconType = NavigationIconType.Close,
                    onNavigationPressed = onClose
                )
            },
            content = {
                NavHost(
                    navController = searchNavController,
                    startDestination = SearchListScreens.KnownContactsScreen.route
                ) {
                    composable(
                        route = SearchListScreens.KnownContactsScreen.route,
                        content = {
                            ContactsScreen(
                                scrollPositionProvider = {
                                    searchBarState.scrollPositionProvider = it
                                },
                                allKnownContactResult = allKnownContacts,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onAddToGroup = onAddContactToGroup,
                                onRemoveFromGroup = onRemoveContactFromGroup,
                                onOpenUserProfile = { onOpenUserProfile(SearchOpenUserProfile(it)) },
                                onNewGroupClicked = openNewGroup
                            )
                        }
                    )
                    composable(
                        route = SearchListScreens.SearchPeopleScreen.route,
                        content = {
                            SearchPeopleScreen(
                                scrollPositionProvider = {
                                    searchBarState.scrollPositionProvider = it
                                },
                                searchQuery = searchQuery,
                                noneSearchSucceed = noneSearchSucceed,
                                knownContactSearchResult = localContactSearchResult,
                                publicContactSearchResult = publicContactsSearchResult,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onAddToGroup = onAddContactToGroup,
                                onRemoveFromGroup = onRemoveContactFromGroup,
                                onOpenUserProfile = { searchContact ->
                                    onOpenUserProfile(SearchOpenUserProfile(searchContact.contact))
                                },
                                onNewGroupClicked = openNewGroup,
                                onAddContactClicked = onAddContact
                            )
                        }
                    )
                }
            }
        )
    }

    BackHandler(searchBarState.isSearchActive) {
        searchBarState.closeSearch()
        searchNavController.popBackStack()
    }
}

