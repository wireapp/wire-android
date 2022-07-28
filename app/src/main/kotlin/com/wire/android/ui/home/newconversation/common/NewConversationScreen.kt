package com.wire.android.ui.home.newconversation.common

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.androidx.AndroidScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.wire.android.ui.home.conversations.search.SearchPeopleRouter
import com.wire.android.ui.home.conversations.search.SearchPeopleScreen
import com.wire.android.ui.home.conversations.search.SearchPeopleViewModel
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionScreen
import com.wire.android.ui.home.newconversation.newgroup.NewGroupScreen

// TODO simplify these by creating separate ViewModels for each
sealed class NewConversationScreen : AndroidScreen() {
    data class NewGroupName(
        val newConversationViewModel: NewConversationViewModel,
    ) : NewConversationScreen() {

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            NewGroupScreen(
                newGroupState = newConversationViewModel.groupNameState,
                onGroupNameChange = newConversationViewModel::onGroupNameChange,
                onGroupNameErrorAnimated = newConversationViewModel::onGroupNameErrorAnimated,
                onBackPressed = { navigator.pop() },
                onContinuePressed = { navigator.push(GroupOptions(newConversationViewModel)) }
            )
        }
    }

    data class SearchList(
        val searchBarTitle: String,
        val newConversationViewModel: NewConversationViewModel,
    ) : NewConversationScreen() {

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            SearchPeopleRouter(
                searchBarTitle = searchBarTitle,
                searchPeopleViewModel = newConversationViewModel,
            onPeoplePicked = { navigator.push(NewGroupName(newConversationViewModel)) })
        }
    }

    data class GroupOptions(
        val newConversationViewModel: NewConversationViewModel
    ) : NewConversationScreen() {

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            GroupOptionScreen(
                onCreateGroup = newConversationViewModel::createGroup,
                groupOptionState = newConversationViewModel.groupOptionsState,
                onAllowGuestChanged = newConversationViewModel::onAllowGuestStatusChanged,
                onAllowServicesChanged = newConversationViewModel::onAllowServicesStatusChanged,
                onReadReceiptChanged = newConversationViewModel::onReadReceiptStatusChanged,
                onAllowGuestsDialogDismissed = newConversationViewModel::onAllowGuestsDialogDismissed,
                onAllowGuestsClicked = newConversationViewModel::onAllowGuestsClicked,
                onNotAllowGuestsClicked = newConversationViewModel::onNotAllowGuestClicked,
                onBackPressed = { navigator.pop() }
            )
        }
    }
}

// TODO simplify these by creating separate ViewModels for each
sealed class SearchListScreen : AndroidScreen() {

    data class KnownContacts(
        val searchPeopleViewModel: SearchPeopleViewModel,
        val scrollPositionProvider: (() -> Int) -> Unit,
        val onNewGroupClicked: () -> Unit
    ) : SearchListScreen() {

        @Composable
        override fun Content() {
            ContactsScreen(
                scrollPositionProvider = scrollPositionProvider,
                allKnownContactResult = searchPeopleViewModel.state.allKnownContacts,
                contactsAddedToGroup = searchPeopleViewModel.state.contactsAddedToGroup,
                onOpenUserProfile = searchPeopleViewModel::openUserProfile,
                onAddToGroup = searchPeopleViewModel::addContactToGroup,
                onRemoveFromGroup = searchPeopleViewModel::removeContactFromGroup,
                onNewGroupClicked = onNewGroupClicked
            )
        }
    }

    data class SearchPeople(
        val searchPeopleViewModel: SearchPeopleViewModel,
        val scrollPositionProvider: (() -> Int) -> Unit,
        val onNewGroupClicked: () -> Unit,
    ) : SearchListScreen() {

        @Composable
        override fun Content() {
            SearchPeopleScreen(
                scrollPositionProvider = scrollPositionProvider,
                searchQuery = searchPeopleViewModel.state.searchQuery,
                noneSearchSucceed = searchPeopleViewModel.state.noneSearchSucceed,
                knownContactSearchResult = searchPeopleViewModel.state.localContactSearchResult,
                publicContactSearchResult = searchPeopleViewModel.state.publicContactsSearchResult,
                contactsAddedToGroup = searchPeopleViewModel.state.contactsAddedToGroup,
                onAddToGroup = searchPeopleViewModel::addContactToGroup,
                onRemoveFromGroup = searchPeopleViewModel::removeContactFromGroup,
                onOpenUserProfile = searchPeopleViewModel::openUserProfile,
                onNewGroupClicked = onNewGroupClicked,
                onAddContactClicked = searchPeopleViewModel::addContact
            )
        }
    }
}
