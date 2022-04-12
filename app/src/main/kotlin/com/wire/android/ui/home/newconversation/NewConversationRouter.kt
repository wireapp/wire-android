package com.wire.android.ui.home.newconversation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.AppTopBarWithSearchBar
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.newGroup.NewGroupScreen
import com.wire.android.ui.home.newconversation.search.SearchPeopleScreen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun NewConversationRouter(newConversationViewModel: NewConversationViewModel = hiltViewModel()) {
    val newConversationState = rememberNewConversationState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(newConversationViewModel) {
        newConversationViewModel.moveToStep.onEach { item ->
            when (item) {
                NewConversationNavigationCommand.KnownContacts -> newConversationState.navigateToKnownContacts()
                NewConversationNavigationCommand.SearchContacts -> newConversationState.navigateToSearch()
                NewConversationNavigationCommand.NewGroup -> newConversationState.navigateToNewGroup()
            }
        }.launchIn(scope)
    }

    with(newConversationViewModel.state) {
        NavHost(
            navController = newConversationState.navController,
            startDestination = NewConversationStateScreen.KNOWN_CONTACTS
        ) {
            composable(
                route = NewConversationStateScreen.KNOWN_CONTACTS,
                content = {
                    ScreenWithSearchTopBar(
                        newConversationViewModel = newConversationViewModel,
                        searchQuery = searchQuery,
                        scrollPosition = newConversationState.scrollPosition,
                        content = {
                            ContactsScreen(
                                onScrollPositionChanged = { newConversationState.updateScrollPosition(it) },
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
                                onNewGroupClicked = { newConversationViewModel.openNewGroupScreen() }
                            )
                        }
                    )

                }
            )
            composable(
                route = NewConversationStateScreen.SEARCH_PEOPLE,
                content = {
                    ScreenWithSearchTopBar(
                        newConversationViewModel = newConversationViewModel,
                        searchQuery = searchQuery,
                        scrollPosition = newConversationState.scrollPosition,
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
                                onNewGroupClicked = { newConversationViewModel.openNewGroupScreen() }
                            )

                        }
                    )
                }
            )
            composable(
                route = NewConversationStateScreen.NEW_GROUP,
                content = {
                    NewGroupScreen(onBackPressed = { newConversationState.navigateBack() })
                }
            )

        }
    }
}

@Composable
fun ScreenWithSearchTopBar(
    newConversationViewModel: NewConversationViewModel,
    searchQuery: String,
    scrollPosition: Int,
    content: @Composable () -> Unit
) {
    AppTopBarWithSearchBar(
        scrollPosition = scrollPosition,
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
        onSearchClicked = { newConversationViewModel.openSearchContacts() },
        onCloseSearchClicked = {
            newConversationViewModel.openKnownContacts()
        },
        appTopBar = {
            WireCenterAlignedTopAppBar(
                elevation = 0.dp,
                title = stringResource(R.string.label_new_conversation),
                navigationIconType = NavigationIconType.Close,
                onNavigationPressed = { newConversationViewModel.close() }
            )
        },
        content = content
    )

}

private class NewConversationStateScreen(
    val navController: NavHostController
) {

    var scrollPosition by mutableStateOf(0)
        private set

    fun updateScrollPosition(newScrollPosition: Int) {
        scrollPosition = newScrollPosition
    }

    fun navigateToSearch() {
        navController.navigate(SEARCH_PEOPLE)
    }

    fun navigateToNewGroup() {
        navController.navigate(NEW_GROUP)
    }

    fun navigateToKnownContacts() {
        navController.navigate(KNOWN_CONTACTS)
    }

    fun navigateBack() {
        navController.popBackStack()
    }


    companion object {
        const val KNOWN_CONTACTS = "known_contacts"
        const val SEARCH_PEOPLE = "search_people"
        const val NEW_GROUP = "new_group"

    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun rememberNewConversationState(
    navController: NavHostController = rememberNavController()
): NewConversationStateScreen {
    return remember {
        NewConversationStateScreen(navController)
    }
}
