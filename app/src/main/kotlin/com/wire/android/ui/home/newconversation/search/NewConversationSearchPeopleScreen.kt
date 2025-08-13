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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.destinations.NewGroupConversationSearchPeopleScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.home.conversations.search.SearchPeopleScreenType
import com.wire.android.ui.home.conversations.search.SearchUsersAndServicesScreen
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.kalium.logic.data.id.QualifiedID

@NewConversationNavGraph(start = true)
@WireDestination(
    style = PopUpNavigationAnimation::class
)
@Composable
fun NewConversationSearchPeopleScreen(
    navigator: Navigator,
    newConversationViewModel: NewConversationViewModel,
) {
    val isSelfTeamMember = newConversationViewModel.newGroupState.isSelfTeamMember ?: false
    val shouldShowChannelPromotion = !isSelfTeamMember
    val showCreateTeamDialog = remember { mutableStateOf(false) }
    SearchUsersAndServicesScreen(
        searchTitle = stringResource(id = R.string.label_new_conversation),
        shouldShowChannelPromotion = shouldShowChannelPromotion,
        isUserAllowedToCreateChannels = newConversationViewModel.isChannelCreationPossible,
        onOpenUserProfile = { contact ->
            OtherUserProfileScreenDestination(QualifiedID(contact.id, contact.domain))
                .let { navigator.navigate(NavigationCommand(it)) }
        },
        onContactChecked = newConversationViewModel::updateSelectedContacts,
        onCreateNewGroup = {
            newConversationViewModel.setIsChannel(false)
            navigator.navigate(NavigationCommand(NewGroupConversationSearchPeopleScreenDestination))
        },
        onCreateNewChannel = {
            if (!shouldShowChannelPromotion) {
                newConversationViewModel.setIsChannel(true)
                navigator.navigate(NavigationCommand(NewGroupConversationSearchPeopleScreenDestination))
            } else {
                showCreateTeamDialog.value = true
            }
        },
        isGroupSubmitVisible = newConversationViewModel.newGroupState.isGroupCreatingAllowed == true,
        onClose = navigator::navigateBack,
        onServiceClicked = { },
        screenType = SearchPeopleScreenType.NEW_CONVERSATION,
        selectedContacts = newConversationViewModel.newGroupState.selectedUsers,
        isAppDiscoveryAllowed = true
    )

    if (showCreateTeamDialog.value) {
        ChannelNotAvailableDialog(
            onDismiss = {
                showCreateTeamDialog.value = false
            },
            onCreateTeam = {
                showCreateTeamDialog.value = false
                navigator.navigate(NavigationCommand(NavGraphs.personalToTeamMigration))
            }
        )
    }
}
