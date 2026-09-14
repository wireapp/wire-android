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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.model.MeetingHeader
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.util.rememberCurrentTimeProvider
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.BigSectionHeader
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.DateAndTimeParsers
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

@Composable
fun MeetingHeader(
    header: MeetingHeader,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        when (header) {
            is MeetingHeader.Ongoing -> BigSectionHeader(
                name = stringResource(R.string.meeting_date_header_ongoing),
                modifier = Modifier.padding(top = dimensions().spacing8x),
            )

            is MeetingHeader.Day -> BigSectionHeader(
                    name = getDateHeaderString(header.time),
                    modifier = Modifier.padding(top = dimensions().spacing24x),
                )

            is MeetingHeader.DayAndHour -> {
                BigSectionHeader(
                    name = getDateHeaderString(header.time),
                    modifier = Modifier.padding(top = dimensions().spacing8x),
                )
                SectionHeader(name = DateAndTimeParsers.meetingTime(header.time))
            }

            is MeetingHeader.Hour -> SectionHeader(name = DateAndTimeParsers.meetingTime(header.time))
        }
    }
}

@Composable
private fun getDateHeaderString(time: Instant): String {
    val currentTime = rememberCurrentTimeProvider()
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

@PreviewMultipleThemes
@Composable
fun PreviewMeetingHeaderOngoing() = WireTheme {
    MeetingHeader(header = MeetingHeader.Ongoing)
}

@PreviewMultipleThemes
@Composable
fun PreviewMeetingHeaderToday() = WireTheme {
    MeetingHeader(header = MeetingHeader.Day(time = Clock.System.now()))
}

@PreviewMultipleThemes
@Composable
fun PreviewMeetingHeaderTomorrow() = WireTheme {
    MeetingHeader(header = MeetingHeader.Day(time = Clock.System.now().plus(1.days)))
}

@PreviewMultipleThemes
@Composable
fun PreviewMeetingHeaderOtherDay() = WireTheme {
    MeetingHeader(header = MeetingHeader.Day(time = Instant.parse("2024-06-01T10:00:00Z")))
}

@PreviewMultipleThemes
@Composable
fun PreviewMeetingHeaderDayAndHour() = WireTheme {
    MeetingHeader(header = MeetingHeader.DayAndHour(time = Instant.parse("2024-06-01T10:00:00Z")))
}

@PreviewMultipleThemes
@Composable
fun PreviewMeetingHeaderHour() = WireTheme {
    MeetingHeader(header = MeetingHeader.Hour(time = Instant.parse("2024-06-01T12:00:00Z")))
}
