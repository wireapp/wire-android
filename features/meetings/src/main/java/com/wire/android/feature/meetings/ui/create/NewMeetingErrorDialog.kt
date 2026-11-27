/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.feature.meetings.ui.create

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.wire.android.model.Contact
import com.wire.kalium.logic.feature.meeting.MeetingSaveWarning
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.android.ui.common.R as commonR

@Composable
fun NewMeetingErrorDialog(
    error: NewMeetingState.SubmitError,
    type: NewMeetingType,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onRetryUpdateConversationName: (conversationId: ConversationId) -> Unit,
    contacts: List<Contact> = emptyList(),
) {
    val (titleResId, descriptionResId) = error.resources(type)
    val warning = (error as? NewMeetingState.SubmitError.Saved)?.warning
    val details = when (warning) {
        is MeetingSaveWarning.AddParticipantsFailed -> meetingParticipantFailureText(warning.participants, contacts)
        else -> AnnotatedString("")
    }

    WireDialog(
        title = stringResource(titleResId),
        text = AnnotatedString(stringResource(descriptionResId)) + details,
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = when (error) {
            is NewMeetingState.SubmitError.UpdateConversationNameFailure -> WireDialogButtonProperties(
                onClick = {
                    onRetryUpdateConversationName(error.conversationId)
                },
                text = stringResource(R.string.new_meeting_edit_conversation_name_failure_action),
                type = WireDialogButtonType.Primary,
                state = if (isSubmitting) WireButtonState.Disabled else WireButtonState.Default,
                loading = isSubmitting,
            )

            else -> WireDialogButtonProperties(
                onClick = onDismiss,
                text = stringResource(commonR.string.label_ok),
                type = WireDialogButtonType.Primary,
            )
        }
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewCreateGroupErrorDialogLackingConnection() = WireTheme {
    NewMeetingErrorDialog(
        error = NewMeetingState.SubmitError.Other,
        type = NewMeetingType.MeetNow,
        isSubmitting = false,
        onDismiss = {},
        onRetryUpdateConversationName = {},
    )
}

private fun NewMeetingState.SubmitError.resources(type: NewMeetingType): Pair<Int, Int> = when (this) {
    is NewMeetingState.SubmitError.UpdateConversationNameFailure ->
        R.string.new_meeting_edit_conversation_name_failure_title to R.string.new_meeting_edit_conversation_name_failure_description

    NewMeetingState.SubmitError.InvalidEdit -> R.string.new_meeting_edit_failure_title to R.string.new_meeting_edit_id_missing
    is NewMeetingState.SubmitError.Saved -> when (warning) {
        MeetingSaveWarning.ConversationSetupFailed ->
            R.string.new_meeting_setup_failure_title to R.string.new_meeting_setup_failure_description
        is MeetingSaveWarning.AddParticipantsFailed ->
            R.string.new_meeting_setup_failure_title to R.string.new_meeting_add_participants_failure
        MeetingSaveWarning.RemoveParticipantsFailed ->
            R.string.new_meeting_edit_failure_title to R.string.new_meeting_remove_participants_failure
    }
    is NewMeetingState.SubmitError.Other -> when (type) {
        NewMeetingType.MeetNow -> R.string.new_meeting_now_failure_title to R.string.new_meeting_now_failure_description
        NewMeetingType.Schedule -> R.string.new_meeting_schedule_failure_title to R.string.new_meeting_schedule_failure_description
        is NewMeetingType.Edit -> R.string.new_meeting_edit_failure_title to R.string.new_meeting_edit_failure_description
    }
}
