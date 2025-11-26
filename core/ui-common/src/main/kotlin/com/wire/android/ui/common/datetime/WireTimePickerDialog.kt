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
package com.wire.android.ui.common.datetime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.ui.common.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireTimePickerDialog(
    title: String,
    selectedTime: TimePickerResult?,
    onTimeSelected: (TimePickerResult) -> Unit,
    onDismiss: () -> Unit
) {
    WireDialog(
        title = null,
        text = null,
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = true,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true),
        contentPadding = PaddingValues(dimensions().spacing0x),
    ) {
        TimePickerDialogContent(
            title = title,
            selectedTime = selectedTime,
            onTimeSelected = onTimeSelected,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogContent(
    title: String,
    selectedTime: TimePickerResult?,
    onTimeSelected: (TimePickerResult) -> Unit,
    onDismiss: () -> Unit,
) {

    val timePickerState = rememberTimePickerState()

    selectedTime?.let {
        LaunchedEffect(Unit) {
            timePickerState.hour = selectedTime.hour
            timePickerState.minute = selectedTime.minute
        }
    }

    Column {
        Text(
            modifier = Modifier.padding(horizontal = dimensions().spacing16x),
            text = title,
            style = typography().title02
        )
        VerticalSpace.x16()
        TimePicker(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            state = timePickerState,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensions().spacing16x),
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing8x)
        ) {
            WireSecondaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.label_cancel),
                onClick = onDismiss,
            )
            WirePrimaryButton(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.label_ok),
                onClick = {
                    onTimeSelected(
                        TimePickerResult(
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                        )
                    )
                }
            )
        }
    }
}

data class TimePickerResult(val hour: Int, val minute: Int)

fun Long.asTimePickerResult(): TimePickerResult {
    val instant = Instant.fromEpochMilliseconds(this)
    val time = instant.toLocalDateTime(TimeZone.currentSystemDefault()).time
    return TimePickerResult(time.hour, time.minute)
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewMultipleThemes
@Composable
private fun PreviewTimePicker() {
    WireTheme {
        TimePickerDialogContent(
            title = "Expiration date",
            selectedTime = null,
            onTimeSelected = {},
            onDismiss = {},
        )
    }
}
