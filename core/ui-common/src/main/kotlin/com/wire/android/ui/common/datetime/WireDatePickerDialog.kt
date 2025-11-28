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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.wire.android.ui.common.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.PreviewMultipleThemes
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireDatePickerDialog(
    title: String,
    selectedDateMillis: Long?,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
    onDateSelected: (Long?) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    WireDialog(
        title = null,
        text = null,
        onDismiss = onDismiss,
        buttonsHorizontalAlignment = true,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true),
        contentPadding = PaddingValues(dimensions().spacing0x),
    ) {
        DatePickerDialogContent(
            title = title,
            selectedDateMillis = selectedDateMillis,
            selectableDates = selectableDates,
            onDateSelected = onDateSelected,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogContent(
    title: String,
    selectedDateMillis: Long?,
    selectableDates: SelectableDates,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {

    val datePickerState = rememberDatePickerState(
        selectableDates = selectableDates
    )
    val dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() }
    val formattedDate = dateFormatter.formatDate(datePickerState.selectedDateMillis, Locale.getDefault())

    LaunchedEffect(Unit) {
        datePickerState.selectedDateMillis = selectedDateMillis
    }

    Column {
        DatePicker(
            title = null,
            headline = {
                Column(
                    modifier = Modifier.padding(horizontal = dimensions().spacing24x),
                ) {
                    Text(
                        text = title,
                        style = typography().title02
                    )
                    VerticalSpace.x32()
                    Text(
                        text = formattedDate ?: stringResource(R.string.label_no_selected_date),
                        style = typography().title01
                    )
                    VerticalSpace.x12()
                }
            },
            state = datePickerState,
            colors = DatePickerDefaults.colors().copy(
                containerColor = colorsScheme().surface
            )
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
                state = if (datePickerState.selectedDateMillis != null) WireButtonState.Default else WireButtonState.Disabled,
                onClick = { onDateSelected(datePickerState.selectedDateMillis) }
            )
        }
    }
}

@Suppress("MagicNumber")
@OptIn(ExperimentalMaterial3Api::class)
class FutureSelectableDates : SelectableDates {

    private val now = LocalDate.now()
    private val dayStart = now.atTime(0, 0, 0, 0).toEpochSecond(ZoneOffset.UTC) * 1000

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis >= dayStart
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year >= now.year
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewMultipleThemes
@Composable
private fun PreviewDatePicker() {
    WireTheme {
        DatePickerDialogContent(
            title = "Expiration date",
            selectedDateMillis = null,
            selectableDates = DatePickerDefaults.AllDates,
            onDateSelected = {},
            onDismiss = {},
        )
    }
}
