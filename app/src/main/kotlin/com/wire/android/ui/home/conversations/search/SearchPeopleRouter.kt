package com.wire.android.ui.home.conversations.search

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.conversations.details.AddMembersToConversationViewModel
import com.wire.android.ui.home.newconversation.common.SearchListScreens
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsRow
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.model.Contact

@Composable
fun AddPeopleToConversationRouter(
    purpose: SearchPeoplePurpose,
    addMembersToConversationViewModel: AddMembersToConversationViewModel = hiltViewModel(),
    navHostController: NavHostController = rememberNavController()
) {
    SearchPeopleRouter(
        purpose = purpose,
        onPeoplePicked = {
            addMembersToConversationViewModel.addMembersToConversation()
        },
        navHostController = navHostController,
        searchPeopleViewModel = addMembersToConversationViewModel
    )
}

@Composable
fun SearchPeopleRouter(
    purpose: SearchPeoplePurpose,
    onPeoplePicked: () -> Unit,
    searchPeopleViewModel: SearchPeopleViewModel,
    navHostController: NavHostController = rememberNavController(),
) {
    SearchPeopleContent(
        purpose = purpose,
        searchPeopleState = searchPeopleViewModel.state,
        onPeoplePicked = onPeoplePicked,
        onSearchQueryChanged = searchPeopleViewModel::searchQueryChanged,
        onClose = searchPeopleViewModel::close,
        onAddContactToGroup = searchPeopleViewModel::addContactToGroup,
        onRemoveContactFromGroup = searchPeopleViewModel::removeContactFromGroup,
        onOpenUserProfile = { searchPeopleViewModel.openUserProfile(it.contact) },
        onAddContact = searchPeopleViewModel::addContact,
        searchNavController = navHostController,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPeopleContent(
    purpose: SearchPeoplePurpose,
    searchPeopleState: SearchPeopleState,
    onPeoplePicked: () -> Unit,
    onSearchQueryChanged: (TextFieldValue) -> Unit,
    onClose: () -> Unit,
    onAddContactToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (SearchOpenUserProfile) -> Unit,
    onAddContact: (Contact) -> Unit,
    searchNavController: NavHostController,
) {
    val searchBarState = rememberSearchbarState()

    with(searchPeopleState) {
        CollapsingTopBarScaffold(
            topBarHeader = { elevation ->
                AnimatedVisibility(
                    visible = !searchBarState.isSearchActive,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Box(modifier = Modifier.wrapContentSize()) {
                        WireCenterAlignedTopAppBar(
                            elevation = elevation,
                            title = stringResource(id = purpose.titleTextResId),
                            navigationIconType = NavigationIconType.Close,
                            onNavigationPressed = onClose
                        )
                    }
                }
            },
            topBarCollapsing = {
                val onInputClicked: () -> Unit = remember(searchBarState) {
                    {
                        searchBarState.openSearch()
                        searchNavController.navigate(SearchListScreens.SearchPeopleScreen.route)
                    }
                }
                val onCloseSearchClicked: () -> Unit = remember {
                    {
                        searchBarState.closeSearch()
                        searchNavController.popBackStack()
                    }
                }
                SearchTopBar(
                    isSearchActive = searchBarState.isSearchActive,
                    isSearchBarCollapsed = searchBarState.isSearchBarCollapsed,
                    searchBarHint = stringResource(R.string.label_search_people),
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onInputClicked = onInputClicked,
                    onCloseSearchClicked = onCloseSearchClicked
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
                                allKnownContactResult = allKnownContacts,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onAddToGroup = onAddContactToGroup,
                                onRemoveFromGroup = onRemoveContactFromGroup,
                                onOpenUserProfile =  remember { { onOpenUserProfile(SearchOpenUserProfile(it)) } }
                            )
                        }
                    )
                    composable(
                        route = SearchListScreens.SearchPeopleScreen.route,
                        content = {
                            SearchPeopleScreen(
                                searchQuery = searchQuery.text,
                                noneSearchSucceed = noneSearchSucceed,
                                knownContactSearchResult = localContactSearchResult,
                                publicContactSearchResult = publicContactsSearchResult,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onAddToGroup = onAddContactToGroup,
                                onRemoveFromGroup = onRemoveContactFromGroup,
                                onOpenUserProfile = remember { { onOpenUserProfile(SearchOpenUserProfile(it.contact)) } },
                                onAddContactClicked = onAddContact
                            )
                        }
                    )
                }
            },
            bottomBar = {
                SelectParticipantsButtonsRow(
                    count = contactsAddedToGroup.size,
                    mainButtonText = stringResource(id = purpose.continueButtonTextResId),
                    onMainButtonClick = onPeoplePicked
                )
            },
            snapOnFling = false,
            keepElevationWhenCollapsed = true
        )
    }

    BackHandler(searchBarState.isSearchActive) {
        searchBarState.closeSearch()
        searchNavController.popBackStack()
    }
}

enum class SearchPeoplePurpose(
    @StringRes val titleTextResId: Int,
    @StringRes val continueButtonTextResId: Int
) {
    NEW_CONVERSATION(R.string.label_new_conversation, R.string.label_new_group),
    ADD_PARTICIPANTS(R.string.label_add_participants, R.string.label_continue);
}
