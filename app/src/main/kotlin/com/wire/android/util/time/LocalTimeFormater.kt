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
package com.wire.android.util.time

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Stable
import java.util.Date
import java.util.TimeZone

@Stable
fun convertTimestampToDateTime(
    timestampSeconds: Long,
    context: Context
): Pair<String, String> {

    val dateFormat = DateFormat.getDateFormat(context)
    val timeFormat = DateFormat.getTimeFormat(context)

    val timezone = TimeZone.getDefault()
    dateFormat.timeZone = timezone
    timeFormat.timeZone = timezone

    val dateTime = Date(timestampSeconds * 1000)
    val date = dateFormat.format(dateTime)
    val time = timeFormat.format(dateTime)

    return Pair(date, time)
}
