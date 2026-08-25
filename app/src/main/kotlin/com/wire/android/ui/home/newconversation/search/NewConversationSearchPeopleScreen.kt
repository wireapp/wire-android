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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.ItemActionType
import com.wire.android.search.SearchUsersAndAppsScreen
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.CreateRegularGroupOrChannelButtons
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult

/**
 * Navigation-neutral adapter for the root step. Primitive identity values keep its callback
 * boundary independent from legacy Parcelable destination arguments.
 */
@Composable
internal fun NewConversationSearchPeopleRouteScreen(
    newConversationViewModel: NewConversationViewModel,
    onNavigateBack: () -> Unit,
    onOpenUserProfile: (value: String, domain: String) -> Unit,
    onOpenServiceDetails: (value: String, domain: String, useNewAppsUi: Boolean) -> Unit,
    onStartGroupOrChannel: () -> Unit,
    onOpenTeamPlan: () -> Unit,
) {
    val showCreateTeamDialog = remember { mutableStateOf(false) }
    SearchUsersAndAppsScreen(
        searchTitle = stringResource(id = R.string.label_new_conversation),
        onOpenUserProfile = { contact ->
            onOpenUserProfile(contact.id, contact.domain)
        },
        onContactChecked = newConversationViewModel::updateSelectedContacts,
        onClose = onNavigateBack,
        navigationIconType = NavigationIconType.Close(R.string.content_description_new_conversation_close_btn),
        itemActionType = ItemActionType.CLICK,
        shouldHideBottomActionForSearch = true,
        selectedContacts = newConversationViewModel.newGroupState.selectedUsers,
        isAppsTabVisible = (newConversationViewModel.groupOptionsState.isTeamAllowedToUseApps is AppsAllowedResult.Enabled),
        conversationProtocol = null,
        onAppClicked = { contact ->
            onOpenServiceDetails(
                contact.id,
                contact.domain,
                newConversationViewModel.groupOptionsState.shouldShowNewAppsUi,
            )
        },
        peopleBottomActions = if (newConversationViewModel.newGroupState.isGroupCreatingAllowed == true) {
            { focusRequester ->
                CreateRegularGroupOrChannelButtons(
                    shouldShowChannelPromotion = false,
                    isUserAllowedToCreateChannels = newConversationViewModel.isChannelCreationPossible,
                    onCreateNewRegularGroup = {
                        newConversationViewModel.setIsChannel(false)
                        onStartGroupOrChannel()
                    },
                    onCreateNewChannel = {
                        newConversationViewModel.setIsChannel(true)
                        onStartGroupOrChannel()
                    },
                    firstVisibleButtonModifier = Modifier.focusRequester(focusRequester),
                )
            }
        } else {
            null
        }
    )

    if (showCreateTeamDialog.value) {
        ChannelNotAvailableDialog(
            onDismiss = {
                showCreateTeamDialog.value = false
            },
            onCreateTeam = {
                showCreateTeamDialog.value = false
                onOpenTeamPlan()
            }
        )
    }
}
