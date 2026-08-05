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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ramcosta.composedestinations.generated.meetings.destinations.NewMeetingScreenDestination
import com.wire.android.feature.meetings.ui.AllMeetingsScreen
import com.wire.android.feature.meetings.ui.NewMeetingBottomSheet
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.annotation.app.WireHomeDestination
import com.wire.android.ui.calling.meetingsCallViewModel
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.conversations.call.HandleActions
import com.wire.android.ui.home.conversations.call.HandleJoinOrStartCallScreenDialogs
import com.wire.android.feature.meetings.R as meetingsR
import com.wire.android.ui.common.R as commonR

@WireHomeDestination
@Composable
fun MeetingsScreen(
    homeStateHolder: HomeStateHolder,
    viewModel: MeetingsCallViewModel = meetingsCallViewModel()
) {
    val context = LocalContext.current
    AllMeetingsScreen(
        lazyListState = homeStateHolder.lazyListStateFor(HomeDestination.Meetings),
        contentPadding = PaddingValues(bottom = dimensions().spacing80x), // to ensure last item is not obscured by FAB
        startCall = { conversationId ->
            viewModel.startCallIfPossible(conversationId = conversationId)
        },
        joinCall = { conversationId ->
            viewModel.joinOngoingCall(conversationId = conversationId)
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
            homeStateHolder.navigator.navigate(NavigationCommand(NewMeetingScreenDestination(NewMeetingType.Edit(meetingId))))
        },
    )

    viewModel.callManager.actions.HandleActions()
    viewModel.callManager.HandleJoinOrStartCallScreenDialogs()

    NotEstablishedDialog(dialogState = viewModel.notEstablishedDialogState)

    NewMeetingBottomSheet(
        sheetState = homeStateHolder.newMeetingBottomSheetState,
        onMeetNowClick = {
            homeStateHolder.newMeetingBottomSheetState.hide {
                homeStateHolder.navigator.navigate(NavigationCommand(NewMeetingScreenDestination(NewMeetingType.MeetNow)))
            }
        },
        onScheduleClick = {
            homeStateHolder.newMeetingBottomSheetState.hide {
                homeStateHolder.navigator.navigate(NavigationCommand(NewMeetingScreenDestination(NewMeetingType.Schedule)))
            }
        }
    )
}

@Composable
private fun NotEstablishedDialog(dialogState: VisibilityState<Unit>) {
    VisibilityState(dialogState) {
        WireDialog(
            title = stringResource(meetingsR.string.meeting_join_failure_title),
            text = stringResource(id = meetingsR.string.meeting_join_failure_description),
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            optionButton1Properties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = commonR.string.label_ok),
                type = WireDialogButtonType.Primary,
                state = WireButtonState.Default,
            )
        )
    }
}
