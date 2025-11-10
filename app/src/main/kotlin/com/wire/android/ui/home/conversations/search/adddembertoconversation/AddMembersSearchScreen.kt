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
package com.wire.android.ui.home.conversations.search.adddembertoconversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.ServiceDetailsScreenDestination
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.conversations.search.SearchPeopleScreenType
import com.wire.android.ui.home.conversations.search.SearchUsersAndAppsScreen
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.BotService

@WireDestination(
    navArgsDelegate = AddMembersSearchNavArgs::class
)
@Composable
fun AddMembersSearchScreen(
    navigator: Navigator,
    navArgs: AddMembersSearchNavArgs,
    addMembersToConversationViewModel: AddMembersToConversationViewModel = hiltViewModel(),
) {
    if (addMembersToConversationViewModel.newGroupState.isCompleted) {
        navigator.navigateBack()
    }
    SearchUsersAndAppsScreen(
        searchTitle = stringResource(id = R.string.label_add_participants),
        onOpenUserProfile = { contact: Contact ->
            OtherUserProfileScreenDestination(QualifiedID(contact.id, contact.domain))
                .let { navigator.navigate(NavigationCommand(it)) }
        },
        onContactChecked = addMembersToConversationViewModel::updateSelectedContacts,
        onContinue = addMembersToConversationViewModel::addMembersToConversation,
        isGroupSubmitVisible = true,
        onClose = navigator::navigateBack,
        onAppClicked = { contact: Contact ->
            ServiceDetailsScreenDestination(BotService(contact.id, contact.domain), navArgs.conversationId)
                .let { navigator.navigate(NavigationCommand(it)) }
        },
        screenType = SearchPeopleScreenType.CONVERSATION_DETAILS,
        selectedContacts = addMembersToConversationViewModel.newGroupState.selectedContacts,
        isAppsTabVisible = true,
        isUserAllowedToCreateChannels = false,
        shouldShowChannelPromotion = false,
        isConversationWithAppsEnabled = navArgs.isConversationWithAppsEnabled,
    )
}
