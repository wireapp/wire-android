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

package com.wire.android.ui.common

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.wire.android.model.Clickable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.format.TextStyle
import java.util.Locale

@SuppressLint("ComposeComposableModifier")
@Composable
fun Modifier.selectableBackground(
    isSelected: Boolean,
    onClickDescription: String = stringResource(id = R.string.content_description_select_label),
    onClick: () -> Unit
): Modifier {
    val onItemClick = Clickable(
        enabled = !isSelected,
        onClick = onClick,
        onClickDescription = onClickDescription
    )

    return this
        .clickable(onItemClick)
        .semantics {
            if (isSelected) selected = true // So TalkBack ignores selection when it's not selected
        }
}

fun monthYearHeader(month: Int, year: Int): String {
    val currentYear = Instant.fromEpochMilliseconds(System.currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).year
    val monthYearInstant = LocalDateTime(year = year, monthNumber = month, 1, 0, 0, 0)

    val monthName = monthYearInstant.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    return if (year == currentYear) {
        // If it's the current year, display only the month name
        monthName
    } else {
        // If it's not the current year, display both the month name and the year
        "$monthName $year"
    }
}
