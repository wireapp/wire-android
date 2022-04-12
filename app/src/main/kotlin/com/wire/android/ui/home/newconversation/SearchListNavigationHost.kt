package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.R
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.AppTopBarWithSearchBar
import com.wire.android.ui.home.newconversation.common.Screen
import com.wire.android.ui.home.newconversation.common.SearchListScreens
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.search.SearchPeopleScreen

@Composable
fun SearchListNavigationHost(
    newConversationNavController: NavController,
    searchNavController: NavController,
    newConversationViewModel: NewConversationViewModel
) {
    with(newConversationViewModel.state) {
        AppTopBarWithSearchBar(
            scrollPosition = newConversationViewModel.scrollPosition,
            searchBarHint = stringResource(R.string.label_search_people),
            searchQuery = searchQuery,
            onSearchQueryChanged = { searchTerm ->
                // when the searchTerm changes, we want to propagate it
                // to the ViewModel, only when the searchQuery inside the ViewModel
                // is different than searchTerm coming from the TextInputField
                if (searchTerm != searchQuery) {
                    newConversationViewModel.search(searchTerm)
                }
            },
            onSearchClicked = {
                searchNavController.navigate(SearchListScreens.SearchPeopleScreen.route)
            },
            onCloseSearchClicked = {
                searchNavController.navigate(SearchListScreens.KnownContactsScreen.route)
            },
            appTopBar = {
                WireCenterAlignedTopAppBar(
                    elevation = 0.dp,
                    title = stringResource(R.string.label_new_conversation),
                    navigationIconType = NavigationIconType.Close,
                    onNavigationPressed = { newConversationViewModel.close() }
                )
            },
            content = {
                NavHost(
                    navController = searchNavController as NavHostController,
                    startDestination = SearchListScreens.KnownContactsScreen.route
                ) {
                    composable(
                        route = SearchListScreens.KnownContactsScreen.route,
                        content = {
                            ContactsScreen(
                                onScrollPositionChanged = { newConversationViewModel.updateScrollPosition(it) },
                                allKnownContact = allKnownContacts,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onAddToGroup = { contact ->
                                    newConversationViewModel.addContactToGroup(contact)
                                },
                                onRemoveFromGroup = { contact ->
                                    newConversationViewModel.removeContactFromGroup(contact)
                                },
                                onOpenUserProfile = { contact ->
                                    newConversationViewModel.openUserProfile(contact, true)
                                },
                                onNewGroupClicked = {
                                    newConversationNavController.navigate(Screen.NewGroupNameScreen.route)
                                }
                            )
                        }
                    )
                    composable(
                        route = SearchListScreens.SearchPeopleScreen.route,
                        content = {
                            SearchPeopleScreen(
                                searchQuery = searchQuery,
                                noneSearchSucceed = noneSearchSucceed,
                                knownContactSearchResult = localContactSearchResult,
                                publicContactSearchResult = publicContactsSearchResult,
                                federatedBackendResultContact = federatedContactSearchResult,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onAddToGroup = { contact -> newConversationViewModel.addContactToGroup(contact) },
                                onRemoveFromGroup = { contact -> newConversationViewModel.removeContactFromGroup(contact) },
                                onOpenUserProfile = { searchContact ->
                                    newConversationViewModel.openUserProfile(
                                        contact = searchContact.contact,
                                        internal = searchContact.internal
                                    )
                                },
                                onNewGroupClicked = {
                                    newConversationNavController.navigate(Screen.NewGroupNameScreen.route)
                                }
                            )
                        }
                    )
                }
            }
        )
    }
}
