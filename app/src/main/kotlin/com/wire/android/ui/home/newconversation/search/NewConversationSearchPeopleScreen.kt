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

import com.wire.android.navigation.annotation.app.WireNewConversationDestination
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.ramcosta.composedestinations.generated.app.navgraphs.PersonalToTeamMigrationGraph
import com.ramcosta.composedestinations.generated.app.destinations.NewGroupConversationSearchPeopleScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.OtherUserProfileScreenDestination
import com.ramcosta.composedestinations.generated.app.destinations.ServiceDetailsScreenDestination
import com.wire.android.ui.home.conversations.search.SearchPeopleScreenType
import com.wire.android.ui.home.conversations.search.SearchUsersAndAppsScreen
import com.wire.android.ui.home.newconversation.sharedNewConversationViewModel
import com.wire.android.ui.userprofile.service.ServiceDetailsNavArgs
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.BotService
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult

@WireNewConversationDestination(
    start = true,
    style = PopUpNavigationAnimation::class
)
@Composable
fun NewConversationSearchPeopleScreen(
    navigator: Navigator,
) {
    val newConversationViewModel = sharedNewConversationViewModel(navigator)
    val showCreateTeamDialog = remember { mutableStateOf(false) }
    SearchUsersAndAppsScreen(
        searchTitle = stringResource(id = R.string.label_new_conversation),
        shouldShowChannelPromotion = false,
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
            newConversationViewModel.setIsChannel(true)
            navigator.navigate(NavigationCommand(NewGroupConversationSearchPeopleScreenDestination))
        },
        isGroupSubmitVisible = newConversationViewModel.newGroupState.isGroupCreatingAllowed == true,
        onClose = navigator::navigateBack,
        screenType = SearchPeopleScreenType.NEW_CONVERSATION,
        selectedContacts = newConversationViewModel.newGroupState.selectedUsers,
        isAppsTabVisible = (newConversationViewModel.groupOptionsState.isTeamAllowedToUseApps is AppsAllowedResult.Enabled),
        conversationProtocol = null,
        onAppClicked = { contact ->
            val serviceDetailsNavArgsId: ServiceDetailsNavArgs.Id =
                if (newConversationViewModel.groupOptionsState.shouldShowNewAppsUi) {
                    ServiceDetailsNavArgs.Id.AppId(
                        UserId(contact.id, contact.domain)
                    )
                } else {
                    ServiceDetailsNavArgs.Id.BotServiceId(
                        BotService(id = contact.id, provider = contact.domain)
                    )
                }

            navigator.navigate(
                NavigationCommand(
                    ServiceDetailsScreenDestination(
                        null,
                        serviceDetailsNavArgsId
                    )
                )
            )
        }
    )

    if (showCreateTeamDialog.value) {
        ChannelNotAvailableDialog(
            onDismiss = {
                showCreateTeamDialog.value = false
            },
            onCreateTeam = {
                showCreateTeamDialog.value = false
                navigator.navigate(NavigationCommand(PersonalToTeamMigrationGraph))
            }
        )
    }
}
