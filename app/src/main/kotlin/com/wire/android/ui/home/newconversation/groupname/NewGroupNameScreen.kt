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

package com.wire.android.ui.home.newconversation.groupname
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.common.groupname.GroupNameMode
import com.wire.android.ui.common.groupname.GroupNameScreen
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.GroupOptionScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.NewGroupConversationSearchPeopleScreenDestination
import com.wire.android.ui.home.newconversation.NewConversationViewModel
import com.wire.android.ui.home.newconversation.common.CreateGroupErrorDialog
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.android.ui.home.newconversation.common.NewConversationNavGraph
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId

@NewConversationNavGraph
@Destination<WireRootNavGraph>
@Composable
fun NewGroupNameScreen(
    navigator: Navigator,
    newConversationViewModel: NewConversationViewModel,
) {
    fun navigateToGroup(conversationId: ConversationId): Unit =
        navigator.navigate(NavigationCommand(ConversationScreenDestination(conversationId), BackStackMode.REMOVE_CURRENT_NESTED_GRAPH))

    LaunchedEffect(Unit) {
        newConversationViewModel.observeGroupNameChanges()
    }
    LaunchedEffect(newConversationViewModel.createGroupState) {
        (newConversationViewModel.createGroupState as? CreateGroupState.Created)?.let {
            navigateToGroup(it.conversationId)
        }
    }
    GroupNameScreen(
        newGroupState = newConversationViewModel.newGroupState,
        newGroupNameTextState = newConversationViewModel.newGroupNameTextState,
        onContinuePressed = {
            if (newConversationViewModel.newGroupState.isSelfTeamMember == true) {
                navigator.navigate(NavigationCommand(GroupOptionScreenDestination))
            } else {
                newConversationViewModel.createGroup()
            }
        },
        onGroupNameErrorAnimated = newConversationViewModel::onGroupNameErrorAnimated,
        onBackPressed = navigator::navigateBack
    )
    (newConversationViewModel.createGroupState as? CreateGroupState.Error)?.let {
        CreateGroupErrorDialog(
            error = it,
            onDismiss = newConversationViewModel::onCreateGroupErrorDismiss,
            onEditParticipantsList = {
                newConversationViewModel.onCreateGroupErrorDismiss()
                navigator.navigate(NavigationCommand(NewGroupConversationSearchPeopleScreenDestination, BackStackMode.UPDATE_EXISTED))
            },
            onCancel = {
                newConversationViewModel.onCreateGroupErrorDismiss()
                navigator.navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
            },
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewNewGroupScreen() = WireTheme {
    GroupNameScreen(GroupMetadataState(mode = GroupNameMode.CREATION), TextFieldState(), {}, {}, {})
}
