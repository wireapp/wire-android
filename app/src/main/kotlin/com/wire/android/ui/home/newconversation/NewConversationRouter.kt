package com.wire.android.ui.home.newconversation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.R
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.AppTopBarWithSearchBar
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.search.SearchPeopleScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NewConversationRouter(newConversationViewModel: NewConversationViewModel = hiltViewModel()) {
    val newConversationState = rememberNewConversationState()

    with(newConversationViewModel.state) {
        AppTopBarWithSearchBar(
            scrollPosition = newConversationState.scrollPosition,
            searchBarHint = stringResource(R.string.label_search_people),
            searchQuery = newConversationState.searchQuery,
            onSearchQueryChanged = {
                newConversationState.searchQuery = it
            },
            onSearchClicked = { newConversationState.navigateToSearch() },
            onCloseSearchClicked = {
                newConversationState.clearSearchQuery()
                newConversationState.navigateBack()
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
                AnimatedNavHost(
                    navController = newConversationState.navController,
                    startDestination = NewConversationStateScreen.KNOWN_CONTACTS
                ) {
                    composable(
                        route = NewConversationStateScreen.KNOWN_CONTACTS,
                        content = {
                            ContactsScreen(
                                onScrollPositionChanged = { newConversationState.updateScrollPosition(it) },
                                allKnownContact = allKnownContacts,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onOpenUserProfile = { contact -> newConversationViewModel.openUserProfile(contact) },
                                onAddToGroup = { contact -> newConversationViewModel.addContactToGroup(contact) },
                                onRemoveFromGroup = { contact -> newConversationViewModel.removeContactFromGroup(contact) }
                            )
                        }
                    )
                    composable(
                        route = NewConversationStateScreen.SEARCH_PEOPLE,
                        content = {
                            SearchPeopleScreen(
                                searchQuery = searchQuery,
                                noneSearchSucceed = noneSearchSucceed,
                                knownContactSearchResult = localContactSearchResult,
                                publicContactSearchResult = publicContactsSearchResult,
                                federatedBackendResultContact = federatedContactSearchResult,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onOpenUserProfile = { contact -> newConversationViewModel.openUserProfile(contact) },
                                onAddToGroup = { contact -> newConversationViewModel.addContactToGroup(contact) },
                                onRemoveFromGroup = { contact -> newConversationViewModel.removeContactFromGroup(contact) }
                            )
                        }
                    )
                }
            }
        )
    }
}

private class NewConversationStateScreen(
    val navController: NavHostController
) {

    var searchQuery by mutableStateOf("")

    var scrollPosition by mutableStateOf(0)
        private set

    fun updateScrollPosition(newScrollPosition: Int) {
        scrollPosition = newScrollPosition
    }

    fun navigateToSearch() {
        navController.navigate(SEARCH_PEOPLE)
    }

    fun navigateBack() {
        navController.navigate(KNOWN_CONTACTS)
    }

    fun clearSearchQuery() {
        searchQuery = ""
    }

    companion object {
        const val KNOWN_CONTACTS = "known_contacts"
        const val SEARCH_PEOPLE = "search_people"
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun rememberNewConversationState(
    navController: NavHostController = rememberAnimatedNavController()
): NewConversationStateScreen {
    return remember {
        NewConversationStateScreen(navController)
    }
}
