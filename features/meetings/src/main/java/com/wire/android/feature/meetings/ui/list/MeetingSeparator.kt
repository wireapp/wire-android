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
package com.wire.android.feature.meetings.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.model.MeetingSeparator
import com.wire.android.feature.meetings.ui.util.CurrentTimeScope
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.DateAndTimeParsers
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun CurrentTimeScope.MeetingSeparator(
    header: MeetingSeparator,
    modifier: Modifier = Modifier,
    onShowAll: () -> Unit,
) {
    Column(modifier = modifier) {
        when (header) {
            is MeetingSeparator.Ongoing -> BigSectionHeader(
                    name = stringResource(R.string.meeting_date_header_ongoing),
                    modifier = Modifier.padding(top = dimensions().spacing8x),
                )

            is MeetingSeparator.DayAndHour -> {
                BigSectionHeader(
                    name = getDateHeaderString(header.time),
                    modifier = Modifier.padding(top = dimensions().spacing8x),
                )
                SectionHeader(name = DateAndTimeParsers.meetingTime(header.time))
            }

            is MeetingSeparator.Hour -> SectionHeader(name = DateAndTimeParsers.meetingTime(header.time))

            is MeetingSeparator.ShowAll -> WireSecondaryButton(
                    text = stringResource(R.string.meeting_button_show_all),
                    onClick = onShowAll,
                    fillMaxWidth = true,
                    minSize = dimensions().buttonSmallMinSize,
                    minClickableSize = dimensions().buttonMinClickableSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions().spacing16x),
                )
        }
    }
}

@Composable
fun SectionHeader(
    name: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
) {
    Text(
        text = name.uppercase(),
        modifier = modifier.padding(padding),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.wireTypography.title03,
        color = MaterialTheme.wireColorScheme.secondaryText,
    )
}

@Composable
fun BigSectionHeader(
    name: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = dimensions().spacing16x, vertical = dimensions().spacing8x),
) {
    Text(
        text = name,
        modifier = modifier.padding(padding),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.wireTypography.body02,
        color = MaterialTheme.wireColorScheme.onSurface,
    )
}

@Composable
private fun CurrentTimeScope.getDateHeaderString(time: Instant): String {
    val dateString = DateAndTimeParsers.meetingDate(time)
    val currentLocalDate = currentTime().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val localDate = time.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when (localDate) {
        currentLocalDate -> stringResource(R.string.meeting_date_header_today, dateString)
        currentLocalDate.plus(1, DateTimeUnit.DAY) -> stringResource(R.string.meeting_date_header_tomorrow, dateString)
        currentLocalDate.minus(1, DateTimeUnit.DAY) -> stringResource(R.string.meeting_date_header_yesterday, dateString)
        else -> dateString
    }
}
