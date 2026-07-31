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
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.common.R as commonR

@Composable
fun NewMeetingErrorDialog(
    type: NewMeetingType,
    onDismiss: () -> Unit,
) {
    // TODO: specific errors to be handled later, for now we just show single generic one
    val (titleResId, descriptionResId) = when (type) {
        NewMeetingType.MeetNow -> R.string.new_meeting_now_failure_title to R.string.new_meeting_now_failure_description
        NewMeetingType.Schedule -> R.string.new_meeting_schedule_failure_title to R.string.new_meeting_schedule_failure_description
    }
    WireDialog(
        title = stringResource(titleResId),
        text = stringResource(descriptionResId),
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = false,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(commonR.string.label_ok),
            type = WireDialogButtonType.Primary,
        ),
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewCreateGroupErrorDialogLackingConnection() = WireTheme {
    NewMeetingErrorDialog(type = NewMeetingType.MeetNow, onDismiss = {})
}
