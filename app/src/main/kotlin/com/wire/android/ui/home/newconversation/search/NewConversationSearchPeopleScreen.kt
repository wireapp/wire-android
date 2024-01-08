/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */
package com.wire.android.ui.home.newconversation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.destinations.NewGroupNameScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.home.conversations.search.SearchAllPeopleViewModel
import com.wire.android.ui.home.conversations.search.SearchPeopleScreen
import com.wire.android.ui.home.conversations.search.SearchPeopleScreenType
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.kalium.logic.data.id.QualifiedID

@NewConversationNavGraph(start = true)
@Destination(
    style = PopUpNavigationAnimation::class
)
@Composable
fun NewConversationSearchPeopleScreen(
    navigator: Navigator,
    newConversationViewModel: NewConversationViewModel,
) {
    val searchAllPeopleViewModel = newConversationViewModel as SearchAllPeopleViewModel
    SearchPeopleScreen(
        searchPeopleState = searchAllPeopleViewModel.state,
        searchTitle = stringResource(id = R.string.label_new_conversation),
        actionButtonTitle = stringResource(id = R.string.label_new_group),
        onSearchQueryChanged = searchAllPeopleViewModel::searchQueryChanged,
        onOpenUserProfile = { contact ->
            OtherUserProfileScreenDestination(QualifiedID(contact.id, contact.domain))
                .let { navigator.navigate(NavigationCommand(it)) }
        },
        onAddContactToGroup = searchAllPeopleViewModel::addContactToGroup,
        onRemoveContactFromGroup = searchAllPeopleViewModel::removeContactFromGroup,
        onAddContact = searchAllPeopleViewModel::addContact,
        onGroupSelectionSubmitAction = { navigator.navigate(NavigationCommand(NewGroupNameScreenDestination)) },
        onClose = navigator::navigateBack,
        onServiceClicked = { },
        screenType = SearchPeopleScreenType.NEW_CONVERSATION
    )

    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        searchAllPeopleViewModel.infoMessage.collect {
            snackbarHostState.showSnackbar(it.asString(context.resources))
        }
    }
}
