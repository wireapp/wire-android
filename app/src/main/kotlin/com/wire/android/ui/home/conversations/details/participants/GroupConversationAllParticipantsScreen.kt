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

package com.wire.android.ui.home.conversations.details.participants
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.destinations.ServiceDetailsScreenDestination
import com.wire.android.ui.home.conversations.details.participants.model.ParticipantsExpansionState
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.theme.WireTheme

@Destination<WireRootNavGraph>(
    navArgs = GroupConversationAllParticipantsNavArgs::class
)
@Composable
fun GroupConversationAllParticipantsScreen(
    navigator: Navigator,
    navArgs: GroupConversationAllParticipantsNavArgs,
    viewModel: GroupConversationParticipantsViewModel = hiltViewModel()
) {
    GroupConversationAllParticipantsContent(
        onBackPressed = navigator::navigateBack,
        groupParticipantsState = viewModel.groupParticipantsState,
        onProfilePressed = { participant ->
            when {
                participant.isSelf -> navigator.navigate(NavigationCommand(SelfUserProfileScreenDestination))
                participant.isService && participant.botService != null ->
                    navigator.navigate(NavigationCommand(ServiceDetailsScreenDestination(participant.botService, navArgs.conversationId)))

                else -> navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(participant.id, navArgs.conversationId)))
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupConversationAllParticipantsContent(
    onBackPressed: () -> Unit,
    onProfilePressed: (UIParticipant) -> Unit,
    groupParticipantsState: GroupConversationParticipantsState
) {
    val lazyListState: LazyListState = rememberLazyListState()
    val participantsExpansionState = remember { ParticipantsExpansionState() }
    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                elevation = lazyListState.rememberTopBarElevationState().value,
                title = stringResource(R.string.conversation_details_participants_title),
                navigationIconType = NavigationIconType.Back(),
                onNavigationPressed = onBackPressed
            ) {
                // TODO add search bar
            }
        },
        modifier = Modifier.fillMaxHeight(),
    ) { internalPadding ->
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            val context = LocalContext.current
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(internalPadding)
            ) {
                participantsFoldersWithElements(
                    context,
                    groupParticipantsState,
                    onProfilePressed,
                    participantsExpansionState
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewGroupConversationAllParticipants() {
    WireTheme {
        GroupConversationAllParticipantsContent({}, {}, GroupConversationParticipantsState.PREVIEW)
    }
}
