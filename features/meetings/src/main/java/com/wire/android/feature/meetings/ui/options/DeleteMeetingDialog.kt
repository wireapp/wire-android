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
        val (titleResId, textResId) = when (state.deleteType) {
            DeleteMeetingType.ForEveryone -> Pair(
                R.string.delete_meeting_for_everyone_title,
                R.string.delete_meeting_for_everyone_description,
            )

            DeleteMeetingType.ForMe -> Pair(
                R.string.delete_meeting_for_me_title,
                R.string.delete_meeting_for_me_description,
            )
        }
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
                    onDelete(state)
                },
                text = stringResource(id = commonR.string.label_ok),
                type = WireDialogButtonType.Primary,
                state = if (state.loading) WireButtonState.Disabled else WireButtonState.Error,
                loading = state.loading
            )
        )
    }
}

enum class DeleteMeetingType {
    ForMe,
    ForEveryone,
}

data class DeleteMeetingDialogState(
    val deleteType: DeleteMeetingType,
    val meetingId: MeetingId,
    val meetingTitle: String,
    val loading: Boolean = false,
)
