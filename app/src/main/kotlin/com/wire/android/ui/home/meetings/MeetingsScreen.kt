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

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.feature.meetings.ui.AllMeetingsScreen
import com.wire.android.feature.meetings.ui.MeetingsHomeNavigationActions
import com.wire.android.feature.meetings.ui.NewMeetingBottomSheet
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dimensions
import com.wire.android.feature.meetings.ui.create.NewMeetingType
import com.wire.android.ui.calling.meetingsCallViewModel
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.HomeShellState
import com.wire.android.ui.home.conversations.call.HandleActions
import com.wire.android.ui.home.conversations.call.HandleJoinOrStartCallScreenDialogs
import com.wire.kalium.logic.data.conversation.Conversation

/**
 * Navigation-neutral Meetings renderer used by the Navigation 3 Home shell.
 */
@Composable
internal fun MeetingsScreen(
    homeShellState: HomeShellState,
    navigationActions: MeetingsHomeNavigationActions,
    viewModel: MeetingsCallViewModel = meetingsCallViewModel(),
) {
    val context = LocalContext.current
    val alarmManager = remember(context) { context.getSystemService(AlarmManager::class.java) }
    val showExactAlarmAccessDialogState = rememberVisibilityState<Unit>()

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms() &&
            !viewModel.isExactAlarmAccessDialogSeen()
        ) {
            viewModel.markExactAlarmAccessDialogSeen()
            showExactAlarmAccessDialogState.show(Unit)
        }
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
            navigationActions.openNewMeeting(NewMeetingType.Edit(meetingId))
        },
    )

    viewModel.callManager.actions.HandleActions()
    viewModel.callManager.HandleJoinOrStartCallScreenDialogs()

    NewMeetingBottomSheet(
        sheetState = homeShellState.newMeetingBottomSheetState,
        onMeetNowClick = {
            homeShellState.newMeetingBottomSheetState.hide {
                navigationActions.openNewMeeting(NewMeetingType.MeetNow)
            }
        },
        onScheduleClick = {
            homeShellState.newMeetingBottomSheetState.hide {
                navigationActions.openNewMeeting(NewMeetingType.Schedule)
            }
        }
    )

    VisibilityState(showExactAlarmAccessDialogState) {
        WireDialog(
            title = stringResource(R.string.meeting_reminder_exact_alarm_title),
            text = stringResource(R.string.meeting_reminder_exact_alarm_description),
            onDismiss = showExactAlarmAccessDialogState::dismiss,
            optionButton1Properties = WireDialogButtonProperties(
                text = stringResource(R.string.meeting_reminder_exact_alarm_settings),
                type = WireDialogButtonType.Primary,
                onClick = {
                    showExactAlarmAccessDialogState.dismiss()
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    )
                },
            ),
            optionButton2Properties = WireDialogButtonProperties(
                text = stringResource(R.string.label_cancel),
                type = WireDialogButtonType.Primary,
                onClick = showExactAlarmAccessDialogState::dismiss,
            ),
        )
    }
}
