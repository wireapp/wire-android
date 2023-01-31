/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.ui.home.conversations.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.conversations.details.AddMembersToConversationViewModel
import com.wire.android.ui.home.newconversation.common.SearchListScreens
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsAlwaysEnabled
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsRow
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.model.Contact

@Composable
fun AddMembersSearchRouter(
    addMembersToConversationViewModel: AddMembersToConversationViewModel = hiltViewModel()
) {
    SearchPeopleContent(
        searchPeopleState = addMembersToConversationViewModel.state,
        searchTitle = stringResource(id = R.string.label_add_participants),
        actionButtonTitle = stringResource(id = R.string.label_continue),
        onSearchQueryChanged = addMembersToConversationViewModel::searchQueryChanged,
        onOpenUserProfile = addMembersToConversationViewModel::openUserProfile,
        onAddContactToGroup = addMembersToConversationViewModel::addContactToGroup,
        onRemoveContactFromGroup = addMembersToConversationViewModel::removeContactFromGroup,
        // Members search does not has the option to add a contact
        onAddContact = { },
        onGroupSelectionSubmitAction = addMembersToConversationViewModel::addMembersToConversation,
        onClose = addMembersToConversationViewModel::close,
    )
}

@Composable
fun SearchPeopleRouter(
    onGroupSelectionSubmitAction: () -> Unit,
    searchAllPeopleViewModel: SearchAllPeopleViewModel,
) {
    SearchPeopleContent(
        searchPeopleState = searchAllPeopleViewModel.state,
        searchTitle = stringResource(id = R.string.label_new_conversation),
        actionButtonTitle = stringResource(id = R.string.label_new_group),
        onSearchQueryChanged = searchAllPeopleViewModel::searchQueryChanged,
        onOpenUserProfile = searchAllPeopleViewModel::openUserProfile,
        onAddContactToGroup = searchAllPeopleViewModel::addContactToGroup,
        onRemoveContactFromGroup = searchAllPeopleViewModel::removeContactFromGroup,
        onAddContact = searchAllPeopleViewModel::addContact,
        onGroupSelectionSubmitAction = onGroupSelectionSubmitAction,
        onClose = searchAllPeopleViewModel::close,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPeopleContent(
    searchPeopleState: SearchPeopleState,
    searchTitle: String,
    actionButtonTitle: String,
    onSearchQueryChanged: (TextFieldValue) -> Unit,
    onGroupSelectionSubmitAction: () -> Unit,
    onAddContact: (Contact) -> Unit,
    onAddContactToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onClose: () -> Unit
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
                            title = searchTitle,
                            navigationIconType = NavigationIconType.Close,
                            onNavigationPressed = onClose
                        )
                    }
                }
            },
            topBarCollapsing = {
                val onInputClicked: () -> Unit = remember { { searchBarState.openSearch() } }
                val onCloseSearchClicked: () -> Unit = remember { { searchBarState.closeSearch() } }
                SearchTopBar(
                    isSearchActive = searchBarState.isSearchActive,
                    searchBarHint = stringResource(R.string.label_search_people),
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onInputClicked = onInputClicked,
                    onCloseSearchClicked = onCloseSearchClicked
                )
            },
            content = {
                Crossfade(
                    targetState = searchBarState.isSearchActive
                ) { isSearchActive ->
                    if (isSearchActive)
                        SearchAllPeopleScreen(
                            searchQuery = searchQuery.text,
                            noneSearchSucceed = noneSearchSucceed,
                            searchResult = searchResult,
                            contactsAddedToGroup = contactsAddedToGroup,
                            onAddToGroup = onAddContactToGroup,
                            onRemoveFromGroup = onRemoveContactFromGroup,
                            onOpenUserProfile = onOpenUserProfile,
                            onAddContactClicked = onAddContact
                        )
                    else
                        ContactsScreen(
                            allKnownContactResult = initialContacts,
                            contactsAddedToGroup = contactsAddedToGroup,
                            onAddToGroup = onAddContactToGroup,
                            onRemoveFromGroup = onRemoveContactFromGroup,
                            onOpenUserProfile = onOpenUserProfile
                        )
                }
                BackHandler(enabled = searchBarState.isSearchActive) {
                    searchBarState.closeSearch()
                }
            },
            bottomBar = {
                if (searchPeopleState.isGroupCreationContext) {
                    SelectParticipantsButtonsAlwaysEnabled(
                        count = contactsAddedToGroup.size,
                        mainButtonText = actionButtonTitle,
                        onMainButtonClick = onGroupSelectionSubmitAction
                    )
                } else {
                    SelectParticipantsButtonsRow(
                        count = contactsAddedToGroup.size,
                        mainButtonText = actionButtonTitle,
                        onMainButtonClick = onGroupSelectionSubmitAction
                    )
                }
            },
            snapOnFling = false,
            keepElevationWhenCollapsed = true
        )
    }
}
