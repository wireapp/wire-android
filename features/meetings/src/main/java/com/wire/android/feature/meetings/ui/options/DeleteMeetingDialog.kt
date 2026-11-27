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

package com.wire.android.feature.meetings.ui.options

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.meetings.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.kalium.logic.data.id.MeetingId
import com.wire.android.ui.common.R as commonR

@Composable
internal fun DeleteMeetingDialog(
    dialogState: VisibilityState<DeleteMeetingDialogState>,
    onDelete: (DeleteMeetingDialogState) -> Unit,
) {
    VisibilityState(dialogState) { state ->
        val (titleResId, textResId) = state.resources()
        val canRetry = state.error?.retryable != false
        WireDialog(
            properties = wireDialogPropertiesBuilder(dismissOnBackPress = !state.loading, dismissOnClickOutside = !state.loading),
            title = stringResource(id = titleResId),
            text = stringResource(id = textResId),
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = commonR.string.label_cancel),
                state = if (state.loading) WireButtonState.Disabled else WireButtonState.Default,
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = {
                    if (canRetry) onDelete(state) else dialogState.dismiss()
                },
                text = stringResource(id = if (state.error != null && canRetry) R.string.meetings_retry else commonR.string.label_ok),
                type = WireDialogButtonType.Primary,
                state = when {
                    state.loading -> WireButtonState.Disabled
                    canRetry -> WireButtonState.Error
                    else -> WireButtonState.Default
                },
                loading = state.loading
            )
        )
    }
}

private fun DeleteMeetingDialogState.resources(): Pair<Int, Int> = error?.let {
    it.titleRes to it.descriptionRes
} ?: when (deleteType) {
    DeleteMeetingType.ForEveryone -> R.string.delete_meeting_for_everyone_title to when (isRecurring) {
        true -> R.string.delete_meeting_for_everyone_recurring_description
        false -> R.string.delete_meeting_for_everyone_description
    }
    DeleteMeetingType.ForMe -> R.string.delete_meeting_for_me_title to when (isRecurring) {
        true -> R.string.delete_meeting_for_me_recurring_description
        false -> R.string.delete_meeting_for_me_description
    }
}

enum class DeleteMeetingType {
    ForMe,
    ForEveryone,
}

data class DeleteMeetingDialogState(
    val deleteType: DeleteMeetingType,
    val isRecurring: Boolean,
    val meetingId: MeetingId,
    val meetingTitle: String,
    val loading: Boolean = false,
    val error: DeleteMeetingError? = null,
)

enum class DeleteMeetingError(val titleRes: Int, val descriptionRes: Int, val retryable: Boolean) {
    DeleteFailed(R.string.delete_meeting_failure_title, R.string.delete_meeting_failure_description, true),
    LeaveFailed(R.string.leave_meeting_failure_title, R.string.leave_meeting_failure_description, true),
    CleanupFailed(R.string.delete_meeting_cleanup_failure_title, R.string.delete_meeting_cleanup_failure_description, false),
    NotAllowed(R.string.delete_meeting_not_allowed_title, R.string.delete_meeting_not_allowed_description, false),
}
