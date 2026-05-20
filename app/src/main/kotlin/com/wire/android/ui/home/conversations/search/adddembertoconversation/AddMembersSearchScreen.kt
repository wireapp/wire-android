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

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.di.metro.metroViewModel
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.ramcosta.composedestinations.generated.app.destinations.OtherUserProfileScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ServiceDetailsScreenDestination
import com.wire.android.ui.home.conversations.search.AddMembersSearchNavArgs
import com.wire.android.ui.home.conversations.search.SearchPeopleScreenType
import com.wire.android.ui.home.conversations.search.SearchUsersAndAppsScreen
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.userprofile.service.ServiceDetailsNavArgs
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.UserId

@WireRootDestination(
    navArgs = AddMembersSearchNavArgs::class
)
@Composable
fun AddMembersSearchScreen(
    navigator: Navigator,
    navArgs: AddMembersSearchNavArgs,
    addMembersToConversationViewModel: AddMembersToConversationViewModel = metroViewModel {
        addMembersToConversationViewModelFactory.create(navArgs)
    },
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
            val serviceId = when (navArgs.shouldUseNewAppsUi) {
                true -> ServiceDetailsNavArgs.Id.AppId(UserId(contact.id, contact.domain))
                false -> ServiceDetailsNavArgs.Id.BotServiceId(BotService(contact.id, contact.domain))
            }

            ServiceDetailsScreenDestination(
                navArgs.conversationId,
                serviceId
            ).let { navigator.navigate(NavigationCommand(it)) }
        },
        screenType = SearchPeopleScreenType.CONVERSATION_DETAILS,
        selectedContacts = addMembersToConversationViewModel.newGroupState.selectedContacts,
        isAppsTabVisible = navArgs.isSelfPartOfATeam,
        isUserAllowedToCreateChannels = false,
        shouldShowChannelPromotion = false,
        isConversationAppsEnabled = navArgs.isConversationAppsEnabled,
        conversationProtocol = navArgs.protocolInfo,
        addMembersSearchNavArgs = navArgs
    )
}
