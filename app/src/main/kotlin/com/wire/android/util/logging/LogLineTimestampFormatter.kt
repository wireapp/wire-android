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
package com.wire.android.util.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class LogLineTimestampFormatter(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val dateFormat = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US)
    private val date = Date(0L)
    private var cachedEpochSecond = Long.MIN_VALUE
    private var cachedPrefix = ""

    fun now(): String = format(currentTimeMillis())

    fun format(timestampMillis: Long): String {
        val epochSecond = timestampMillis / MILLIS_IN_SECOND
        if (epochSecond != cachedEpochSecond) {
            date.time = epochSecond * MILLIS_IN_SECOND
            cachedPrefix = dateFormat.format(date)
            cachedEpochSecond = epochSecond
        }

        val millis = (timestampMillis - epochSecond * MILLIS_IN_SECOND).toInt()
        return buildString(capacity = cachedPrefix.length + MILLIS_SUFFIX_LENGTH) {
            append(cachedPrefix)
            append('.')
            appendPaddedMillis(millis)
        }
    }

    private fun StringBuilder.appendPaddedMillis(millis: Int) {
        if (millis < HUNDRED_MILLIS) append('0')
        if (millis < TEN_MILLIS) append('0')
        append(millis)
    }

    private companion object {
        const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss"
        const val MILLIS_IN_SECOND = 1000L
        const val MILLIS_SUFFIX_LENGTH = 4
        const val HUNDRED_MILLIS = 100
        const val TEN_MILLIS = 10
    }
}
