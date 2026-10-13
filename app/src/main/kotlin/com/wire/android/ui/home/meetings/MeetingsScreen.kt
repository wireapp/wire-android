/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.meetings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wire.android.feature.meetings.ui.AllMeetingsScreen
import com.wire.android.feature.meetings.ui.NewMeetingBottomSheet
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.dimensions
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.android.feature.meetings.ui.create.NewMeetingDetailsRoute
import com.wire.android.feature.meetings.ui.create.NewMeetingNavigation3ResultType
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.ui.calling.meetingsCallViewModel
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.home.HomeShellState
import com.wire.android.ui.home.conversations.call.HandleActions
import com.wire.android.ui.home.conversations.call.HandleJoinOrStartCallScreenDialogs
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.navigation.WireNavResult
import com.wire.navigation.WireNavResultRequestId
import com.wire.navigation.WireSessionId

/**
 * Meetings screen used by the Navigation 3 Home shell.
 */
@Composable
internal fun MeetingsScreen(
    homeShellState: HomeShellState,
    sessionId: WireSessionId,
    runtime: WireNavigation3Runtime,
    viewModel: MeetingsCallViewModel = meetingsCallViewModel(),
) {
    val context = LocalContext.current
    var pendingRequestIdValue by rememberSaveable(sessionId.value, sessionId.domain) { mutableStateOf<String?>(null) }
    val currentEntryId = runtime.navigator.currentRoute?.entryId
    LaunchedEffect(pendingRequestIdValue, currentEntryId) {
        val requestId = pendingRequestIdValue?.let(::WireNavResultRequestId) ?: return@LaunchedEffect
        when (val result = runtime.consumeResult(requestId, NewMeetingNavigation3ResultType)) {
            is WireNavResult.Value -> {
                pendingRequestIdValue = null
                result.value.conversationIdToStartACall?.let(viewModel::startCallIfPossible)
            }
            WireNavResult.Canceled -> pendingRequestIdValue = null
            null -> Unit
        }
    }
    val openNewMeeting: (NewMeetingType) -> Unit = { type ->
        pendingRequestIdValue = runtime.navigateForResult(
            destination = NewMeetingDetailsRoute.start(sessionId, type),
            resultType = NewMeetingNavigation3ResultType,
        )?.value
    }
    AllMeetingsScreen(
        lazyListState = homeShellState.lazyListStateFor(HomeDestination.Meetings),
        contentPadding = PaddingValues(bottom = dimensions().spacing80x), // to ensure last item is not obscured by FAB
        startCall = { conversationId ->
            viewModel.callManager.startCallIfPossible(
                conversationId = conversationId,
                conversationType = Conversation.Type.Group.Regular,
                shouldCheckParticipantCount = false, // since this is a meeting, we don't need to check participant count
            )
        },
        joinCall = { conversationId ->
            viewModel.callManager.joinOngoingCall(conversationId = conversationId)
        },
        returnToCall = { conversationId ->
            context.startActivity(
                getOngoingCallIntent(
                    context = context,
                    conversationId = conversationId.toString(),
                    userId = viewModel.callManager.currentAccount.toString(),
                )
            )
        },
        editMeeting = { meetingId ->
            openNewMeeting(NewMeetingType.Edit(meetingId))
        },
    )

    viewModel.callManager.actions.HandleActions()
    viewModel.callManager.HandleJoinOrStartCallScreenDialogs()

    NewMeetingBottomSheet(
        sheetState = homeShellState.newMeetingBottomSheetState,
        onMeetNowClick = {
            homeShellState.newMeetingBottomSheetState.hide {
                openNewMeeting(NewMeetingType.MeetNow)
            }
        },
        onScheduleClick = {
            homeShellState.newMeetingBottomSheetState.hide {
                openNewMeeting(NewMeetingType.Schedule)
            }
        }
    )
}
