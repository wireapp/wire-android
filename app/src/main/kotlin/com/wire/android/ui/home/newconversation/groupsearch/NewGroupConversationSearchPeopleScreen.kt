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
package com.wire.android.ui.home.newconversation.groupsearch

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.destinations.NewGroupNameScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.home.conversations.search.SearchPeopleScreenType
import com.wire.android.ui.home.conversations.search.SearchUsersAndServicesScreen
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.kalium.logic.data.id.QualifiedID

@NewConversationNavGraph
@WireDestination
@Composable
fun NewGroupConversationSearchPeopleScreen(
    navigator: Navigator,
    newConversationViewModel: NewConversationViewModel,
) {
    val onBackClicked = remember(Unit) {
        {
            newConversationViewModel.resetState()
            navigator.navigateBack()
        }
    }

    BackHandler(true, onBackClicked)
    val isSelfTeamMember = newConversationViewModel.newGroupState.isSelfTeamMember ?: false

    val screenTitle = if (newConversationViewModel.newGroupState.isChannel) {
        stringResource(id = R.string.label_new_channel)
    } else {
        stringResource(id = R.string.label_new_group)
    }
    SearchUsersAndServicesScreen(
        searchTitle = screenTitle,
        onOpenUserProfile = { contact ->
            OtherUserProfileScreenDestination(QualifiedID(contact.id, contact.domain))
                .let { navigator.navigate(NavigationCommand(it)) }
        },
        shouldShowChannelPromotion = !isSelfTeamMember,
        isUserAllowedToCreateChannels = false,
        onContactChecked = newConversationViewModel::updateSelectedContacts,
        onContinue = { navigator.navigate(NavigationCommand(NewGroupNameScreenDestination)) },
        isGroupSubmitVisible = newConversationViewModel.newGroupState.isGroupCreatingAllowed == true,
        onClose = onBackClicked,
        onServiceClicked = { },
        screenType = SearchPeopleScreenType.NEW_GROUP_CONVERSATION,
        selectedContacts = newConversationViewModel.newGroupState.selectedUsers,
    )
}
