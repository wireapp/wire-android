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
package com.wire.android.util

import android.text.format.DateFormat
import androidx.compose.runtime.Stable
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale

//region convenience ext functions
@Stable
fun Date.toMediumOnlyDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.toMediumOnlyDateTime(this, locale, zoneId)

@Stable
fun Instant.deviceDateTimeFormat(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.deviceDateTimeFormat(this, locale, zoneId)

@Stable
fun Instant.formatMediumDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.formatMediumDateTime(this, locale, zoneId)

@Stable
fun Instant.formatFullDateShortTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.formatFullDateShortTime(this, locale, zoneId)

@Stable
fun Instant.formatMonthDayShortTime(
    is24Hour: Boolean,
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.formatMonthDayShortTime(this, is24Hour, locale, zoneId)

@Stable
fun Instant.uiMessageDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.uiMessageDateTime(this, locale, zoneId)

@Stable
fun Instant.fileDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.fileDateTime(this, locale, zoneId)

@Stable
fun Instant.cellFileDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.cellFileDateTime(this, locale, zoneId)

@Stable
fun Instant.cellFileTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.cellTimeFormat(this, locale, zoneId)

@Stable
fun Instant.uiReadReceiptDateTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.uiReadReceiptDateTime(this, locale, zoneId)

@Stable
fun Long.uiLinkExpirationDate(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.linkExpirationDate(this, locale, zoneId)

@Stable
fun Long.uiLinkExpirationTime(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = DateAndTimeParsers.linkExpirationTime(this, locale, zoneId)
//endregion

/**
 * Date and time parsers between different formats and types.
 */
class DateAndTimeParsers private constructor() {

    @Suppress("TooManyFunctions")
    companion object {
        private val longDateShortTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
        private val shortTimeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        private val shortTime24hFormat = DateTimeFormatter.ofPattern("HH:mm")
        private val mediumDateTimeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
        private val fullDateShortTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
        private val fileDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss")
        private val readReceiptDateTimeFormat = DateTimeFormatter.ofPattern("MMM dd yyyy,  hh:mm a")
        private val mediumOnlyDateTimeFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        private val durationMessageTimeFormat = DateTimeFormatter.ofPattern("mm:ss")
        private val dayOfWeekMonthDayDateFormat = DateTimeFormatter.ofPattern("EEEE, MMMM d")

        fun deviceDateTimeFormat(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = longDateShortTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun formatMediumDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = mediumDateTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun formatFullDateShortTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = fullDateShortTimeFormatter.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun formatMonthDayShortTime(
            instant: Instant,
            is24Hour: Boolean,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String {
            val date = instant.toJavaInstant()
            val datePattern = DateFormat.getBestDateTimePattern(locale, "MMMMd")
            val timePattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm")
            val formattedDate = DateTimeFormatter.ofPattern(datePattern).withLocale(locale).withZone(zoneId).format(date)
            val formattedTime = DateTimeFormatter.ofPattern(timePattern).withLocale(locale).withZone(zoneId).format(date)
            return "$formattedDate, $formattedTime"
        }

        fun cellTimeFormat(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = shortTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun cellDateFormat(
            instant: Instant,
            showYear: Boolean = false,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String {
            val pattern = if (showYear) "MMM dd, yyyy" else "MMM dd"
            val formatter = DateTimeFormatter.ofPattern(pattern).withLocale(locale).withZone(zoneId)
            return formatter.format(instant.toJavaInstant())
        }

        fun cellFileDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String {

            val dateString = cellDateFormat(instant = instant, showYear = false, locale = locale, zoneId = zoneId)
            val timeString = cellTimeFormat(instant = instant, locale = locale, zoneId = zoneId)

            return "$dateString, $timeString"
        }

        fun fileDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = fileDateTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun uiReadReceiptDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = readReceiptDateTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun toMediumOnlyDateTime(
            date: Date,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = mediumOnlyDateTimeFormat.withLocale(locale).withZone(zoneId).format(date.toInstant())

        fun uiMessageDateTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = shortTimeFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun audioMessageTime(
            timeMs: Long,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = durationMessageTimeFormat.withLocale(locale).withZone(zoneId).format(java.time.Instant.ofEpochMilli(timeMs))

        fun videoMessageTime(
            timeMs: Long,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = durationMessageTimeFormat.withLocale(locale).withZone(zoneId).format(java.time.Instant.ofEpochMilli(timeMs))

        fun meetingDate(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = dayOfWeekMonthDayDateFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun meetingTime(
            instant: Instant,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = shortTime24hFormat.withLocale(locale).withZone(zoneId).format(instant.toJavaInstant())

        fun linkExpirationDate(
            timeMs: Long,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = dayOfWeekMonthDayDateFormat.withLocale(locale).withZone(zoneId).format(java.time.Instant.ofEpochMilli(timeMs))

        fun linkExpirationTime(
            timeMs: Long,
            locale: Locale = Locale.getDefault(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): String = shortTimeFormat.withLocale(locale).withZone(zoneId).format(java.time.Instant.ofEpochMilli(timeMs))
    }
}
